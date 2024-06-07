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
package wvlet.log

class LogLevelTest extends Spec {
  test("Set log level with a wildcard pattern") {
    val l = Logger.setLogLevel("example.*", LogLevel.WARN)
    Logger("example.app").getLogLevel shouldBe LogLevel.WARN
    Logger("example3").getLogLevel shouldBe LogLevel.INFO
  }

  test("Set log level with a wild pattern after creating a logger") {
    val l = Logger("example.app")
    l.setLogLevel(LogLevel.INFO)
    val l2 = Logger("example.app.Parser")
    l2.setLogLevel(LogLevel.INFO)

    Logger.setLogLevel("example.*", LogLevel.WARN)
    l.getLogLevel shouldBe LogLevel.WARN
    l2.getLogLevel shouldBe LogLevel.WARN
  }

  test("Set a specific logger's level with setLevel(pattern, level)") {
    val l = Logger("example.app")
    l.setLogLevel(LogLevel.INFO)
    l.getLogLevel shouldBe LogLevel.INFO

    Logger.setLogLevel("example.app", LogLevel.WARN)
    l.getLogLevel shouldBe LogLevel.WARN
  }
}
