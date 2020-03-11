/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package wvlet.airframe.control

import java.util.UUID
import java.util.concurrent.atomic.{AtomicLong, AtomicReference}
import java.util.concurrent._

import wvlet.airframe.jmx.{JMX, JMXAgent}
import wvlet.log.LogSupport

import scala.reflect.ClassTag

/**
  * Utilities for parallel execution.
  */
object Parallel extends LogSupport {
  val jmxStats = new ParallelExecutionStats()

  class ParallelExecutionStats(
      @JMX(description = "Total number of worker threads")
      val totalThreads: AtomicLong = new AtomicLong(0),
      @JMX(description = "Total number of running workers")
      val runningWorkers: AtomicLong = new AtomicLong(0),
      @JMX(description = "Accumulated total number of started tasks")
      val startedTasks: AtomicLong = new AtomicLong(0),
      @JMX(description = "Accumulated total number of finished tasks")
      val finishedTasks: AtomicLong = new AtomicLong(0)
  )

  private class ResultIterator[R](queue: LinkedBlockingQueue[Option[R]]) extends Iterator[R] {
    private var nextMessage: Option[R] = None

    override def hasNext: Boolean = {
      nextMessage = queue.take()
      nextMessage.isDefined
    }

    override def next(): R = {
      nextMessage.get
    }
  }

  /**
    * Process all elements of the source by the given function then wait for the completion.
    *
    * @param source      Source collection
    * @param parallelism Parallelism, the default value is a number of available processors
    * @param f           Function which processes each element of the source collection
    * @return Collection of the results
    */
  def run[T, R: ClassTag](source: Seq[T], parallelism: Int = Runtime.getRuntime.availableProcessors())(
      f: T => R
  ): Seq[R] = {
    val executionId = UUID.randomUUID.toString
    trace(s"$executionId - Begin Parallel.run (parallelism = ${parallelism})")

    val requestQueue = new LinkedBlockingQueue[IndexedWorker[T, R]](parallelism)
    val resultArray  = new Array[R](source.length)

    Range(0, parallelism).foreach { i =>
      val worker = new IndexedWorker[T, R](executionId, i.toString, requestQueue, resultArray, f)
      requestQueue.put(worker)
    }

    val executor = Executors.newFixedThreadPool(parallelism)
    jmxStats.totalThreads.addAndGet(parallelism)

    try {
      // Process all elements of source
      val it = source.zipWithIndex.iterator
      while (it.hasNext) {
        val worker = requestQueue.take()
        worker.message.set(it.next())
        executor.execute(worker)
      }

      // Wait for completion
      while (requestQueue.size() != parallelism) {
        try {
          Thread.sleep(10)
        } catch {
          case _: InterruptedException => ()
        }
      }

      resultArray.toSeq
    } catch {
      case _: InterruptedException => throw new TimeoutException()
    } finally {
      // Cleanup
      executor.shutdown()
      requestQueue.clear()
      jmxStats.totalThreads.addAndGet(parallelism * -1)
    }
  }

  /**
    * Process all elements of the source by the given function then don't wait completion.
    * The result is an iterator which is likely a stream which elements are pushed continuously.
    *
    * @param source      the source iterator
    * @param parallelism Parallelism, the default value is a number of available processors
    * @param f           Function which processes each element of the source collection
    * @return Iterator of the results
    */
  def iterate[T, R](
      source: Iterator[T],
      parallelism: Int = Runtime.getRuntime.availableProcessors(),
      jmxAgent: Option[JMXAgent] = None
  )(f: T => R): Iterator[R] = {
    val executionId = UUID.randomUUID.toString
    trace(s"$executionId - Begin Parallel.iterate (parallelism = ${parallelism})")

    val requestQueue = new LinkedBlockingQueue[Worker[T, R]](parallelism)
    val resultQueue  = new LinkedBlockingQueue[Option[R]]()

    Range(0, parallelism).foreach { i =>
      val worker = new Worker[T, R](executionId, i.toString, requestQueue, resultQueue, f)
      requestQueue.put(worker)
    }

    new Thread {
      override def run(): Unit = {
        val executor = Executors.newFixedThreadPool(parallelism)
        jmxStats.totalThreads.addAndGet(parallelism)

        try {
          // Process all elements of source
          while (source.hasNext) {
            val worker = requestQueue.take()
            worker.message.set(source.next())
            executor.execute(worker)
          }

          // Wait for completion
          while (requestQueue.size() != parallelism) {
            try {
              Thread.sleep(10)
            } catch {
              case _: InterruptedException => ()
            }
          }
        } catch {
          case _: InterruptedException => throw new TimeoutException()
        } finally {
          // Terminate
          resultQueue.put(None)

          // Cleanup
          executor.shutdown()
          requestQueue.clear()
          jmxStats.totalThreads.addAndGet(parallelism * -1)
        }
      }
    }.start()

    new ResultIterator[R](resultQueue)
  }

  //  /**
  //    * Run the given function with each element of the source periodically and repeatedly.
  //    * Execution can be stopped by the returned Stoppable object.
  //    *
  //    * @param source Source collection
  //    * @param interval Interval of execution of an element
  //    * @param f Function which process each element of the source collection
  //    * @return Object to stop execution
  //    */
  //  def repeat[T](source: Seq[T], interval: Duration, ticker: Ticker = Ticker.systemTicker)(f: T => Unit): Stoppable = {
  //    val requestQueue = new LinkedBlockingQueue[IndexedWorker[T, Unit]](source.size)
  //    val resultArray  = new Array[Unit](source.size)
  //    val executor     = Executors.newFixedThreadPool(source.size)
  //    val cancelable   = new Stoppable(executor)
  //
  //    Range(0, source.size).foreach { _ =>
  //      val repeatedFunction = (arg: T) => {
  //        while (!cancelable.isStopped) {
  //          // Use nanotime to make it independent from the system clock time
  //          val startNano = ticker.read
  //          f(arg)
  //          val durationNanos = ticker.read - startNano
  //          val wait          = math.max(0, interval.toMillis - TimeUnit.NANOSECONDS.toMillis(durationNanos))
  //          try {
  //            Thread.sleep(wait)
  //          } catch {
  //            case _: InterruptedException => ()
  //          }
  //        }
  //      }
  //
  //      val worker = new IndexedWorker[T, Unit](requestQueue, resultArray, repeatedFunction)
  //      requestQueue.put(worker)
  //    }
  //
  //    source.zipWithIndex.foreach {
  //      case (e, i) =>
  //        val worker = requestQueue.take()
  //        worker.message.set(e, i)
  //        executor.execute(worker)
  //    }
  //
  //    cancelable
  //  }
  //
  //  class Stoppable(executor: ExecutorService) {
  //    private val cancelled  = new AtomicBoolean(false)
  //    def isStopped: Boolean = cancelled.get()
  //
  //    def stop: Unit = {
  //      executor.shutdownNow()
  //      cancelled.set(true)
  //    }
  //  }

  private[control] class Worker[T, R](
      executionId: String,
      workerId: String,
      requestQueue: BlockingQueue[Worker[T, R]],
      resultQueue: BlockingQueue[Option[R]],
      f: T => R
  ) extends Runnable
      with LogSupport {
    val message: AtomicReference[T] = new AtomicReference[T]()

    override def run: Unit = {
      trace(s"$executionId - Begin worker-$workerId")
      Parallel.jmxStats.runningWorkers.incrementAndGet()
      jmxStats.startedTasks.incrementAndGet()
      try {
        resultQueue.put(Some(f(message.get())))
      } catch {
        case e: Exception =>
          warn(s"$executionId - Error worker-$workerId", e)
          throw e
      } finally {
        requestQueue.put(this)
        trace(s"$executionId - End worker-$workerId")
        jmxStats.finishedTasks.incrementAndGet()
        Parallel.jmxStats.runningWorkers.decrementAndGet()
      }
    }
  }

  private[control] class IndexedWorker[T, R](
      executionId: String,
      workerId: String,
      requestQueue: BlockingQueue[IndexedWorker[T, R]],
      resultArray: Array[R],
      f: T => R
  ) extends Runnable
      with LogSupport {
    val message: AtomicReference[(T, Int)] = new AtomicReference[(T, Int)]()

    override def run: Unit = {
      trace(s"$executionId - Begin worker-$workerId")
      Parallel.jmxStats.runningWorkers.incrementAndGet()
      jmxStats.startedTasks.incrementAndGet()
      try {
        val (m, i) = message.get()
        resultArray(i) = f(m)
      } catch {
        case e: Exception =>
          warn(s"$executionId - Error worker-$workerId", e)
          throw e
      } finally {
        requestQueue.put(this)
        trace(s"$executionId - End worker-$workerId")
        jmxStats.finishedTasks.incrementAndGet()
        Parallel.jmxStats.runningWorkers.decrementAndGet()
      }
    }
  }
}
