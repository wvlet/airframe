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
package wvlet.airframe.tablet.msgpack

import org.msgpack.core.MessagePack
import wvlet.airframe.tablet.Schema

/**
  *
  */
class PrimitiveCodecTest extends CodecSpec {
  "PrimitiveCodec" should {

    "support numeric" in {
      roundTripTestWithStr[Int](Schema.INTEGER)
      roundTripTestWithStr[Byte](Schema.INTEGER)
      roundTripTestWithStr[Short](Schema.INTEGER)
      roundTripTestWithStr[Long](Schema.INTEGER)
      roundTripTestWithStr[Boolean](Schema.BOOLEAN)
    }

    "support char" in {
      roundTripTest[Char](Schema.INTEGER)
    }

    "support float" in {
      roundTripTestWithStr[Float](Schema.FLOAT)
      roundTripTestWithStr[Double](Schema.FLOAT)
    }

    "support string" in {
      roundTripTest[String](Schema.STRING)
    }

    "support arrays" taggedAs ("array") in {
      arrayRoundTripTest[Byte]
      arrayRoundTripTest[Char]
      arrayRoundTripTest[Int]
      arrayRoundTripTest[Short]
      arrayRoundTripTest[Long]
      arrayRoundTripTest[String]
      arrayRoundTripTest[Float]
      arrayRoundTripTest[Double]
      arrayRoundTripTest[Boolean]
    }

    "read various types of data as int" in {
      val expected = Seq(10, 12, 13, 0, 1, 13, 12345, 0)

      val p = MessagePack.newDefaultBufferPacker()
      p.packArrayHeader(expected.size)
      p.packInt(10)
      p.packString("12")
      p.packString("13.2")
      p.packBoolean(false)
      p.packBoolean(true)
      p.packFloat(13.4f)
      p.packDouble(12345.01)
      p.packString("non-number") // will be 0

      val codec = MessageCodec.of[Seq[Int]]
      val seq   = codec.unpackBytes(p.toByteArray)
      seq shouldBe defined
      seq.get shouldBe expected
    }

    "read various types of data as short" in {
      val expected = Seq[Short](10, 12, 13, 0, 1, 13, 1021, 0)

      val p = MessagePack.newDefaultBufferPacker()
      p.packArrayHeader(expected.size)
      p.packInt(10)
      p.packString("12")
      p.packString("13.2")
      p.packBoolean(false)
      p.packBoolean(true)
      p.packFloat(13.4f)
      p.packDouble(1021.1)
      p.packString("non-number") // will be 0

      val codec = MessageCodec.of[Seq[Short]]
      val seq   = codec.unpackBytes(p.toByteArray)
      seq shouldBe defined
      seq.get shouldBe expected
    }

    "read various types of data as byte" in {
      val expected = Seq[Byte](10, 12, 13, 0, 1, 13, 123, 0)

      val p = MessagePack.newDefaultBufferPacker()
      p.packArrayHeader(expected.size)
      p.packInt(10)
      p.packString("12")
      p.packString("13.2")
      p.packBoolean(false)
      p.packBoolean(true)
      p.packFloat(13.4f)
      p.packDouble(123.0)
      p.packString("non-number") // will be 0

      val codec = MessageCodec.of[Seq[Byte]]
      val seq   = codec.unpackBytes(p.toByteArray)
      seq shouldBe defined
      seq.get shouldBe expected
    }

    "read various types of data as float" in {
      val expected = Seq[Float](10f, 12f, 13.2f, 0f, 1f, 13.4f, 12345.01f, 0f)

      val p = MessagePack.newDefaultBufferPacker()
      p.packArrayHeader(expected.size)
      p.packInt(10)
      p.packString("12")
      p.packString("13.2")
      p.packBoolean(false)
      p.packBoolean(true)
      p.packFloat(13.4f)
      p.packDouble(12345.01)
      p.packString("non-number") // will be 0

      val codec = MessageCodec.of[Seq[Float]]
      val seq   = codec.unpackBytes(p.toByteArray)
      seq shouldBe defined
      seq.get shouldBe expected
    }

    "read various types of data as double" in {
      val expected = Seq[Double](10.0, 12.0, 13.2, 0.0, 1.0, 0.1f, 12345.01, 0.0)

      val p = MessagePack.newDefaultBufferPacker()
      p.packArrayHeader(expected.size)
      p.packInt(10)
      p.packString("12")
      p.packString("13.2")
      p.packBoolean(false)
      p.packBoolean(true)
      p.packFloat(0.1f)
      p.packDouble(12345.01)
      p.packString("non-number") // will be 0

      val codec = MessageCodec.of[Seq[Double]]
      val seq   = codec.unpackBytes(p.toByteArray)
      seq shouldBe defined
      seq.get shouldBe expected
    }

    "read various types of data as boolean" in {
      val expected = Seq(true, true, true, false, true, false, false, true, false, true, true, false)

      val p = MessagePack.newDefaultBufferPacker()
      p.packArrayHeader(expected.size)
      p.packInt(10)
      p.packString("12")
      p.packString("13.2")
      p.packString("0")
      p.packString("true")
      p.packString("false")
      p.packBoolean(false)
      p.packBoolean(true)
      p.packFloat(0.0f)
      p.packFloat(0.1f)
      p.packDouble(12345.01)
      p.packString("non-number") // will be false (default value)

      val codec = MessageCodec.of[Seq[Boolean]]
      val seq   = codec.unpackBytes(p.toByteArray)
      seq shouldBe defined
      seq.get shouldBe expected
    }

    "read various types of data as string" in {
      val expected = Seq("10", "12", "13.2", "false", "true", "10.0", "12345.01")

      val p = MessagePack.newDefaultBufferPacker()
      p.packArrayHeader(expected.size)
      p.packInt(10)
      p.packString("12")
      p.packString("13.2")
      p.packBoolean(false)
      p.packBoolean(true)
      p.packFloat(10.0f)
      p.packDouble(12345.01)

      val codec = MessageCodec.of[Seq[String]]
      val seq   = codec.unpackBytes(p.toByteArray)
      seq shouldBe defined
      seq.get shouldBe expected
    }

  }

}
