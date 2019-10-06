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
package wvlet.airframe.control.util

/**
  * This code is based on com.twitter.finagle.util.Ema.
  *
  * This class is non-thread safe, so callers of the update method need to manage synchronization.
  */
private[control] class ExponentialMovingAverage(windowSize: Long) {
  private[this] var time = Long.MinValue

  // this is volatile to allow read-only calls to `last`
  // without needing synchronization.
  @volatile private[this] var ema = 0.0

  /**
    * Update the average with observed value `x`, and return the new average.
    *
    * Since `update` requires monotonic timestamps, it is up to the caller to
    * ensure that calls to update do not race.
    */
  def update(timeStamp: Long, x: Double): Double = {
    if (time == Long.MinValue) {
      time = timeStamp
      ema = x
      x
    } else {
      val td = timeStamp - time
      assert(td >= 0, "Nonmonotonic timestamp")
      time = timeStamp
      val w      = if (windowSize == 0.0) 0.0 else math.exp(-td.toDouble / windowSize)
      val newEma = x * (1 - w) + ema * w
      ema = newEma
      newEma
    }
  }

  /**
    * Return the last observation.
    *
    * @note This is safe to call without synchronization.
    */
  def last: Double = ema

  /**
    * Reset the average to 0 and erase all observations.
    */
  def reset(): Unit = {
    time = Long.MinValue
    ema = 0.0
  }
}
