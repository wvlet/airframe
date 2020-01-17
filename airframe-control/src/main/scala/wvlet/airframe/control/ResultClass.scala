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

import wvlet.airframe.control.Retry.ExtraWait

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
  case class Failed(isRetryable: Boolean, cause: Throwable, extraWait: ExtraWait = Retry.noExtraWait)
      extends ResultClass {
    def withExtraWaitMillis(extraWaitMillis: Int): Failed = {
      this.copy(extraWait = ExtraWait(maxExtraWaitMillis = extraWaitMillis))
    }
    def withExtraWaitFactor(factor: Double, maxExtraWaitMillis: Int = 5000): Failed = {
      this.copy(extraWait = ExtraWait(maxExtraWaitMillis = maxExtraWaitMillis, factor = factor))
    }
  }

  def retryableFailure(e: Throwable): Failed    = Retry.retryableFailure(e)
  def nonRetryableFailure(e: Throwable): Failed = Retry.nonRetryableFailure(e)

  def ALWAYS_SUCCEED: Any => ResultClass = { x: Any =>
    Succeeded
  }

  def ALWAYS_RETRY: Throwable => ResultClass.Failed = { e: Throwable =>
    retryableFailure(e)
  }
}
