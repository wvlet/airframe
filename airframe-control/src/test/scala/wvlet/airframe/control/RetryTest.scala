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
import wvlet.airframe.control.Retry.MaxRetryException

/**
  *
  */
class RetryTest extends AirframeSpec {
  "support backoff retry" in {
    var count = 0

    val r =
      Retry
        .withBackOff(maxRetry = 3)

    val e = r.run {
      count += 1
      logger.info("hello retry")
      if (count < 3) {
        throw new IllegalStateException("retry test")
      } else {
        "success"
      }
    }

    e shouldBe "success"
    count shouldBe 3
  }

  "support jitter retry" in {
    var count = 0

    val r =
      Retry
        .withJitter(maxRetry = 3)
        .run {
          count += 1
          logger.info("hello retry")
          if (count < 2) {
            throw new IllegalStateException("retry test")
          } else {
            "success"
          }
        }

    r shouldBe "success"
    count shouldBe 2
  }

  "throw max retry exception" in {
    val e = intercept[MaxRetryException] {
      Retry
        .withBackOff(maxRetry = 3)
        .retryOn {
          case e: IllegalStateException =>
            warn(e.getMessage)
        }
        .run {
          logger.info("hello retry")
          throw new IllegalStateException("retry test")
        }
    }

    e.retryState.maxRetry shouldBe 3
    e.retryState.retryCount shouldBe 3
    e.retryState.lastError shouldBe a[IllegalStateException]
  }
}
