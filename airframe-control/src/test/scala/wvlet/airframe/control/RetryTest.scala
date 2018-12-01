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
import wvlet.airframe.control.Retry.{RetryContext, MaxRetryException}

/**
  *
  */
class RetryTest extends AirframeSpec {
  "Control" should {
    "convery a context" in {
      val r =
        Retry
          .withBackOff[String](maxRetry = 3)
          .retryOn { ctx: RetryContext[String] =>
            ctx.context.get shouldBe "hello"
          }

      r.runWithContext("hello") {}
      r.withContext("hello").run {}
    }

    "support backoff retry" in {
      var count = 0

      val r =
        Retry
          .withBackOff(maxRetry = 3)
          .retryOn { s: RetryContext[_] =>
            warn(s"[${s.retryCount}/${s.maxRetry}] ${s.lastError.getMessage}. Retrying in ${s.nextWaitMillis} millis")
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

    "support jitter retry" in {
      var count = 0

      val r =
        Retry
          .withJitter(maxRetry = 3)
          .retryOn { s: RetryContext[_] =>
            warn(s"[${s.retryCount}/${s.maxRetry}] ${s.lastError.getMessage}. Retrying in ${s.nextWaitMillis} millis")
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
