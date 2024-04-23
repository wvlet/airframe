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
package wvlet.airframe.rx

import scala.util.Try
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Promise}
import java.util.concurrent.{Executors, ThreadFactory, TimeUnit}
import java.util.concurrent.atomic.{AtomicInteger, AtomicBoolean}

/**
  */
object compat:

  private def newDaemonThreadFactory(name: String): ThreadFactory = new ThreadFactory:
    private val group: ThreadGroup = new ThreadGroup(Thread.currentThread().getThreadGroup(), name)
    private val threadNumber       = new AtomicInteger(1)

    override def newThread(r: Runnable): Thread =
      val threadName = s"${name}-${threadNumber.getAndIncrement()}"
      val thread     = new Thread(group, r, threadName)
      thread.setName(threadName)
      thread.setDaemon(true)
      thread

  private lazy val defaultExecutor =
    ExecutionContext.fromExecutorService(Executors.newCachedThreadPool(newDaemonThreadFactory("airframe-rx")))

  def defaultExecutionContext: scala.concurrent.ExecutionContext = defaultExecutor

  def newTimer: Timer = ???

  def scheduleOnce[U](delayMills: Long)(body: => U): Cancelable =
    val thread = Executors.newScheduledThreadPool(1)
    val schedule = thread.schedule(
      new Runnable:
        override def run(): Unit =
          body
      ,
      delayMills,
      TimeUnit.MILLISECONDS
    )
    // Immediately start the thread pool shutdown to avoid thread leak
    thread.shutdown()
    Cancelable { () =>
      try
        schedule.cancel(false)
      finally
        thread.shutdown()
    }

  def toSeq[A](rx: Rx[A]): Seq[A] =
    val ready = new AtomicBoolean(true)
    val s     = Seq.newBuilder[A]
    var c     = Cancelable.empty
    c = RxRunner.run(rx) {
      case OnNext(v) => s += v.asInstanceOf[A]
      case OnError(e) =>
        c.cancel
        throw e
      case OnCompletion =>
        c.cancel
        ready.set(true)
    }
    while !ready.get() do {}
    s.result()

  private[rx] def await[A](rx: RxOps[A]): A =
    val p = Promise[A]()
    val c = RxRunner.runOnce(rx) {
      case OnNext(v)    => p.success(v.asInstanceOf[A])
      case OnError(e)   => p.failure(e)
      case OnCompletion => p.failure(new IllegalStateException(s"OnCompletion should not be issued in: ${rx}"))
    }
    try
      Await.result(p.future, Duration.Inf)
    finally
      c.cancel
