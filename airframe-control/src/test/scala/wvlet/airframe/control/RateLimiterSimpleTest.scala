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

/**
  * Simple test suite for RateLimiter basic functionality
  */
class RateLimiterSimpleTest extends AirSpec {

  test("basic creation works") {
    val limiter = new RateLimiter(10.0)
    limiter.getRate shouldBe 10.0
  }

  test("single tryAcquire works") {
    val limiter = new RateLimiter(10.0)
    val result  = limiter.tryAcquire()
    result shouldBe true
  }

  test("multiple tryAcquire respects burst") {
    val ticker  = new ManualTicker(0)
    val limiter = new RateLimiter(10.0, 2, ticker) // burst of 2

    val result1 = limiter.tryAcquire()
    result1 shouldBe true // 1st should work

    val result2 = limiter.tryAcquire()
    result2 shouldBe true // 2nd should work (burst)

    val result3 = limiter.tryAcquire()
    result3 shouldBe false // 3rd should fail
  }

  test("tokens refill over time") {
    val ticker  = new ManualTicker(0)
    val limiter = new RateLimiter(2.0, 3, ticker) // 2 permits/sec, burst of 3

    // Consume all tokens
    limiter.tryAcquire(3) shouldBe true

    // No more tokens
    limiter.tryAcquire() shouldBe false

    // Advance time by 1 second
    ticker.tick(1000000000L)

    // Should have 2 new tokens
    limiter.tryAcquire(2) shouldBe true
    limiter.tryAcquire() shouldBe false
  }
}
