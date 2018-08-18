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

import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import wvlet.airframe.AirframeSpec
import wvlet.log.io.{IOUtil, Timer}

/**
  *
  */
class JSONScannerBenchmark extends AirframeSpec with Timer {

  val json = IOUtil.readAsString("airframe-json/src/test/resources/twitter.json")

  "JSONScannerBenchmarhk" should {
    "parse twitter.json" in {
      val jsonBytes      = json.getBytes(StandardCharsets.UTF_8)
      val jsonByteBuffer = ByteBuffer.wrap(jsonBytes)

      time("twitter.json", repeat = 10, blockRepeat = 1) {
        block("airframe (string)    ") {
          JSONScanner.scan(JSONSource.fromString(json), SimpleJSONEventHandler)
        }
        block("airframe (byte buffer)") {
          JSONScanner.scan(JSONSource.fromByteBuffer(jsonByteBuffer), SimpleJSONEventHandler)
        }
        block("airframe (byte array)") {
          JSONScanner.scan(JSONSource.fromBytes(jsonBytes), SimpleJSONEventHandler)
        }
        block("airframe json parser ") {
          JSON.parse(jsonBytes)
        }
//        // Excluded for supporting muiltiple Scala versions
//        block("circe                 ") {
//          io.circe.parser.parse(json)
//        }
//        block("json4s 3.5.4 (native)") {
//          org.json4s.native.JsonMethods.parse(json)
//        }
//        block("json4s 3.5.4 (jackson)") {
//          org.json4s.jackson.JsonMethods.parse(json)
//        }
//        block("uJson (string)        ") {
//          ujson.read(json)
//        }
//        block("uJson (byte array)    ") {
//          ujson.read(jsonBytes)
//        }
      }
    }

    "parser twiter.json as JSONValue" in {
      val j = JSON.parse(json)
    }
  }

}
