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

import wvlet.airframe.Design
import wvlet.airspec.AirSpec
import wvlet.log.LogSupport

/**
  */
class TestJSSpec extends AirSpec with LogSupport {
  protected override def design: Design = {
    Design.newDesign
      .bind[String].toInstance("hello Scala.js")
  }

  test("hello") { (name: String) =>
    debug(name)
    assert(name == "hello Scala.js")
  }

  test("natural method name test") {
    debug("hello symbol name tests")
  }

  test("java.util.logging should be available on Scala.js") {
    // This test verifies that java.util.logging is available (requires scalajs-java-logging dependency)
    val logger = java.util.logging.Logger.getLogger("test.logger")
    logger.info("java.util.logging works on Scala.js")

    // Verify that we can create LogRecord instances
    val record = new java.util.logging.LogRecord(java.util.logging.Level.INFO, "test message")
    record.getMessage shouldBe "test message"

    info("Successfully used java.util.logging on Scala.js platform")
  }
}

class FlexWordSpecTest extends AirSpec {
  test("should support long method names") {
    true
  }
}

class NoTestTest extends AirSpec {
  warn(s"Show warning message if no test is defined")
}
