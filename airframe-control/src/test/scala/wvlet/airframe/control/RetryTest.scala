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

import wvlet.airframe.control.Retry.{MaxRetryException, RetryContext}
import wvlet.airspec.AirSpec

import scala.concurrent.TimeoutException

/**
  */
class RetryTest extends AirSpec {

  test("support backoff retry") {
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

  test("support bounded backoff retry") {
    val maxWait = 50000
    var r =
      Retry
        .withBoundedBackoff(initialIntervalMillis = 1000, maxTotalWaitMillis = maxWait)
        .noRetryLogging

    r = r.init()
    var waitTotal = 0
    while (r.canContinue) {
      waitTotal += r.nextWaitMillis
      r = r.nextRetry(new IllegalStateException())
    }
    waitTotal <= maxWait shouldBe true
  }

  test("support jitter retry") {
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

  test("throw max retry exception") {
    val e = intercept[MaxRetryException] {
      Retry
        .withBackOff(maxRetry = 3)
        .retryOn { case e: IllegalStateException =>
          warn(e.getMessage)
          ResultClass.retryableFailure(e)
        }
        .run {
          logger.info("hello retry")
          throw new IllegalStateException("retry test")
        }
    }

    e.retryContext.maxRetry shouldBe 3
    e.retryContext.retryCount shouldBe 3
    e.retryContext.lastError.getClass shouldBe classOf[IllegalStateException]
  }

  test("change retry wait strategy") {
    val r = Retry
      .withJitter()
      .withBackOff(initialIntervalMillis = 3)

    r.retryWaitStrategy.retryPolicyConfig.initialIntervalMillis shouldBe 3

    val j = r.withJitter(initialIntervalMillis = 20)
    j.retryWaitStrategy.retryPolicyConfig.initialIntervalMillis shouldBe 20

    val m = j.withMaxRetry(100)
    m.maxRetry shouldBe 100
  }

  test("pass the execution context") {
    val r = Retry.withBackOff(initialIntervalMillis = 0)

    var count   = 0
    var checked = false
    r.beforeRetry { (ctx: RetryContext) =>
      ctx.context shouldBe Some("hello world")
      checked = true
    }
      .runWithContext("hello world") {
        if (count == 0) {
          count += 1
          throw new Exception()
        }
      }

    count shouldBe 1
    checked shouldBe true
  }

  test("add extra wait") {
    intercept[MaxRetryException] {
      Retry
        .withBackOff(initialIntervalMillis = 10)
        .withMaxRetry(1)
        .withErrorClassifier { case e: TimeoutException =>
          ResultClass.retryableFailure(e).withExtraWaitMillis(100)
        }
        .beforeRetry { (ctx: RetryContext) =>
          debug(s"${ctx.retryCount} ${ctx.nextWaitMillis}")
          ctx.nextWaitMillis shouldBe 10 + 100
        }
        .run {
          throw new TimeoutException()
        }
    }
  }

  test("add extra wait factor") {
    intercept[MaxRetryException] {
      Retry
        .withBackOff(initialIntervalMillis = 10)
        .withMaxRetry(1)
        .withErrorClassifier { case e: TimeoutException =>
          ResultClass.retryableFailure(e).withExtraWaitFactor(0.2)
        }
        .beforeRetry { (ctx: RetryContext) =>
          debug(s"${ctx.retryCount} ${ctx.nextWaitMillis}")
          ctx.nextWaitMillis shouldBe 10 + 2
        }
        .run {
          throw new TimeoutException()
        }
    }
  }
}
