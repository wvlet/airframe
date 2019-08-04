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
package wvlet.airframe.benchmark

import wvlet.airframe.json.JSON.{JSONArray, JSONString}
import wvlet.airframe.json.{JSON, JSONScanner, JSONSource, JSONTraverser, JSONVisitor, NullJSONContext}
import wvlet.log.io.{IOUtil, Timer}

import scala.util.Random

/**
  *
  */
object JSONBenchmark extends Timer {

  val twitterJson = IOUtil.readAsString("twitter.json")

  def twitterJson(N: Int = 10, B: Int = 10): Unit = {
    bench("twitter.json", twitterJson, N = N, B = N)
  }

  def booleanArrayBench(N: Int = 10, B: Int = 10): Unit = {
    val jsonArray = s"[${(0 until 10000).map(_ => Random.nextBoolean()).mkString(",")}]"
    bench("boolean array", jsonArray, N = N, B = B)
  }

  def stringArrayBench(N: Int = 10, B: Int = 10): Unit = {
    // Extract JSON strings from twitter.json
    val j = JSON.parse(twitterJson)
    val b = IndexedSeq.newBuilder[JSONString]
    JSONTraverser.traverse(j, new JSONVisitor {
      override def visitKeyValue(k: String, v: JSON.JSONValue): Unit = {
        b += JSONString(k)
      }
      override def visitString(v: JSON.JSONString): Unit = {
        b += v
      }
    })
    val jsonArray = JSONArray(b.result()).toJSON
    bench("string array", jsonArray, N = N, B = B)
  }

  def runAll(N: Int = 10, B: Int = 10): Unit = {
    twitterJson(N, B)
    booleanArrayBench(N, B)
    stringArrayBench(N, B)
  }

  private def bench(benchName: String, json: String, N: Int, B: Int): Unit = {
    val jsonSource = JSONSource.fromString(json)
    time(benchName, repeat = N, blockRepeat = B) {
      block("airframe scan-only") {
        JSONScanner.scan(jsonSource, new NullJSONContext(isObject = true))
      }
      block("airframe          ") {
        JSON.parse(jsonSource)
      }
      block("circe             ") {
        io.circe.parser.parse(json)
      }
      block("jawn              ") {
        new io.circe.jawn.JawnParser().parse(json)
      }
      block("json4s-jackson    ") {
        org.json4s.jackson.JsonMethods.parse(json)
      }
      block("json4s-native     ") {
        org.json4s.native.JsonMethods.parse(json)
      }
      block("uJson             ") {
        ujson.read(json)
      }
    }
  }

}
