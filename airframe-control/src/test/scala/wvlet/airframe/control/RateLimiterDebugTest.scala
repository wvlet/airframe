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
 * Debug test with manual ticker to understand token behavior
 */
class RateLimiterDebugTest extends AirSpec {

  test("debug token consumption with manual ticker") {
    val ticker = new ManualTicker(0)
    val limiter = new RateLimiter(10.0, 2, ticker) // 2 tokens max, 10 permits/sec
    
    println(s"Rate: ${limiter.getRate}")
    
    // Should start with 2 tokens
    val result1 = limiter.tryAcquire()
    println(s"After 1st tryAcquire: ${result1}")
    
    val result2 = limiter.tryAcquire()
    println(s"After 2nd tryAcquire: ${result2}")
    
    val result3 = limiter.tryAcquire()
    println(s"After 3rd tryAcquire: ${result3}")
    
    result1 shouldBe true
    result2 shouldBe true  
    result3 shouldBe false
  }
}