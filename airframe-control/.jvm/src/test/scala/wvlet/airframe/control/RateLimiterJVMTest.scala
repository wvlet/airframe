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
import java.util.concurrent.atomic.AtomicInteger

/**
 * JVM-specific tests for RateLimiter that use java.lang.Thread
 */
class RateLimiterJVMTest extends AirSpec {

  test("handle concurrent access") {
    val limiter               = RateLimiter.create(100.0) // High rate to avoid blocking in test
    val counter               = new AtomicInteger(0)
    val threads               = 10
    val acquisitionsPerThread = 10

    val threadList = (1 to threads).map { _ =>
      new Thread(() => {
        for (_ <- 1 to acquisitionsPerThread) {
          limiter.acquire()
          counter.incrementAndGet()
        }
      })
    }

    threadList.foreach(_.start())
    threadList.foreach(_.join())

    counter.get() shouldBe threads * acquisitionsPerThread
  }
}