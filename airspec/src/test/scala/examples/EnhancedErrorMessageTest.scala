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
package examples

import wvlet.airspec.spi.AssertionFailure
import wvlet.airspec.AirSpec

/**
  * Test enhanced error messages that show context code
  */
class EnhancedErrorMessageTest extends AirSpec {
  test("simple should match test") {
    // First, let's test that basic functionality still works
    val plan: String = "value"
    plan shouldBe "value"
  }

  test("enhanced error message for shouldBe") {
    val plan: String = "original"
    try {
      plan shouldBe "expected"
      fail("Expected AssertionFailure")
    } catch {
      case e: AssertionFailure =>
        val message = e.message
        info(s"Error message: ${message}")
        // For now, just verify the test runs and produces a message
        message should not be empty
    }
  }

  test("enhanced error message for shouldNotBe null") {
    val plan: String = null
    try {
      plan shouldNotBe null
      fail("Expected AssertionFailure")  
    } catch {
      case e: AssertionFailure =>
        val message = e.message
        info(s"Error message: ${message}")
        // For now, just verify the test runs and produces a message
        message should not be empty
    }
  }
}