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

import wvlet.airspec.AirSpec
import java.util.concurrent.TimeUnit

/**
  * Test suite for RateLimiter
  */
class RateLimiterTest extends AirSpec {

  test("create rate limiter with valid parameters") {
    val limiter = RateLimiter.create(10.0)
    limiter.getRate shouldBe 10.0
  }

  test("create rate limiter with burst size") {
    val limiter = RateLimiter.create(5.0, 20)
    limiter.getRate shouldBe 5.0
  }

  test("reject invalid parameters") {
    intercept[IllegalArgumentException] {
      RateLimiter.create(0.0)
    }

    intercept[IllegalArgumentException] {
      RateLimiter.create(-1.0)
    }
  }

  test("acquire single permit immediately when tokens available") {
    val limiter  = RateLimiter.create(10.0)
    val waitTime = limiter.acquire()
    waitTime shouldBe 0L
  }

  test("tryAcquire single permit immediately when tokens available") {
    val limiter = RateLimiter.create(10.0)
    val result  = limiter.tryAcquire()
    result shouldBe true
  }

  test("tryAcquire multiple permits") {
    val limiter = RateLimiter.create(10.0)
    val result  = limiter.tryAcquire(5)
    result shouldBe true
  }

  test("reject invalid permit counts") {
    val limiter = RateLimiter.create(10.0)

    intercept[IllegalArgumentException] {
      limiter.acquire(0)
    }

    intercept[IllegalArgumentException] {
      limiter.acquire(-1)
    }

    intercept[IllegalArgumentException] {
      limiter.tryAcquire(0)
    }

    intercept[IllegalArgumentException] {
      limiter.tryAcquire(-1)
    }
  }

  test("respect rate limiting with manual ticker") {
    val ticker  = new ManualTicker(0)
    val limiter = new RateLimiter(2.0, ticker = ticker) // 2 permits per second

    // First permit should be immediate
    limiter.tryAcquire() shouldBe true

    // Second permit should be immediate (burst)
    limiter.tryAcquire() shouldBe true

    // Third permit should fail without time advancement
    limiter.tryAcquire() shouldBe false

    // Advance time by 0.5 seconds (500ms = 500M nanos)
    ticker.tick(500000000L)

    // Now one more permit should be available
    limiter.tryAcquire() shouldBe true

    // But not another one
    limiter.tryAcquire() shouldBe false
  }

  test("token bucket refill over time") {
    val ticker  = new ManualTicker(0)
    val limiter = new RateLimiter(1.0, 5, ticker) // 1 permit per second, max 5 burst

    // Consume all initial tokens
    for (_ <- 0 until 5) {
      limiter.tryAcquire() shouldBe true
    }

    // No more tokens available
    limiter.tryAcquire() shouldBe false

    // Advance time by 3 seconds
    ticker.tick(3000000000L)

    // Should have 3 new tokens available
    limiter.tryAcquire(3) shouldBe true
    limiter.tryAcquire() shouldBe false
  }

  test("withRate creates new limiter with different rate") {
    val original = RateLimiter.create(5.0)
    val modified = original.withRate(10.0)

    original.getRate shouldBe 5.0
    modified.getRate shouldBe 10.0
  }

  test("withMaxBurstSize creates new limiter with different burst size") {
    val original = RateLimiter.create(5.0, 10)
    val modified = original.withMaxBurstSize(20)

    // Both should have the same rate
    original.getRate shouldBe 5.0
    modified.getRate shouldBe 5.0
  }

  test("tryAcquire with timeout succeeds when tokens available") {
    val limiter = RateLimiter.create(10.0)
    val result  = limiter.tryAcquire(1, 100, TimeUnit.MILLISECONDS)
    result shouldBe true
  }

  test("tryAcquire with timeout respects timeout") {
    val ticker  = new ManualTicker(0)
    val limiter = new RateLimiter(1.0, ticker = ticker) // Very slow rate

    // Consume initial token
    limiter.tryAcquire() shouldBe true

    // This should timeout because we need to wait 1 second for next token
    val start   = System.nanoTime()
    val result  = limiter.tryAcquire(1, 100, TimeUnit.MILLISECONDS)
    val elapsed = System.nanoTime() - start

    result shouldBe false
    // Should not have waited the full second
    (elapsed < 1000000000L) shouldBe true
  }

  test("reject negative timeout") {
    val limiter = RateLimiter.create(10.0)
    intercept[IllegalArgumentException] {
      limiter.tryAcquire(1, -1, TimeUnit.MILLISECONDS)
    }
  }

  test("demonstrate burst behavior") {
    val ticker  = new ManualTicker(0)
    val limiter = new RateLimiter(2.0, 4, ticker) // 2 permits/sec, max burst of 4

    // Should be able to acquire burst of 4 immediately
    limiter.tryAcquire(4) shouldBe true

    // No more permits available
    limiter.tryAcquire() shouldBe false

    // After 1 second, should have 2 more permits
    ticker.tick(1000000000L)
    limiter.tryAcquire(2) shouldBe true

    // But not more than that
    limiter.tryAcquire() shouldBe false
  }

  test("handle edge case of very high rate") {
    val limiter = RateLimiter.create(1000000.0) // 1M permits per second

    // Should be able to acquire many permits quickly
    for (_ <- 1 to 100) {
      limiter.tryAcquire() shouldBe true
    }
  }

  test("handle edge case of very low rate") {
    val ticker  = new ManualTicker(0)
    val limiter = new RateLimiter(0.1, ticker = ticker) // 1 permit per 10 seconds

    // First permit should be available
    limiter.tryAcquire() shouldBe true

    // No more until significant time passes
    limiter.tryAcquire() shouldBe false

    // After 10 seconds
    ticker.tick(10000000000L)
    limiter.tryAcquire() shouldBe true
  }
}
