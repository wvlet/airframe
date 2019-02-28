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

import java.util.concurrent.atomic.{AtomicBoolean, AtomicReference}
import java.util.concurrent._

import scala.concurrent.duration.Duration
import scala.reflect.ClassTag

/**
  * Tiny utilities for parallel execution.
  */
object Parallel {

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
    * Process all elements of the source by the given function then wait for completion.
    *
    * @param source Source collection
    * @param parallelism Parallelism, the default value is a number of available processors
    * @param f Function which process each element of the source collection
    * @return Collection of results
    */
  def run[T, R: ClassTag](source: Seq[T],
                          parallelism: Int = Runtime.getRuntime.availableProcessors(),
                          timeout: Duration = Duration.Inf)(f: T => R): Seq[R] = {
    val requestQueue = new LinkedBlockingQueue[WithIndexWorker[T, R]](parallelism)
    val resultArray  = new Array[R](source.length)

    Range(0, parallelism).foreach { _ =>
      val worker = new WithIndexWorker[T, R](requestQueue, resultArray, f)
      requestQueue.put(worker)
    }

    val executor = Executors.newFixedThreadPool(parallelism)

    try {
      // Process all elements of source
      val it = source.zipWithIndex.toIterator
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
    }
  }

  /**
    * Process all elements of the source by the given function then don't wait completion.
    * The result is an iterator which is likely a stream which elements are pushed continuously.
    *
    * @param source the source iterator
    * @param parallelism Parallelism, the default value is a number of available processors
    * @param f Function which process each element of the source collection
    * @return Iterator of results
    */
  def iterate[T, R](source: Iterator[T],
                    parallelism: Int = Runtime.getRuntime.availableProcessors(),
                    timeout: Duration = Duration.Inf)(f: T => R): Iterator[R] = {
    val requestQueue = new LinkedBlockingQueue[Worker[T, R]](parallelism)
    val resultQueue  = new LinkedBlockingQueue[Option[R]]()

    Range(0, parallelism).foreach { _ =>
      val worker = new Worker[T, R](requestQueue, resultQueue, f)
      requestQueue.put(worker)
    }

    new Thread {
      override def run(): Unit = {
        val executor = Executors.newFixedThreadPool(parallelism)

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
        }
      }
    }.start()

    new ResultIterator[R](resultQueue)
  }

  /**
    * Run the given function with each element of the source periodically and repeatedly.
    * Execution can be stopped by the returned Stoppable object.
    *
    * @param source Source collection
    * @param interval Interval of execution of an element
    * @param f Function which process each element of the source collection
    * @return Object to stop execution
    */
  def repeat[T](source: Seq[T], interval: Duration)(f: T => Unit): Stoppable = {
    val requestQueue = new LinkedBlockingQueue[WithIndexWorker[T, Unit]](source.size)
    val resultArray  = new Array[Unit](source.size)
    val executor     = Executors.newFixedThreadPool(source.size)
    val cancelable   = new Stoppable(executor)

    Range(0, source.size).foreach { _ =>
      val repeatedFunction = (arg: T) => {
        while (!cancelable.isStopped) {
          val start = System.currentTimeMillis()
          f(arg)
          val duration = System.currentTimeMillis() - start
          val wait     = math.max(0, interval.toMillis - duration)
          try {
            Thread.sleep(wait)
          } catch {
            case _: InterruptedException => ()
          }
        }
      }

      val worker = new WithIndexWorker[T, Unit](requestQueue, resultArray, repeatedFunction)
      requestQueue.put(worker)
    }

    source.zipWithIndex.foreach {
      case (e, i) =>
        val worker = requestQueue.take()
        worker.message.set(e, i)
        executor.execute(worker)
    }

    cancelable
  }

  class Stoppable(executor: ExecutorService) {
    private val cancelled  = new AtomicBoolean(false)
    def isStopped: Boolean = cancelled.get()
    def stop(): Unit = {
      executor.shutdownNow()
      cancelled.set(true)
    }
  }

  private[control] class Worker[T, R](requestQueue: BlockingQueue[Worker[T, R]],
                                      resultQueue: BlockingQueue[Option[R]],
                                      f: T => R)
      extends Runnable {

    val message: AtomicReference[T] = new AtomicReference[T]()

    override def run: Unit = {
      try {
        resultQueue.put(Some(f(message.get())))
      } finally {
        requestQueue.put(this)
      }
    }
  }

  private[control] class WithIndexWorker[T, R](requestQueue: BlockingQueue[WithIndexWorker[T, R]],
                                               resultArray: Array[R],
                                               f: T => R)
      extends Runnable {

    val message: AtomicReference[(T, Int)] = new AtomicReference[(T, Int)]()

    override def run: Unit = {
      try {
        val (m, i) = message.get()
        resultArray(i) = f(m)
      } finally {
        requestQueue.put(this)
      }
    }
  }

}
