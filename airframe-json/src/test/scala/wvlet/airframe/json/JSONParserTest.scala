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

import wvlet.airframe.json.JSON._
import wvlet.airspec.AirSpec

/**
  */
class JSONParserTest extends AirSpec {
  scalaJsSupport

  protected def parse(s: String): JSONValue = {
    val v = JSON.parse(JSONSource.fromString(s))
    debug(s"parse ${s}: ${v}")
    v
  }

  def `parser json string`: Unit = {
    parse("{}")
    parse("""{"id":1, "name":"leo", "value":0.1, "num":1000}""")
  }
  def `parse large array of objects`: Unit = {
    val json = (for (_ <- 0 to 10000) yield "{}").mkString("[", ",", "]")
    parse(json)
  }

  def `parse any json values`: Unit = {
    val v = JSON.parseAny("null")
    v shouldBe JSONNull
    JSON.parseAny("1") shouldBe JSONLong(1L)
    JSON.parseAny("1.23") shouldBe JSONDouble(1.23)
    JSON.parseAny("[]") shouldBe JSONArray(IndexedSeq.empty)
    JSON.parseAny("[1, 2]") shouldBe JSONArray(IndexedSeq(JSONLong(1L), JSONLong(2L)))
    JSON.parseAny("""{"id":1}""") shouldBe JSONObject(Seq("id" -> JSONLong(1L)))
  }

  def `parse numeric json values`: Unit = {
    JSON.parseAny(Long.MaxValue.toString) shouldBe JSONLong(Long.MaxValue)
    JSON.parseAny(Long.MinValue.toString) shouldBe JSONLong(Long.MinValue)

    JSON.parseAny(Double.MaxValue.toString) shouldBe JSONDouble(Double.MaxValue)
    JSON.parseAny(Double.MinValue.toString) shouldBe JSONDouble(Double.MinValue)
  }

  def `throw IntegerOverflowException error`: Unit = {
    intercept[IntegerOverflow] {
      JSON.parseAny("9223372036854775808")
    }
    intercept[IntegerOverflow] {
      JSON.parseAny("-9223372036854775809")
    }
  }
}
