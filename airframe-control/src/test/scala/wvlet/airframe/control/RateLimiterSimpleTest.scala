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
 * Simple test suite for RateLimiter to debug issues
 */
class RateLimiterSimpleTest extends AirSpec {

  test("basic creation works") {
    val limiter = RateLimiter.create(10.0)
    limiter.getRate shouldBe 10.0
  }

  test("single tryAcquire works") {
    val limiter = RateLimiter.create(10.0)
    val result = limiter.tryAcquire()
    result shouldBe true
  }

  test("multiple tryAcquire respects burst") {
    val limiter = RateLimiter.create(10.0, 2) // burst of 2
    println(s"Initial rate: ${limiter.getRate}")
    
    val result1 = limiter.tryAcquire()
    println(s"First tryAcquire: ${result1}")
    result1 shouldBe true  // 1st should work
    
    val result2 = limiter.tryAcquire()
    println(s"Second tryAcquire: ${result2}")
    result2 shouldBe true  // 2nd should work (burst)
    
    val result3 = limiter.tryAcquire()
    println(s"Third tryAcquire: ${result3}")
    result3 shouldBe false // 3rd should fail
  }
}