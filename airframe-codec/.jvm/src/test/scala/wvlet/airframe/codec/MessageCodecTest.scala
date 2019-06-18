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

import wvlet.airframe.AirframeSpec
import wvlet.airframe.codec.MessageCodecTest.ExtractTest
import wvlet.airframe.codec.PrimitiveCodec.LongCodec

/**
  *
  */
class MessageCodecTest extends AirframeSpec {
  "MessageCodec" should {

    "have surface" in {
      val l = LongCodec.surface
      debug(l)
    }

    "throw an error for invalid data" in {
      val s = MessageCodec.of[String]
      intercept[Exception] {
        s.unpack(Array.emptyByteArray)
      }
    }

    "throw an IllegalArgumentException for invalid input" in {
      val s = MessageCodec.of[Seq[String]]
      intercept[IllegalArgumentException] {
        s.unpack(JSONCodec.toMsgPack("{}"))
      }
    }

    "unpack empty json" in {
      val codec = MessageCodec.of[Seq[String]]
      codec.unpackJson("")
    }

    "unpack empty msgapack" in {
      val codec = MessageCodec.of[Seq[String]]
      codec.unpackMsgPack(Array.emptyByteArray)
    }

    "convert JSON to Scala object" in {
      val obj = MessageCodec.fromJson[ExtractTest](
        """{"id":1, "name":"leo", "flag":true, "number":0.01, "arr":[0, 1, 2], "nil":null}""")
      assert(obj == ExtractTest(1, "leo", true, 0.01, Seq(0, 1, 2), ""))
    }

    "throw MessageCodecException upon invalid JSON data" in {
      val ex = intercept[MessageCodecException[_]] {
        val a = MessageCodec.fromJson[ExtractTest]("""{"id":"invalid_id"}""")
      }
      ex.errorCode shouldBe INVALID_DATA
    }

    "convert Scala object to JSON" in {
      val json = MessageCodec.toJson(ExtractTest(1, "leo", true, 0.01, Seq(0, 1, 2), null))
      assert(json == """{"id":1,"name":"leo","flag":true,"number":0.01,"arr":[0,1,2],"nil":null}""")
    }

  }

}

object MessageCodecTest {
  case class ExtractTest(id: Int, name: String, flag: Boolean, number: Double, arr: Seq[Int], nil: String)
}
