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
package wvlet.airframe.log
import wvlet.log.{LogLevel, Logger, Spec}

import java.io.StringReader
import java.util.Properties

/**
  */
class LogTest extends Spec {
  test("should support initialization") {
    wvlet.airframe.log.init
    debug("hello")

    wvlet.airframe.log.initNoColor
    debug("hello")
  }

  test("Test setting root log level") {
    val p            = new Properties()
    val rootLogLevel = Logger.getDefaultLogLevel
    try {
      p.load(new StringReader("_root_ = trace"))
      Logger.setLogLevels(p)
      Logger.getDefaultLogLevel shouldBe LogLevel.TRACE
    } finally {
      Logger.setDefaultLogLevel(rootLogLevel)
    }
  }
}
