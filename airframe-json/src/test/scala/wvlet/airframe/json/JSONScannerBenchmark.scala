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
import wvlet.log.io.IOUtil

/**
  *
  */
class JSONScannerBenchmark extends AirframeSpec {
  "JSONScannerBenchmarhk" should {

    "parse twitter.json" in {
      val json = IOUtil.readAsString("airframe-json/src/test/resources/twitter.json")
      JSONScanner.scan(json, SimpleJSONEventHandler)
    }
  }

}
