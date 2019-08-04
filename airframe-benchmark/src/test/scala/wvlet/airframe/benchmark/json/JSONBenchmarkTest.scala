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

import wvlet.airframe.AirframeSpec
import wvlet.airframe.json.{JSON, JSONTraverser, JSONVisitor}
import wvlet.airframe.json.JSON.{JSONArray, JSONString}
import wvlet.log.io.{IOUtil, Timer}

import scala.util.Random

/**
  *
  */
class JSONBenchmarkTest extends AirframeSpec with Timer {

  import JSONBenchmark._
  val repetition = if (inCI) 2 else 10

  lazy val twitterJson = IOUtil.readAsString("airframe-benchmark/src/test/resources/twitter.json")

  "JSONScannerBenchmark" should {
    "parse twitter.json" taggedAs ("comparison") in {
      bench("twitter.json", twitterJson, N = repetition, B = repetition)
    }

    "parse boolen arrays" taggedAs ("boolean-array") in {
      val jsonArray = s"[${(0 until 10000).map(_ => Random.nextBoolean()).mkString(",")}]"
      bench("boolean array", jsonArray, N = repetition, B = repetition)
    }

    "parse string arrays" taggedAs ("string-array") in {
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
      bench("string array", jsonArray, N = repetition, B = repetition)
    }
  }

}
