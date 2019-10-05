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
  * A classification of the code execution result
  */
sealed trait ResultClass

object ResultClass {

  /**
    * A label for the successful code execution result
    */
  case object Succeeded extends ResultClass

  /**
    * A label for the failed code execution result
    */
  case class Failed(isRetryable: Boolean, cause: Throwable, extraWait: ExtraWait = noExtraWait) extends ResultClass {
    def withExtraWaitMillis(extraWaitMillis: Int): Failed = {
      this.copy(extraWait = ExtraWait(maxExtraWaitMillis = extraWaitMillis))
    }
    def withExtraWaitFactor(factor: Double, maxExtraWaitMillis: Int = 5000): Failed = {
      this.copy(extraWait = ExtraWait(maxExtraWaitMillis = maxExtraWaitMillis, factor = factor))
    }
  }

  private val noExtraWait = ExtraWait()

  case class ExtraWait(maxExtraWaitMillis: Int = 0, factor: Double = 0.0) {
    require(maxExtraWaitMillis >= 0)
    require(factor >= 0)

    def hasNoWait: Boolean = {
      maxExtraWaitMillis == 0 && factor == 0.0
    }

    // Compute the extra wait millis based on the next wait millis
    def extraWaitMillis(nextWaitMillis: Int): Int = {
      if (maxExtraWaitMillis == 0) {
        if (factor == 0.0) {
          0
        } else {
          (nextWaitMillis * factor).toInt
        }
      } else {
        if (factor == 0.0) {
          maxExtraWaitMillis
        } else {
          (nextWaitMillis * factor).toInt.min(maxExtraWaitMillis)
        }
      }
    }
  }

  def retryableFailure(e: Throwable): Failed    = Retry.retryableFailure(e)
  def nonRetryableFailure(e: Throwable): Failed = Retry.nonRetryableFailure(e)

  val ALWAYS_SUCCEED: Any => ResultClass = { x: Any =>
    Succeeded
  }

  val ALWAYS_RETRY: Throwable => ResultClass = { e: Throwable =>
    retryableFailure(e)
  }

}
