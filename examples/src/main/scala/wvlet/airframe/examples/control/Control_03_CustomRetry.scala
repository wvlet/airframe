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
package wvlet.airframe.examples.control

import java.util.concurrent.TimeoutException

import wvlet.airframe.control.Retry
import wvlet.log.LogSupport

/**
  *
  */
object Control_03_CustomRetry extends App with LogSupport {
  val withRetry =
    Retry
      .withJitter()
      .retryOn {
        case e: IllegalArgumentException =>
          Retry.nonRetryableFailure(e)
        case e: TimeoutException =>
          Retry
            .retryableFailure(e)
            // Add extra wait millis
            .withExtraWaitMillis(50)
      }

  withRetry.run {
    debug("Hello Retry!")
  }

  withRetry.run {
    debug("Retryer can be reused for other runs")
  }
}
