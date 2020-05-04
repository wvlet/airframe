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

/**
  * Ticker is for measuring the elapsed time.
  */
trait Ticker {
  // Return the number of nanoseconds elapsed
  def read: Long
}

/**
  * A Ticker implementation thta can be incremanted for test
  */
class ManualTicker(private var counter: Long = 0) extends Ticker {
  def tick(n: Long): Unit = counter += n
  def read: Long          = counter
}

object Ticker {
  // A ticker that reads the current time using System.nanoTime()
  def systemTicker: Ticker =
    new Ticker {
      override def read: Long = System.nanoTime()
    }
  def manualTicker: ManualTicker = new ManualTicker
}
