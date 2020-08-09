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
import wvlet.airframe.codec.PrimitiveCodec.StringCodec
import wvlet.airspec.AirSpec

/**
  */
object EnumCodecTest extends AirSpec {

  sealed trait Color
  case object Blue extends Color
  case object Red  extends Color

  object Color {
    def values: Seq[Color] = Seq(Blue, Red)
    def unapply(s: String): Option[Color] = {
      values.find(_.toString == s)
    }
  }

  test("read Enum-like classes") {
    val codec = MessageCodec.of[Color]
    codec.unpackMsgPack(codec.toMsgPack(Blue)) shouldBe Some(Blue)
    codec.unpackMsgPack(codec.toMsgPack(Red)) shouldBe Some(Red)
    codec.unpackMsgPack(StringCodec.toMsgPack("Green")) shouldBe empty
  }

  import enumtest._

  test("find unapply(String) from the object") {
    val codec = MessageCodec.of[Status]
    debug(codec)
    codec.unpackMsgPack(codec.toMsgPack(Status.SUCCESS)) shouldBe Some(Status.SUCCESS)
    codec.unpackMsgPack(codec.toMsgPack(Status.FAILURE)) shouldBe Some(Status.FAILURE)
    codec.unpackMsgPack(StringCodec.toMsgPack("unknown")) shouldBe empty
  }

  test("detect invalid Strings for the enum") {
    val codec = MessageCodec.of[Status]
    val e = intercept[MessageCodecException] {
      codec.fromString("unknown")
    }
    e.errorCode shouldBe INVALID_DATA
  }

  test("read enum in case class") {
    val codec = MessageCodec.of[Resp]
    val r1    = codec.fromJson("""{"status":"SUCCESS"}"""")
    r1 shouldBe Resp(Status.SUCCESS)

    val e = intercept[MessageCodecException] {
      val r2 = codec.fromJson("""{"status":"invalid-value"}""")
    }
    e.errorCode shouldBe INVALID_DATA
  }

}

package enumtest {

  sealed trait Status

  object Status {
    def values = Seq(SUCCESS, FAILURE)
    case object SUCCESS extends Status
    case object FAILURE extends Status

    def unapply(s: String): Option[Status] = {
      Status.values.find(_.toString == s)
    }
  }

  case class Resp(status: Status)
}
