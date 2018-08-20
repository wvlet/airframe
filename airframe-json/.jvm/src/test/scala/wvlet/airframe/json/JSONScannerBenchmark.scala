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

import io.circe.jawn.JawnParser
import wvlet.airframe.AirframeSpec
import wvlet.airframe.json.JSON.{JSONArray, JSONString}
import wvlet.log.io.{IOUtil, Timer}

import scala.util.Random

/**
  *
  */
class JSONScannerBenchmark extends AirframeSpec with Timer {

  val json      = IOUtil.readAsString("airframe-json/src/test/resources/twitter.json")
  val jsonBytes = json.getBytes(StandardCharsets.UTF_8)

  "JSONScannerBenchmarhk" should {
    "parse twitter.json" taggedAs ("comparison") in {
      val jsonByteBuffer = ByteBuffer.wrap(jsonBytes)
      val jawnParser     = new JawnParser()
      time("twitter.json", repeat = 10, blockRepeat = 10) {
//        block("airframe (string)    ") {
//          JSONScanner.scan(JSONSource.fromString(json), SimpleJSONEventHandler)
//        }
//        block("airframe (byte buffer)") {
//          JSONScanner.scan(JSONSource.fromByteBuffer(ByteBuffer.wrap(jsonBytes)), SimpleJSONEventHandler)
//        }
        // Excluded for supporting muiltiple Scala versions
        block("jawn                  ") {
          jawnParser.parse(json)
        }
        block("circe                 ") {
          io.circe.parser.parse(json)
        }
        block("airframe (push parser) ") {
          JSONScanner.scan(JSONSource.fromBytes(jsonBytes), SimpleJSONEventHandler)
        }
        block("json4s 3.5.4 (native)") {
          org.json4s.native.JsonMethods.parse(json)
        }
        block("json4s 3.5.4 (jackson)") {
          org.json4s.jackson.JsonMethods.parse(json)
        }
        block("airframe json parser ") {
          JSON.parse(jsonBytes)
        }
//        block("uJson (string)        ") {
//          ujson.read(json)
//        }
//        block("uJson (byte array)    ") {
//          ujson.read(jsonBytes)
//        }
      }
    }

    "parse twitter.json bytes" taggedAs ("airframe-push") in {
      time("airframe-push", repeat = 10, blockRepeat = 1) {
        block("airframe (byte array)") {
          JSONScanner.scan(JSONSource.fromBytes(jsonBytes), SimpleJSONEventHandler)
        }
      }
    }

    "parse boolen arrays" taggedAs ("boolean-array") in {
      val jsonArray = s"[${(0 until 10000).map(_ => Random.nextBoolean()).mkString(",")}]"
      val s         = JSONSource.fromString(jsonArray)

      time("boolean array", repeat = 10) {
        JSONScanner.scan(s, SimpleJSONEventHandler)
      }
    }

    "parse string arrays" taggedAs ("string-array") in {
      // Extract JSON strings from twitter.json
      val j = JSON.parse(json)
      val b = Seq.newBuilder[JSONString]
      JSONTraverser.traverse(j, new JSONVisitor {
        override def visitKeyValue(k: String, v: JSON.JSONValue): Unit = {
          b += JSONString(k)
        }
        override def visitString(v: JSON.JSONString): Unit = {
          b += v
        }
      })
      val jsonArray = JSONArray(b.result()).toJSON
      val s         = JSONSource.fromString(jsonArray)

      time("string array", repeat = 10) {
        JSONScanner.scan(s, SimpleJSONEventHandler)
      }
    }

    "parser twiter.json as JSONValue" in {
      val j = JSON.parse(json)
    }
  }

}
