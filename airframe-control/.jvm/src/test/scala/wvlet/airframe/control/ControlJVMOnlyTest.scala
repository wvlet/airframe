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

import scala.concurrent.TimeoutException

/**
  */
class ControlJVMOnlyTest extends AirSpec {

  test("support failure rate health checker") {
    val cb = CircuitBreaker.withFailureRate(0.01, timeWindowMillis = 1000)
    val e  = new TimeoutException()
    cb.isConnected shouldBe true

    // 1/1
    cb.recordSuccess
    cb.isConnected shouldBe true

    // 1/2
    Thread.sleep(200)
    cb.recordFailure(e)
    cb.isConnected shouldBe false

    // 1/3
    Thread.sleep(200)
    cb.recordFailure(e)
    cb.isConnected shouldBe false

    // Force probing
    cb.halfOpen

    // The state should be recovered after the successful request
    cb.recordSuccess
    cb.isConnected shouldBe true
  }

}
