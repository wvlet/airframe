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

import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.{ScheduledThreadPoolExecutor, ThreadFactory, TimeUnit}
import scala.concurrent.{ExecutionContext, Future, Promise}

/**
  */
object Compat {
  def sleep(millis: Long): Unit = {
    Thread.sleep(millis)
  }

  private val threadFactoryId = new AtomicInteger()

  /**
    * A thread factory for creating a daemon thread so as not to block JVM shutdown
    */
  private class DefaultThreadFactory extends ThreadFactory {
    private val factoryId = threadFactoryId.getAndIncrement()
    private val threadId  = new AtomicInteger()
    override def newThread(r: Runnable): Thread = {
      val threadName = s"airframe-control-${factoryId}:${threadId.getAndIncrement()}"
      val thread     = new Thread(null, r, threadName)
      thread.setName(threadName)
      thread.setDaemon(true)
      thread
    }
  }
  private lazy val scheduledExecutor = new ScheduledThreadPoolExecutor(2, new DefaultThreadFactory)

  def scheduleAsync[A](waitMillis: Long)(body: => Future[A])(implicit ec: ExecutionContext): Future[A] = {
    val promise = Promise[A]()
    try {
      scheduledExecutor.schedule(
        new Runnable {
          override def run(): Unit = {
            body.onComplete { ret =>
              promise.complete(ret)
            }
          }
        },
        waitMillis,
        TimeUnit.MILLISECONDS
      )
    } catch {
      case e: Throwable => promise.failure(e)
    }
    promise.future
  }
}
