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
package wvlet.airframe.benchmark.json

import wvlet.airframe.json.{JSON, JSONScanner, JSONSource, NullJSONContext}
import wvlet.log.io.Timer

/**
  *
  */
object JSONBenchmark extends Timer {

  def bench(benchName: String, json: String, N: Int, B: Int): Unit = {
    val jsonSource = JSONSource.fromString(json)
    time(benchName, repeat = N, blockRepeat = B) {
      block("airframe      ") {
        JSON.parse(jsonSource)
      }
      block("airframe scan ") {
        JSONScanner.scan(jsonSource, new NullJSONContext(isObject = true))
      }
      block("circe         ") {
        io.circe.parser.parse(json)
      }
      block("jawn          ") {
        new io.circe.jawn.JawnParser().parse(json)
      }
      block("json4s-jackson") {
        org.json4s.jackson.JsonMethods.parse(json)
      }
      block("json4s-native ") {
        org.json4s.native.JsonMethods.parse(json)
      }
      block("uJson         ") {
        ujson.read(json)
      }
    }
  }

}
