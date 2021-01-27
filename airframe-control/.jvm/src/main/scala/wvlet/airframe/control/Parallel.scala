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
import java.util.concurrent._
import java.util.concurrent.atomic.{AtomicBoolean, AtomicLong, AtomicReference}

import wvlet.log.LogSupport

import scala.reflect.ClassTag
import scala.jdk.CollectionConverters._
import scala.util.control.{ControlThrowable, NonFatal}

/**
  * Utilities for parallel execution.
  */
object Parallel extends LogSupport {

  private class BreakException extends ControlThrowable

  // Parallel execution can be stopped by calling this method.
  def break: Unit = throw new BreakException()

  val stats = new ParallelExecutionStats()

  class ParallelExecutionStats(
      val totalThreads: AtomicLong = new AtomicLong(0),
      val runningWorkers: AtomicLong = new AtomicLong(0),
      val startedTasks: AtomicLong = new AtomicLong(0),
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

    val requestQueue = new LinkedBlockingQueue[Worker[T, R]](parallelism)
    val resultQueue  = new LinkedBlockingQueue[Option[R]]()
    val interrupted  = new AtomicBoolean(false)

    Range(0, parallelism).foreach { i =>
      val worker = new Worker[T, R](executionId, i.toString, requestQueue, resultQueue, interrupted, f)
      requestQueue.put(worker)
    }

    val executor = Executors.newFixedThreadPool(parallelism)
    stats.totalThreads.addAndGet(parallelism)

    try {
      // Process all elements of source
      val it = source.iterator
      while (it.hasNext && !interrupted.get()) {
        val worker = requestQueue.take()
        if (!interrupted.get()) {
          worker.message.set(it.next())
          executor.execute(worker)
        } else {
          requestQueue.put(worker)
        }
      }

      // Wait for completion
      while (requestQueue.size() != parallelism) {
        try {
          Thread.sleep(10)
        } catch {
          case _: InterruptedException => ()
        }
      }

      resultQueue.asScala.flatten.toSeq
    } catch {
      case _: InterruptedException => throw new TimeoutException()
    } finally {
      // Cleanup
      executor.shutdown()
      requestQueue.clear()
      stats.totalThreads.addAndGet(parallelism * -1)
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
      parallelism: Int = Runtime.getRuntime.availableProcessors()
  )(f: T => R): Iterator[R] = {
    val executionId = UUID.randomUUID.toString
    trace(s"$executionId - Begin Parallel.iterate (parallelism = ${parallelism})")

    val requestQueue = new LinkedBlockingQueue[Worker[T, R]](parallelism)
    val resultQueue  = new LinkedBlockingQueue[Option[R]]()
    val interrupted  = new AtomicBoolean(false)

    Range(0, parallelism).foreach { i =>
      val worker = new Worker[T, R](executionId, i.toString, requestQueue, resultQueue, interrupted, f)
      requestQueue.put(worker)
    }

    new Thread {
      override def run(): Unit = {
        val executor = Executors.newFixedThreadPool(parallelism)
        stats.totalThreads.addAndGet(parallelism)

        try {
          // Process all elements of source
          while (source.hasNext && !interrupted.get()) {
            val worker = requestQueue.take()
            if (!interrupted.get()) {
              worker.message.set(source.next())
              executor.execute(worker)
            } else {
              requestQueue.put(worker)
            }
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
          stats.totalThreads.addAndGet(parallelism * -1)
        }
      }
    }.start()

    new ResultIterator[R](resultQueue)
  }

  private[control] class Worker[T, R](
      executionId: String,
      workerId: String,
      requestQueue: BlockingQueue[Worker[T, R]],
      resultQueue: BlockingQueue[Option[R]],
      interrupted: AtomicBoolean,
      f: T => R
  ) extends Runnable
      with LogSupport {
    val message: AtomicReference[T] = new AtomicReference[T]()

    override def run: Unit = {
      trace(s"$executionId - Begin worker-$workerId")
      Parallel.stats.runningWorkers.incrementAndGet()
      stats.startedTasks.incrementAndGet()
      try {
        resultQueue.put(Some(f(message.get())))
      } catch {
        case _: BreakException =>
          interrupted.set(true)
        case NonFatal(e) =>
          warn(s"$executionId - Error worker-$workerId", e)
          throw e
      } finally {
        trace(s"$executionId - End worker-$workerId")
        stats.finishedTasks.incrementAndGet()
        Parallel.stats.runningWorkers.decrementAndGet()
        requestQueue.put(this)
      }
    }
  }
}
