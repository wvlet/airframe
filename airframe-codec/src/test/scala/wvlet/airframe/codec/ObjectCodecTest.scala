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

import wvlet.airframe.msgpack.spi.MessageFormat.FIXMAP
import wvlet.airframe.msgpack.spi.MessagePack
import wvlet.airframe.surface.required

object ObjectCodecTest {
  case class A1(
      i: Int,
      l: Long,
      f: Float,
      d: Double,
      c: Char,
      st: Short,
      b: Boolean,
      s: String
  )

  case class A2(i: Int, l: Long = 2L, i2: Int)

  case class A3(opt: Option[String], str: String)

  case class B(@required name: String)

  case class Nested(i: Int, s: String)
  case class JSONField(v: Nested)
}

/**
  */
class ObjectCodecTest extends CodecSpec {
  import wvlet.airframe.codec.ObjectCodecTest._

  val codec = MessageCodec.of[A1].asInstanceOf[ObjectMapCodec[A1]]

  test("support reading map value") {
    val v: A1  = A1(1, 2, 3, 4, 5, 6, true, "str")
    val packer = MessagePack.newBufferPacker
    codec.packAsMap(packer, v)
    val b = packer.toByteArray

    val h = new MessageContext
    codec.unpack(MessagePack.newUnpacker(b), h)

    h.isNull shouldBe false
    h.hasError shouldBe false
    h.getDataType shouldBe DataType.ANY
    h.getLastValue shouldBe v
  }

  test("populate the default value when missing") {
    pending("TODO")
    val packer = MessagePack.newBufferPacker
    packer.packMapHeader(1)
    packer.packString("i")
    packer.packInt(10)
    val b = packer.toByteArray

    val h = new MessageContext
    MessageCodec.of[A2].unpack(MessagePack.newUnpacker(b), h)

    h.isNull shouldBe false
    h.hasError shouldBe false
    h.getDataType shouldBe DataType.ANY
    // map input, constructor default, Zero
    h.getLastValue shouldBe A2(10, 2L, 0)
  }

  test("populate case class with Option") {
    val codec = MessageCodec.of[A3]

    {
      val json    = """{"str":"hello"}"""
      val a3      = A3(None, "hello")
      val msgpack = JSONCodec.toMsgPack(json)
      val a       = codec.unpackMsgPack(msgpack)
      a shouldBe Some(a3)

      // Helper method to read JSON
      codec.unpackJson(json) shouldBe Some(a3)

      // Check codec.toJson produces Map values
      JSONCodec.toMsgPack(codec.toJson(a3)) shouldBe msgpack
    }

    {
      val json    = """{"opt":"hello", "str":"world"}"""
      val a3      = A3(Some("hello"), "world")
      val msgpack = JSONCodec.toMsgPack(json)
      val a       = codec.unpackMsgPack(msgpack)
      a shouldBe Some(a3)

      codec.unpackJson(json) shouldBe Some(a3)

      // Check codec.toJson produces Map values
      JSONCodec.toMsgPack(codec.toJson(a3)) shouldBe msgpack
    }
  }

  test("write as map type message pack") {
    val codec    = MessageCodec.of[A3]
    val a3       = A3(Some("optValue"), "strValue")
    val msgpack  = codec.toMsgPack(a3)
    val unpacker = MessagePack.newUnpacker(msgpack)
    // The written message pack should be FIXMAP type for A3 class
    unpacker.getNextFormat shouldBe FIXMAP

    // Succeed to unpack to object.
    val a = codec.unpackMsgPack(msgpack)
    a shouldBe Some(a3)
  }

  test("support @required annotation") {
    val codec = MessageCodec.of[B]
    val ex = intercept[MessageCodecException] {
      codec.unpackJson("{}")
    }
    warn(ex.getMessage)
    ex.errorCode shouldBe MISSING_PARAMETER
  }

  test("nested JSON field") {
    val codec = MessageCodec.of[JSONField]
    val p     = MessagePack.newBufferPacker
    p.packArrayHeader(1)
    p.packString("""{"i":1,"s":"hello"}""")
    val msgpack = p.toByteArray
    codec.fromMsgPack(msgpack) shouldBe JSONField(Nested(1, "hello"))
  }
}
