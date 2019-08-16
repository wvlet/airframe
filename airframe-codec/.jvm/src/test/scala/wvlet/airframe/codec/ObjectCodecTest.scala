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

import wvlet.airframe.codec.ObjectCodecTest._
import wvlet.airframe.msgpack.spi.MessageFormat.FIXMAP
import wvlet.airframe.msgpack.spi.MessagePack

/**
  *
  */
class ObjectCodecTest extends CodecSpec {
  val codec = MessageCodec.of[A1].asInstanceOf[ObjectCodec[A1]]

  def `support reading map value`: Unit = {
    val v: A1  = A1(1, 2, 3, 4, 5, 6, true, "str")
    val packer = MessagePack.newBufferPacker
    codec.packAsMap(packer, v)
    val b = packer.toByteArray

    val h = new MessageHolder
    codec.unpack(MessagePack.newUnpacker(b), h)

    h.isNull shouldBe false
    h.hasError shouldBe false
    h.getDataType shouldBe DataType.ANY
    h.getLastValue shouldBe v
  }

  def `populate the default value when missing`: Unit = {
    val packer = MessagePack.newBufferPacker
    packer.packMapHeader(1)
    packer.packString("i")
    packer.packInt(10)
    val b = packer.toByteArray

    val h = new MessageHolder
    MessageCodec.of[A2].unpack(MessagePack.newUnpacker(b), h)

    h.isNull shouldBe false
    h.hasError shouldBe false
    h.getDataType shouldBe DataType.ANY
    // map input, constructor default, Zero
    h.getLastValue shouldBe A2(10, 2L, 0)
  }

  def `populate case class with Option`: Unit = {
    val codec = MessageCodec.of[A3]

    {
      val json    = """{"opt":null, "str":"hello"}"""
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

  def `write as map type message pack`: Unit = {
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
}

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
}
