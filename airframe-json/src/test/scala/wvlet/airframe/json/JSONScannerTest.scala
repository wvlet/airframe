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
package wvlet.airframe.json

import wvlet.airframe.AirframeSpec

/**
  *
  */
class JSONScannerTest extends AirframeSpec {

  def scan(json: String): Unit = {
    debug(s"scan: ${json}")
    JSONScanner.scan(JSONSource.fromString(json), SimpleJSONEventHandler)
  }

  "JSONScanner" should {
    "parse JSON" in {
      scan("{}")
      scan("[]")

      scan("""{"id":1}""")
      scan("""{"id":1, "name":"leo"}""")

      scan("[0, 1, -1, -1.0, 1.0123, 1.11, 10.234, 1.0e-10, 1.123e+10, 12.3E50]")
      scan("[true, false, null]")
      scan("""{"elem":[0, 1], "data":{"id":"0x0x", "val":0.1234}}""")
    }

    "throw unexpected error" in {
      intercept[UnexpectedToken] {
        scan("{13}")
      }
    }

    "throw EOF" in {
      // workaround: Scala.js throws UndefinedBehaviorError if ArrayIndexOutOfBoundsException is thrown
      intercept[Throwable] {
        scan("{")
      }
    }

  }
}
