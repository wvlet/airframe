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

import wvlet.airframe.codec.MessageCodecTest.ExtractTest
import wvlet.airframe.codec.PrimitiveCodec.LongCodec
import wvlet.airspec.AirSpec

/**
  */
class MessageCodecTest extends AirSpec {
  scalaJsSupport

  def `have surface`: Unit = {
    val l = LongCodec.surface
    debug(l)
  }

  def `throw an error for invalid data`: Unit = {
    val s = MessageCodec.of[String]
    intercept[Exception] {
      s.unpack(Array.emptyByteArray)
    }
  }

  def `throw an IllegalArgumentException for invalid input`: Unit = {
    val s = MessageCodec.of[Seq[String]]
    intercept[IllegalArgumentException] {
      s.unpack(JSONCodec.toMsgPack("{}"))
    }
  }

  def `unpack empty json`: Unit = {
    val codec = MessageCodec.of[Seq[String]]
    codec.unpackJson("")
  }

  def `unpack empty msgapack`: Unit = {
    val codec = MessageCodec.of[Seq[String]]
    codec.unpackMsgPack(Array.emptyByteArray)
  }

  def `convert JSON to Scala object`: Unit = {
    val obj = MessageCodec.fromJson[ExtractTest](
      """{"id":1, "name":"leo", "flag":true, "number":0.01, "arr":[0, 1, 2], "nil":null}"""
    )
    assert(obj == ExtractTest(1, "leo", true, 0.01, Seq(0, 1, 2), ""))
  }

  def `throw MessageCodecException upon invalid JSON data`: Unit = {
    val ex = intercept[MessageCodecException] {
      val a = MessageCodec.fromJson[ExtractTest]("""{"id":"invalid_id"}""")
    }
    ex.errorCode shouldBe INVALID_DATA
  }

  def `convert Scala object to JSON`: Unit = {
    val json = MessageCodec.toJson(ExtractTest(1, "leo", true, 0.01, Seq(0, 1, 2), null))
    assert(json == """{"id":1,"name":"leo","flag":true,"number":0.01,"arr":[0,1,2],"nil":null}""")
  }

  def `support aliased Seq[Int]` : Unit = {
    val codec = MessageCodec.of[MessageCodecTest.SeqInt]
    val json  = codec.toJson(Seq(1, 2, 3))
    json shouldBe "[1,2,3]"
  }
}

object MessageCodecTest {
  case class ExtractTest(id: Int, name: String, flag: Boolean, number: Double, arr: Seq[Int], nil: String)

  type SeqInt = Seq[Int]
}
