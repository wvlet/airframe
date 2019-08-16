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
package wvlet.airframe.codec

import wvlet.airframe.codec.JSONCodecTest.WithRawJSON
import wvlet.airframe.json.{JSON, Json}
import wvlet.airspec.AirSpec

/**
  *
  */
class JSONCodecTest extends AirSpec {
  protected def check(json: String): Unit = {
    val b = JSONCodec.toMsgPack(json)
    JSONCodec.unpackMsgPack(b) match {
      case Some(parsedJson) =>
        JSON.parseAny(parsedJson) shouldBe JSON.parseAny(json)
      case None =>
        fail(s"Failed to ser/de ${json}")
    }
  }

  def `serialize json into msgpack`: Unit = {
    check(
      """{"id":1, "name":"leo", "address":["xxx", "yyy"], "flag":true, "float":1.234, "nil":null, "nested":{"message":"hello"}}""")
    check("[1]")
    check("[12342345324234234]")
    check("[0.12]")
    check("[\"hello world\"]")
    check("[true]")
    check("[false]")
    check("[null]")
    check("""[1, 2, 3.0, "apple", true, false]""")
    check("{}")
    check("[]")
  }

  def `serialize non-array/object json values`: Unit = {
    check("true")
    check("null")
    check("false")
    check("1")
    check("1.0e1")
    check("1.234")
  }

  val json1      = """{"id":1, "name":"leo", "flag":true, "number":0.01, "arr":[0, 1, 2], "nil":null}"""
  val json1Value = JSON.parse(json1)

  def `support JSONValue mapping`: Unit = {
    val msgpackOfJson1 = JSONValueCodec.toMsgPack(json1Value)
    JSONValueCodec.unpackMsgPack(msgpackOfJson1) shouldBe Some(json1Value)
  }

  def `support raw json mapping`: Unit = {
    val codec = MessageCodec.of[WithRawJSON]
    // JSON -> msgpack -> WithRawJSON
    val obj = codec.unpackJson(s"""{"json":${json1}}""")
    obj shouldBe defined
    val v = obj.get
    JSON.parse(v.json) shouldBe json1Value

    // WithRawJSON -> msgpack -> WithRawJSON
    val msgpack = codec.toMsgPack(v)
    codec.unpackMsgPack(msgpack) shouldBe Some(v)
  }

}

object JSONCodecTest {
  case class WithRawJSON(json: Json)
}
