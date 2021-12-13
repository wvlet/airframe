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
import scala.scalajs.js.timers.SetIntervalHandle
import scala.util.Try

/**
  */
object compat {
  def newTimer: Timer = {
    new Timer {
      private var intervalHandle: Option[SetIntervalHandle] = None
      private var lastTimeMillis                            = System.currentTimeMillis()
      override def schedule[U](millis: Long)(body: Long => U): Unit = {
        val handle = scala.scalajs.js.timers.setInterval(millis.toDouble) {
          val currentTimeMillis = System.currentTimeMillis()
          try {
            body(currentTimeMillis - lastTimeMillis)
          } finally {
            lastTimeMillis = currentTimeMillis
          }
        }
        intervalHandle = Some(handle)
      }
      override def cancel: Unit = {
        intervalHandle.foreach { handle =>
          Try(scala.scalajs.js.timers.clearInterval(handle))
        }
      }
    }
  }

  def scheduleOnce[U](delayMills: Long)(body: => U): Cancelable = {
    val timeoutHandle = scala.scalajs.js.timers.setTimeout(delayMills.toDouble)(body)
    Cancelable { () =>
      Option(timeoutHandle).foreach { handle =>
        Try(scala.scalajs.js.timers.clearTimeout(handle))
      }
    }
  }

  def toSeq[A](rx: Rx[A]): Seq[A] = {
    throw new UnsupportedOperationException("Rx.toSeq is unsupported in Scala.js")
  }
}
