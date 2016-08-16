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

/**
  *
  */
class LogRotationHandlerTest extends Spec {

  "LogRotationHandler" should {

    "rotate log files" in {
      val l = Logger("wvlet.log.rotation")
      val h = new LogRotationHandler("target/log-rotation-test", 5, 10)

      l.resetHandler(h)

      l.info("test message")
      l.info("this logger handler rotates logs and compressed the log archives in .gz format")
      l.info("this is the end of log files")
      h.flush()
      h.close()
    }

  }
}
