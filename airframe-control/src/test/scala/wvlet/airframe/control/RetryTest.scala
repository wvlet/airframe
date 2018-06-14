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

import wvlet.airframe.AirframeSpec
import wvlet.airframe.control.Retry.{LastError, MaxRetryException}

/**
  *
  */
class RetryTest extends AirframeSpec {
  "Control" should {

    "support retry" in {
      var count = 0

      val r =
        Retry
          .withBackOff(maxRetry = 3)
          .retryOn { s: LastError =>
            warn(s"[${s.retryCount}/${s.maxRetry}] ${s.lastError.getMessage}")
          }
          .run {
            logger.info("hello retry")
            if (count < 2) {
              count += 1
              throw new IllegalStateException("retry test")
            } else {
              "success"
            }
          }

      r shouldBe "success"
    }

    "throw max retry exception" in {
      val e = intercept[MaxRetryException] {
        Retry
          .withBackOff(maxRetry = 3)
          .retryOnError {
            case e: IllegalStateException =>
              warn(e.getMessage)
          }
          .run {
            logger.info("hello retry")
            throw new IllegalStateException("retry test")
          }
      }

      e.retryState.maxRetry shouldBe 3
      e.retryState.lastError shouldBe a[IllegalStateException]
    }

  }
}
