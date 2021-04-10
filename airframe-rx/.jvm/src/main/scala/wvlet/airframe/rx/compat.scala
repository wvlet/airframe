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
import java.util.TimerTask
import java.util.concurrent.{ExecutorService, Executors, TimeUnit}
import java.util.concurrent.atomic.AtomicBoolean

/**
  */
object compat {
  def newTimer: Timer =
    new Timer {
      private val t = new java.util.Timer(true)
      override def schedule[U](millis: Long)(body: Long => U): Unit = {
        t.schedule(
          new TimerTask {
            private var lastTime = System.currentTimeMillis()
            override def run(): Unit = {
              val currentTime = System.currentTimeMillis()
              try {
                body(currentTime - lastTime)
              } finally {
                lastTime = currentTime
              }
            }
          },
          0,
          millis
        )
      }
      override def cancel: Unit = {
        t.cancel()
      }
    }

  def scheduleOnce[U](delayMills: Long)(body: => U): Cancelable = {
    val thread = Executors.newScheduledThreadPool(1)
    val schedule = thread.schedule(
      new Runnable {
        override def run(): Unit = {
          body()
        }
      },
      delayMills,
      TimeUnit.MILLISECONDS
    )
    // Immediately start the thread pool shutdown to avoid thread leak
    thread.shutdown()
    Cancelable { () =>
      try {
        schedule.cancel(false)
      } finally {
        thread.shutdown()
      }
    }
  }

  def toSeq[A](rx: Rx[A]): Seq[A] = {
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
    while (!ready.get()) {}
    s.result()
  }
}
