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

import java.math.BigInteger

import wvlet.airframe.{msgpack, surface}

/**
  *
  */
class PrimitiveCodecTest extends CodecSpec {
  "PrimitiveCodec" should {

    "support numeric" in {
      roundTripTestWithStr[Int](surface.of[Int], DataType.INTEGER)
      roundTripTestWithStr[Byte](surface.of[Byte], DataType.INTEGER)
      roundTripTestWithStr[Short](surface.of[Short], DataType.INTEGER)
      roundTripTestWithStr[Long](surface.of[Long], DataType.INTEGER)
      roundTripTestWithStr[Boolean](surface.of[Boolean], DataType.BOOLEAN)
    }

    "support char" in {
      roundTripTest[Char](surface.of[Char], DataType.INTEGER)
    }

    "support float" in {
      roundTripTestWithStr[Float](surface.of[Float], DataType.FLOAT)
      roundTripTestWithStr[Double](surface.of[Double], DataType.FLOAT)
    }

    "support string" in {
      roundTripTest[String](surface.of[String], DataType.STRING)
    }

    "support arrays" taggedAs ("array") in {
      arrayRoundTripTest[Byte](surface.of[Byte])
      arrayRoundTripTest[Char](surface.of[Char])
      arrayRoundTripTest[Int](surface.of[Int])
      arrayRoundTripTest[Short](surface.of[Short])
      arrayRoundTripTest[Long](surface.of[Long])
      arrayRoundTripTest[String](surface.of[String])
      arrayRoundTripTest[Float](surface.of[Float])
      arrayRoundTripTest[Double](surface.of[Double])
      arrayRoundTripTest[Boolean](surface.of[Boolean])
    }

    // Value 2^64-1 is the maximum value
    val LARGE_VALUE = BigInteger.valueOf(1).shiftLeft(64).subtract(BigInteger.valueOf(1))

    "read various types of data as int" in {
      val expected = Seq(10, 12, 13, 0, 1, 13, 12345, 0, 0, 0)

      val p = msgpack.newBufferPacker
      p.packArrayHeader(expected.size)
      p.packInt(10)
      p.packString("12")
      p.packString("13.2")
      p.packBoolean(false)
      p.packBoolean(true)
      p.packFloat(13.4f)
      p.packDouble(12345.01)
      p.packString("non-number") // will be 0
      p.packNil // will be 0
      p.packBigInteger(LARGE_VALUE) // will be 0

      val codec = Codec.of[Seq[Int]]
      val seq   = codec.unpackMsgPack(p.toByteArray)
      seq shouldBe defined
      seq.get shouldBe expected
    }

    "read various types of data as long" in {
      val expected = Seq[Long](10, 12, 13, 0, 1, 13, 12345, 0, 0, 0)

      val p = msgpack.newBufferPacker
      p.packArrayHeader(expected.size)
      p.packInt(10)
      p.packString("12")
      p.packString("13.2")
      p.packBoolean(false)
      p.packBoolean(true)
      p.packFloat(13.4f)
      p.packDouble(12345.01)
      p.packString("non-number") // will be 0
      p.packNil // will be 0
      p.packBigInteger(LARGE_VALUE) // will be 0

      val codec = Codec.of[Seq[Long]]
      val seq   = codec.unpackMsgPack(p.toByteArray)
      seq shouldBe defined
      seq.get shouldBe expected
    }

    "read various types of data as short" in {
      val expected = Seq[Short](10, 12, 13, 0, 1, 13, 1021, 0, 0, 0)

      val p = msgpack.newBufferPacker
      p.packArrayHeader(expected.size)
      p.packInt(10)
      p.packString("12")
      p.packString("13.2")
      p.packBoolean(false)
      p.packBoolean(true)
      p.packFloat(13.4f)
      p.packDouble(1021.1)
      p.packString("non-number") // will be 0
      p.packNil // will be 0
      p.packBigInteger(LARGE_VALUE) // will be 0

      val codec = Codec.of[Seq[Short]]
      val seq   = codec.unpackMsgPack(p.toByteArray)
      seq shouldBe defined
      seq.get shouldBe expected
    }

    "read various types of data as byte" in {
      val expected = Seq[Byte](10, 12, 13, 0, 1, 13, 123, 0, 0, 0)

      val p = msgpack.newBufferPacker
      p.packArrayHeader(expected.size)
      p.packInt(10)
      p.packString("12")
      p.packString("13.2")
      p.packBoolean(false)
      p.packBoolean(true)
      p.packFloat(13.4f)
      p.packDouble(123.0)
      p.packString("non-number") // will be 0
      p.packNil // will be 0
      p.packBigInteger(LARGE_VALUE) // will be 0

      val codec = Codec.of[Seq[Byte]]
      val seq   = codec.unpackMsgPack(p.toByteArray)
      seq shouldBe defined
      seq.get shouldBe expected
    }

    "read various types of data as char" in {
      val expected = Seq[Char](10, 12, 13, 0, 1, 13, 123, 0, 0, 0)

      val p = msgpack.newBufferPacker
      p.packArrayHeader(expected.size)
      p.packInt(10)
      p.packString("12")
      p.packString("13.2")
      p.packBoolean(false)
      p.packBoolean(true)
      p.packFloat(13.4f)
      p.packDouble(123.0)
      p.packString("non-number") // will be 0
      p.packNil // will be 0
      p.packBigInteger(LARGE_VALUE) // will be 0

      val codec = Codec.of[Seq[Char]]
      val seq   = codec.unpackMsgPack(p.toByteArray)
      seq shouldBe defined
      seq.get shouldBe expected
    }

    "read various types of data as float" in {
      val expected = Seq[Float](10f, 12f, 13.2f, 0f, 1f, 13.4f, 12345.01f, 0f, 0f, LARGE_VALUE.floatValue())

      val p = msgpack.newBufferPacker
      p.packArrayHeader(expected.size)
      p.packInt(10)
      p.packString("12")
      p.packString("13.2")
      p.packBoolean(false)
      p.packBoolean(true)
      p.packFloat(13.4f)
      p.packDouble(12345.01)
      p.packString("non-number") // will be 0
      p.packNil // will be 0
      p.packBigInteger(LARGE_VALUE) // will be 0

      val codec = Codec.of[Seq[Float]]
      val seq   = codec.unpackMsgPack(p.toByteArray)
      seq shouldBe defined
      seq.get shouldBe expected
    }

    "read various types of data as double" in {
      val expected = Seq[Double](10.0, 12.0, 13.2, 0.0, 1.0, 0.1f, 12345.01, 0.0, 0.0, LARGE_VALUE.doubleValue())

      val p = msgpack.newBufferPacker
      p.packArrayHeader(expected.size)
      p.packInt(10)
      p.packString("12")
      p.packString("13.2")
      p.packBoolean(false)
      p.packBoolean(true)
      p.packFloat(0.1f)
      p.packDouble(12345.01)
      p.packString("non-number") // will be 0
      p.packNil // will be 0
      p.packBigInteger(LARGE_VALUE) // will be 0

      val codec = Codec.of[Seq[Double]]
      val seq   = codec.unpackMsgPack(p.toByteArray)
      seq shouldBe defined
      seq.get shouldBe expected
    }

    "read various types of data as boolean" in {
      val expected = Seq(true, true, true, false, true, false, false, true, false, true, true, false, false, true)

      val p = msgpack.newBufferPacker
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
      p.packNil // will be false
      p.packBigInteger(LARGE_VALUE) // will be 0

      val codec = Codec.of[Seq[Boolean]]
      val seq   = codec.unpackMsgPack(p.toByteArray)
      seq shouldBe defined
      seq.get shouldBe expected
    }

    "read various types of data as string" in {
      val expected = Seq("10",
                         "12",
                         "13.2",
                         "false",
                         "true",
                         "10.0",
                         "12345.01",
                         "",
                         LARGE_VALUE.toString,
                         """[1,"leo"]""",
                         """{"name":"leo"}""")

      val p = msgpack.newBufferPacker
      p.packArrayHeader(expected.size)
      p.packInt(10)
      p.packString("12")
      p.packString("13.2")
      p.packBoolean(false)
      p.packBoolean(true)
      p.packFloat(10.0f)
      p.packDouble(12345.01)
      p.packNil // will be 0
      p.packBigInteger(LARGE_VALUE) // will be 0
      p.packArrayHeader(2)
      p.packInt(1)
      p.packString("leo")
      p.packMapHeader(1)
      p.packString("name")
      p.packString("leo")

      val codec = Codec.of[Seq[String]]
      val seq   = codec.unpackMsgPack(p.toByteArray)
      seq shouldBe defined
      seq.get shouldBe expected
    }

  }

}
