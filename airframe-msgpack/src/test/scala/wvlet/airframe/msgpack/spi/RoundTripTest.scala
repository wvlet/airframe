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
package wvlet.airframe.msgpack.spi

import java.math.BigInteger
import java.nio.charset.StandardCharsets
import java.time.Instant

import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.Gen
import org.scalatest.prop.PropertyChecks
import wvlet.airframe.AirframeSpec
import wvlet.airframe.msgpack.io.ByteArrayBuffer

/**
  *
  */
class RoundTripTest extends AirframeSpec with PropertyChecks {

  val buf = ByteArrayBuffer.newBuffer(1024)

  def rawRoundtrip[A, B](v: A)(pack: (WriteCursor, A) => Unit)(unpack: ReadCursor => B): B = {
    val writeCursor = WriteCursor(buf, 0)
    pack(writeCursor, v)
    val readCursor = ReadCursor(buf, 0)
    unpack(readCursor)
  }

  def roundtrip[A](v: A)(pack: (WriteCursor, A) => Unit)(unpack: ReadCursor => A): Unit = {
    try {
      val v2 = rawRoundtrip(v)(pack)(unpack)
      v2 shouldBe v
    } catch {
      case e: Exception =>
        warn(s"Failed roundtrip test for ${v}")
        throw e
    }
  }

  private def testByte(v: Byte): Unit = {
    val packers = Seq[(WriteCursor, Byte) => Unit](
      { Packer.packByte(_, _) },
      { Packer.packShort(_, _) },
      { Packer.packInt(_, _) },
      { Packer.packLong(_, _) },
      { Packer.packINT8(_, _) },
      { Packer.packINT16(_, _) },
      { Packer.packINT32(_, _) },
      { Packer.packINT64(_, _) }
    )

    val posNumPackers = Seq[(WriteCursor, Byte) => Unit](
      { Packer.packUINT8(_, _) },
      { Packer.packUINT16(_, _) },
      { Packer.packUINT32(_, _) },
      { Packer.packUINT64(_, _) }
    )

    val unpackers = Seq[ReadCursor => Byte](
      { Unpacker.unpackByte(_) },
      { Unpacker.unpackShort(_).toByte },
      { Unpacker.unpackInt(_).toByte },
      { Unpacker.unpackLong(_).toByte },
      { Unpacker.unpackBigInteger(_).longValue().toByte }
    )

    for (p <- packers; u <- unpackers) {
      roundtrip(v)(p)(u)
    }
    if (v > 0) {
      for (p <- posNumPackers; u <- unpackers) {
        roundtrip(v)(p)(u)
      }
    }
  }

  private def testShort(v: Short): Unit = {
    val packers = Seq[(WriteCursor, Short) => Unit](
      { Packer.packShort(_, _) },
      { Packer.packInt(_, _) },
      { Packer.packLong(_, _) },
      { Packer.packINT16(_, _) },
      { Packer.packINT32(_, _) },
      { Packer.packINT64(_, _) }
    )

    val posNumPackers = Seq[(WriteCursor, Short) => Unit](
      { Packer.packUINT16(_, _) },
      { Packer.packUINT32(_, _) },
      { Packer.packUINT64(_, _) }
    )

    val unpackers = Seq[ReadCursor => Short](
      { Unpacker.unpackShort(_) },
      { Unpacker.unpackInt(_).toShort },
      { Unpacker.unpackLong(_).toShort },
      { Unpacker.unpackBigInteger(_).longValue().toShort }
    )

    for (p <- packers; u <- unpackers) {
      roundtrip(v)(p)(u)
    }
    if (v > 0) {
      for (p <- posNumPackers; u <- unpackers) {
        roundtrip(v)(p)(u)
      }
    }
  }

  "Packer/Unpacker" should {
    "pack/unpack values" in {
      import ValueFactory._
      val list: Seq[Value] = Seq(
        newNil,
        newBoolean(true),
        newInteger(10),
        newInteger(BigInteger.valueOf(Long.MaxValue)),
        newFloat(0.1),
        newString("hello"),
        newBinary(Array[Byte](0, 1, 2)),
        newExt(1, Array[Byte](2, 3, 4)),
        newTimestamp(Instant.now()),
        newArray(newInteger(20), newBoolean(false)),
        newMap(newString("a") -> newString("apple"), newString("b") -> newString("banana"))
      )
      for (v <- list) {
        roundtrip(v) { Packer.packValue(_, _) } { Unpacker.unpackValue(_) }
      }
    }

    "report error on too big integer" in {
      intercept[IllegalArgumentException] {
        Packer.packBigInteger(WriteCursor(buf, 0), BigInteger.valueOf(1).shiftLeft(64))
      }
    }

    "report overflow errors" in {

      val b1 = Seq(BigInteger.valueOf(Byte.MinValue.toLong - 1), BigInteger.valueOf(Byte.MaxValue.toLong + 1))
      val b2 = Seq(BigInteger.valueOf(Short.MinValue.toLong - 1), BigInteger.valueOf(Short.MaxValue.toLong + 1))
      val b3 = Seq(BigInteger.valueOf(Int.MinValue.toLong - 1), BigInteger.valueOf(Int.MaxValue.toLong + 1))
      val b4 = Seq(BigInteger.valueOf(Long.MaxValue).add(BigInteger.valueOf(1)))
      // Byte
      (b1 ++ b2 ++ b3 ++ b4).foreach { b =>
        intercept[IntegerOverflowException] {
          rawRoundtrip(b) { Packer.packBigInteger(_, _) } { Unpacker.unpackByte(_) }
        }
      }

      // Short
      (b2 ++ b3 ++ b4).foreach { b =>
        intercept[IntegerOverflowException] {
          rawRoundtrip(b) { Packer.packBigInteger(_, _) } { Unpacker.unpackShort(_) }
        }
      }

      // Int
      (b3 ++ b4).foreach { b =>
        intercept[IntegerOverflowException] {
          rawRoundtrip(b) { Packer.packBigInteger(_, _) } { Unpacker.unpackInt(_) }
        }
      }

      // Long
      b4.foreach { b =>
        intercept[IntegerOverflowException] {
          rawRoundtrip(b) { Packer.packBigInteger(_, _) } { x =>
            BigInteger.valueOf(Unpacker.unpackLong(x))
          }
        }
      }
    }

    "support Nil" in {
      rawRoundtrip(null) { (cursor, v) =>
        Packer.packNil(cursor)
      } { cursor =>
        Unpacker.unpackNil(_); null
      }
      rawRoundtrip(null) { (cursor, v) =>
        Packer.packNil(cursor)
      } { cursor =>
        Unpacker.tryUnpackNil(_); null
      }
    }

    "support Boolean" in {
      roundtrip(true) { Packer.packBoolean(_, _) } { Unpacker.unpackBoolean(_) }
      roundtrip(false) { Packer.packBoolean(_, _) } { Unpacker.unpackBoolean(_) }
    }

    "support Fixnum" in {
      forAll(Gen.chooseNum[Byte](-32, 127)) { v: Byte =>
        testByte(v)
      }
    }

    "support Byte" in {
      forAll { (v: Byte) =>
        testByte(v)
      }
    }

    "support Short" in {
      forAll { v: Short =>
        testShort(v)
      }
      forAll(Gen.chooseNum[Short]((Byte.MaxValue.toShort + 1).toShort, (1 << 8).toShort)) { v: Short =>
        testShort(v)
      }
      forAll(Gen.chooseNum[Short]((1 << 8).toShort, Short.MaxValue)) { v: Short =>
        testShort(v)
      }
    }

    "support Int" in {
      forAll { (v: Int) =>
        val packers = Seq[(WriteCursor, Int) => Unit](
          { Packer.packInt(_, _) },
          { Packer.packLong(_, _) },
          { Packer.packINT32(_, _) },
          { Packer.packINT64(_, _) }
        )

        val posNumPackers = Seq[(WriteCursor, Int) => Unit](
          { Packer.packUINT32(_, _) },
          { Packer.packUINT64(_, _) }
        )

        val unpackers = Seq[ReadCursor => Int](
          { Unpacker.unpackInt(_) },
          { Unpacker.unpackLong(_).toInt },
          { Unpacker.unpackBigInteger(_).longValue().toInt }
        )

        for (p <- packers; u <- unpackers) {
          roundtrip(v)(p)(u)
        }
        if (v > 0) {
          for (p <- posNumPackers; u <- unpackers) {
            roundtrip(v)(p)(u)
          }
        }
      }
    }

    "support Long" in {
      // UINT32
      roundtrip((1L << 31) + 1) { (c, v) =>
        Packer.packUINT32(c, v.toInt)
      } { Unpacker.unpackLong(_) }

      forAll { (v: Long) =>
        val packers = Seq[(WriteCursor, Long) => Unit](
          { Packer.packLong(_, _) },
          { Packer.packINT64(_, _) }
        )

        val posNumPackers = Seq[(WriteCursor, Long) => Unit](
          { Packer.packUINT64(_, _) }
        )

        val unpackers = Seq[ReadCursor => Long](
          { Unpacker.unpackLong(_) },
          { Unpacker.unpackBigInteger(_).longValue() }
        )

        for (p <- packers; u <- unpackers) {
          roundtrip(v)(p)(u)
        }
        if (v > 0) {
          for (p <- posNumPackers; u <- unpackers) {
            roundtrip(v)(p)(u)
          }
        }
      }
    }

    "support INT8" in {
      // INT8
      roundtrip[Short](-1) { (cursor, v) =>
        Packer.packShort(cursor, v)
      } { Unpacker.unpackShort(_) }

      roundtrip(-1) { (cursor, v) =>
        Packer.packInt(cursor, v)
      } { Unpacker.unpackInt(_) }

      roundtrip(-1.toLong) { (cursor, v) =>
        Packer.packLong(cursor, v)
      } { Unpacker.unpackLong(_) }
    }

    "support BigInteger" in {
      // UINT32
      roundtrip((1L << 31) + 1) { (c, v) =>
        Packer.packUINT32(c, v.toInt)
      } { Unpacker.unpackBigInteger(_).longValue() }
      forAll { (l: Long) =>
        val v = BigInteger.valueOf(l)
        roundtrip(v) { Packer.packBigInteger(_, _) } { Unpacker.unpackBigInteger(_) }
      }
    }

    "support Float" in {
      forAll { (v: Float) =>
        val packers = Seq[(WriteCursor, Float) => Unit](
          { Packer.packFloat(_, _) },
          { Packer.packFLOAT32(_, _) },
          { Packer.packFLOAT64(_, _) }
        )

        val unpackers = Seq[ReadCursor => Float](
          { Unpacker.unpackFloat(_) },
          { Unpacker.unpackDouble(_).toFloat }
        )

        for (p <- packers; u <- unpackers) {
          roundtrip(v)(p)(u)
        }
      }
    }

    "support Double" in {
      forAll { (v: Double) =>
        val packers = Seq[(WriteCursor, Double) => Unit](
          { Packer.packDouble(_, _) },
          { Packer.packFLOAT64(_, _) }
        )

        val unpackers = Seq[ReadCursor => Double](
          { Unpacker.unpackDouble(_) }
        )

        for (p <- packers; u <- unpackers) {
          roundtrip(v)(p)(u)
        }
        roundtrip(v) { Packer.packDouble(_, _) } { Unpacker.unpackDouble(_) }
      }
    }

    "support String" in {
      forAll(arbitrary[String]) { v: String => // Generate unicode strings
        roundtrip(v) { Packer.packString(_, _) } { Unpacker.unpackString(_) }
      }
    }

    "support RawString" in {
      forAll { (s: String) =>
        val b = s.getBytes(StandardCharsets.UTF_8)
        val v = b.slice(0, b.length.min(1024))
        roundtrip(b) { (cursor, v) =>
          Packer.packRawStringHeader(cursor, v.length)
          Packer.writePayload(cursor, v)
        } { cursor =>
          val len = Unpacker.unpackRawStringHeader(cursor)
          Unpacker.readPayload(cursor, len)
        }
      }
    }

    "support Binary" in {
      forAll { (v: Array[Byte]) =>
        roundtrip(v) { (cursor, v) =>
          Packer.packBinaryHeader(cursor, v.length)
          Packer.writePayload(cursor, v)
        } { cursor =>
          val len = Unpacker.unpackBinaryHeader(cursor)
          Unpacker.readPayload(cursor, len)
        }
      }
    }

    "support Timestamp" in {
      val posLong = Gen.chooseNum[Long](-31557014167219200L, 31556889864403199L)
      val posInt  = Gen.chooseNum(0, 1000000000 - 1) // NANOS_PER_SECOND
      forAll(posLong, posInt) { (second: Long, nano: Int) =>
        val v = Instant.ofEpochSecond(second, nano)
        roundtrip(v) { Packer.packTimestamp(_, _) } { Unpacker.unpackTimestamp(_) }
      }
      val secLessThan34bits = Gen.chooseNum[Long](0, 1L << 34)
      forAll(secLessThan34bits, posInt) { (second: Long, nano: Int) =>
        val v = Instant.ofEpochSecond(second, nano)
        roundtrip(v) { Packer.packTimestamp(_, _) } { Unpacker.unpackTimestamp(_) }
      }

      // Corner cases for u
      // sing uint32 nanoseq (out of int32 range)
      for (v <- Seq(
             Instant.ofEpochSecond(Instant.now().getEpochSecond, 123456789L),
             Instant.ofEpochSecond(-1302749144L, 0), // 1928-09-19T21:14:16Z
             Instant.ofEpochSecond(-747359729L, 0), // 1946-04-27T00:04:31Z
             Instant.ofEpochSecond(4257387427L, 0) // 2104-11-29T07:37:07Z
           )) {
        roundtrip(v) { Packer.packTimestamp(_, _) } { Unpacker.unpackTimestamp(_) }
      }
    }

    val headerSizes = Seq(1, 2, 4, 8, 16, 32, 128, 256, 1024, 1 << 16, 1 << 20)
    val sizeGen     = Gen.chooseNum[Int](0, Int.MaxValue)

    "support ArrayHeader" in {
      for (size <- headerSizes) {
        roundtrip(size) { Packer.packArrayHeader(_, _) } { Unpacker.unpackArrayHeader(_) }
      }

      forAll(sizeGen) { (len: Int) =>
        roundtrip(len) { Packer.packArrayHeader(_, _) } { Unpacker.unpackArrayHeader(_) }
      }
    }

    "support MapHeader" in {
      for (size <- headerSizes) {
        roundtrip(size) { Packer.packMapHeader(_, _) } { Unpacker.unpackMapHeader(_) }
      }
      forAll(sizeGen) { (len: Int) =>
        roundtrip(len) { Packer.packMapHeader(_, _) } { Unpacker.unpackMapHeader(_) }
      }
    }

    "supprot RawStringHeader" in {
      for (size <- headerSizes) {
        roundtrip(size) { Packer.packRawStringHeader(_, _) } { Unpacker.unpackRawStringHeader(_) }
      }
      forAll(sizeGen) { (len: Int) =>
        roundtrip(len) { Packer.packRawStringHeader(_, _) } { Unpacker.unpackRawStringHeader(_) }
      }
    }

    "support BinaryHeader" in {
      for (size <- headerSizes) {
        roundtrip(size) { Packer.packBinaryHeader(_, _) } { Unpacker.unpackBinaryHeader(_) }
      }
      forAll(sizeGen) { (len: Int) =>
        roundtrip(len) { Packer.packBinaryHeader(_, _) } { Unpacker.unpackBinaryHeader(_) }
      }
    }

    "support ExtHeader" in {
      // For FIXEXT1, 2, 4, 8, 16, etc.
      for (i <- headerSizes) {
        roundtrip(ExtTypeHeader(1, i)) { Packer.packExtTypeHeader(_, _) } { Unpacker.unpackExtTypeHeader(_) }
      }
      forAll(Gen.posNum[Byte], sizeGen) { (v: Byte, len: Int) =>
        roundtrip(ExtTypeHeader(v, len)) { Packer.packExtTypeHeader(_, _) } { Unpacker.unpackExtTypeHeader(_) }
      }
    }
  }

}
