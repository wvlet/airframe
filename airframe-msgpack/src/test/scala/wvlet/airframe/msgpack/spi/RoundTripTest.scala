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
import wvlet.airframe.msgpack.io.ByteArrayBuffer
import wvlet.airspec.spi.PropertyCheck
import wvlet.airspec.AirSpec

/**
  *
  */
class RoundTripTest extends AirSpec with PropertyCheck {
  scalaJsSupport

  val buf = ByteArrayBuffer.newBuffer(1024)

  protected def rawRoundtrip[A, B](v: A)(pack: (WriteCursor, A) => Unit)(unpack: ReadCursor => B): B = {
    val writeCursor = WriteCursor(buf, 0)
    pack(writeCursor, v)
    val readCursor = ReadCursor(buf, 0)
    unpack(readCursor)
  }

  protected def roundtrip[A](v: A)(pack: (WriteCursor, A) => Unit)(unpack: ReadCursor => A): Unit = {
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
      { OffsetPacker.packByte(_, _) },
      { OffsetPacker.packShort(_, _) },
      { OffsetPacker.packInt(_, _) },
      { OffsetPacker.packLong(_, _) },
      { OffsetPacker.packINT8(_, _) },
      { OffsetPacker.packINT16(_, _) },
      { OffsetPacker.packINT32(_, _) },
      { OffsetPacker.packINT64(_, _) }
    )

    val posNumPackers = Seq[(WriteCursor, Byte) => Unit](
      { OffsetPacker.packUINT8(_, _) },
      { OffsetPacker.packUINT16(_, _) },
      { OffsetPacker.packUINT32(_, _) },
      { OffsetPacker.packUINT64(_, _) }
    )

    val unpackers = Seq[ReadCursor => Byte](
      { OffsetUnpacker.unpackByte(_) },
      { OffsetUnpacker.unpackShort(_).toByte },
      { OffsetUnpacker.unpackInt(_).toByte },
      { OffsetUnpacker.unpackLong(_).toByte },
      { OffsetUnpacker.unpackBigInteger(_).longValue().toByte }
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
      { OffsetPacker.packShort(_, _) },
      { OffsetPacker.packInt(_, _) },
      { OffsetPacker.packLong(_, _) },
      { OffsetPacker.packINT16(_, _) },
      { OffsetPacker.packINT32(_, _) },
      { OffsetPacker.packINT64(_, _) }
    )

    val posNumPackers = Seq[(WriteCursor, Short) => Unit](
      { OffsetPacker.packUINT16(_, _) },
      { OffsetPacker.packUINT32(_, _) },
      { OffsetPacker.packUINT64(_, _) }
    )

    val unpackers = Seq[ReadCursor => Short](
      { OffsetUnpacker.unpackShort(_) },
      { OffsetUnpacker.unpackInt(_).toShort },
      { OffsetUnpacker.unpackLong(_).toShort },
      { OffsetUnpacker.unpackBigInteger(_).longValue().toShort }
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

  def `pack/unpack values`: Unit = {
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
      roundtrip(v) { OffsetPacker.packValue(_, _) } { OffsetUnpacker.unpackValue(_) }
    }
  }

  def `report error on too big integer`: Unit = {
    intercept[IllegalArgumentException] {
      OffsetPacker.packBigInteger(WriteCursor(buf, 0), BigInteger.valueOf(1).shiftLeft(64))
    }
  }

  def `report overflow errors`: Unit = {
    val b1 = Seq(BigInteger.valueOf(Byte.MinValue.toLong - 1), BigInteger.valueOf(Byte.MaxValue.toLong + 1))
    val b2 = Seq(BigInteger.valueOf(Short.MinValue.toLong - 1), BigInteger.valueOf(Short.MaxValue.toLong + 1))
    val b3 = Seq(BigInteger.valueOf(Int.MinValue.toLong - 1), BigInteger.valueOf(Int.MaxValue.toLong + 1))
    val b4 = Seq(BigInteger.valueOf(Long.MaxValue).add(BigInteger.valueOf(1)))
    // Byte
    (b1 ++ b2 ++ b3 ++ b4).foreach { b =>
      intercept[IntegerOverflowException] {
        rawRoundtrip(b) { OffsetPacker.packBigInteger(_, _) } { OffsetUnpacker.unpackByte(_) }
      }
    }

    // Short
    (b2 ++ b3 ++ b4).foreach { b =>
      intercept[IntegerOverflowException] {
        rawRoundtrip(b) { OffsetPacker.packBigInteger(_, _) } { OffsetUnpacker.unpackShort(_) }
      }
    }

    // Int
    (b3 ++ b4).foreach { b =>
      intercept[IntegerOverflowException] {
        rawRoundtrip(b) { OffsetPacker.packBigInteger(_, _) } { OffsetUnpacker.unpackInt(_) }
      }
    }

    // Long
    b4.foreach { b =>
      intercept[IntegerOverflowException] {
        rawRoundtrip(b) { OffsetPacker.packBigInteger(_, _) } { x => BigInteger.valueOf(OffsetUnpacker.unpackLong(x)) }
      }
    }
  }

  def `support Nil`: Unit = {
    rawRoundtrip(null) { (cursor, v) => OffsetPacker.packNil(cursor) } { cursor =>
      OffsetUnpacker.unpackNil(cursor); null
    }
    rawRoundtrip(null) { (cursor, v) => OffsetPacker.packNil(cursor) } { cursor =>
      OffsetUnpacker.tryUnpackNil(cursor); null
    }
  }

  def `support Boolean`: Unit = {
    roundtrip(true) { OffsetPacker.packBoolean(_, _) } { OffsetUnpacker.unpackBoolean(_) }
    roundtrip(false) { OffsetPacker.packBoolean(_, _) } { OffsetUnpacker.unpackBoolean(_) }
  }

  def `support Fixnum`: Unit = {
    forAll(Gen.chooseNum[Byte](-32, 127)) { v: Byte => testByte(v) }
  }

  def `support Byte`: Unit = {
    forAll { (v: Byte) => testByte(v) }
  }

  def `support Short`: Unit = {
    forAll { v: Short => testShort(v) }
    forAll(Gen.chooseNum[Short]((Byte.MaxValue.toShort + 1).toShort, (1 << 8).toShort)) { v: Short => testShort(v) }
    forAll(Gen.chooseNum[Short]((1 << 8).toShort, Short.MaxValue)) { v: Short => testShort(v) }
  }

  def `support Int`: Unit = {
    forAll { (v: Int) =>
      val packers = Seq[(WriteCursor, Int) => Unit](
        { OffsetPacker.packInt(_, _) },
        { OffsetPacker.packLong(_, _) },
        { OffsetPacker.packINT32(_, _) },
        { OffsetPacker.packINT64(_, _) }
      )

      val posNumPackers = Seq[(WriteCursor, Int) => Unit](
        { OffsetPacker.packUINT32(_, _) },
        { OffsetPacker.packUINT64(_, _) }
      )

      val unpackers = Seq[ReadCursor => Int](
        { OffsetUnpacker.unpackInt(_) },
        { OffsetUnpacker.unpackLong(_).toInt },
        { OffsetUnpacker.unpackBigInteger(_).longValue().toInt }
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

  def `support Long`: Unit = {
    // UINT32
    roundtrip((1L << 31) + 1) { (c, v) => OffsetPacker.packUINT32(c, v.toInt) } { OffsetUnpacker.unpackLong(_) }

    forAll { (v: Long) =>
      val packers = Seq[(WriteCursor, Long) => Unit](
        { OffsetPacker.packLong(_, _) },
        { OffsetPacker.packINT64(_, _) }
      )

      val posNumPackers = Seq[(WriteCursor, Long) => Unit](
        { OffsetPacker.packUINT64(_, _) }
      )

      val unpackers = Seq[ReadCursor => Long](
        { OffsetUnpacker.unpackLong(_) },
        { OffsetUnpacker.unpackBigInteger(_).longValue() }
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

  def `support INT8`: Unit = {
    // INT8
    roundtrip[Short](-1) { (cursor, v) => OffsetPacker.packShort(cursor, v) } { OffsetUnpacker.unpackShort(_) }

    roundtrip(-1) { (cursor, v) => OffsetPacker.packInt(cursor, v) } { OffsetUnpacker.unpackInt(_) }

    roundtrip(-1.toLong) { (cursor, v) => OffsetPacker.packLong(cursor, v) } { OffsetUnpacker.unpackLong(_) }
  }

  def `support BigInteger`: Unit = {
    // UINT32
    roundtrip((1L << 31) + 1) { (c, v) => OffsetPacker.packUINT32(c, v.toInt) } {
      OffsetUnpacker.unpackBigInteger(_).longValue()
    }
    forAll { (l: Long) =>
      val v = BigInteger.valueOf(l)
      roundtrip(v) { OffsetPacker.packBigInteger(_, _) } { OffsetUnpacker.unpackBigInteger(_) }
    }
  }

  def `support Float`: Unit = {
    forAll { (v: Float) =>
      val packers = Seq[(WriteCursor, Float) => Unit](
        { OffsetPacker.packFloat(_, _) },
        { OffsetPacker.packFLOAT32(_, _) },
        { OffsetPacker.packFLOAT64(_, _) }
      )

      val unpackers = Seq[ReadCursor => Float](
        { OffsetUnpacker.unpackFloat(_) },
        { OffsetUnpacker.unpackDouble(_).toFloat }
      )

      for (p <- packers; u <- unpackers) {
        roundtrip(v)(p)(u)
      }
    }
  }

  def `support Double`: Unit = {
    forAll { (v: Double) =>
      val packers = Seq[(WriteCursor, Double) => Unit](
        { OffsetPacker.packDouble(_, _) },
        { OffsetPacker.packFLOAT64(_, _) }
      )

      val unpackers = Seq[ReadCursor => Double](
        { OffsetUnpacker.unpackDouble(_) }
      )

      for (p <- packers; u <- unpackers) {
        roundtrip(v)(p)(u)
      }
      roundtrip(v) { OffsetPacker.packDouble(_, _) } { OffsetUnpacker.unpackDouble(_) }
    }
  }

  def `support String`: Unit = {
    forAll(arbitrary[String]) { v: String => // Generate unicode strings
      roundtrip(v) { OffsetPacker.packString(_, _) } { OffsetUnpacker.unpackString(_) }
    }
  }

  def `support RawString`: Unit = {
    forAll { (s: String) =>
      val b = s.getBytes(StandardCharsets.UTF_8)
      val v = b.slice(0, b.length.min(1024))
      roundtrip(b) { (cursor, v) =>
        OffsetPacker.packRawStringHeader(cursor, v.length)
        OffsetPacker.writePayload(cursor, v)
      } { cursor =>
        val len = OffsetUnpacker.unpackRawStringHeader(cursor)
        OffsetUnpacker.readPayload(cursor, len)
      }
    }
  }

  def `support Binary`: Unit = {
    forAll { (v: Array[Byte]) =>
      roundtrip(v) { (cursor, v) =>
        OffsetPacker.packBinaryHeader(cursor, v.length)
        OffsetPacker.writePayload(cursor, v)
      } { cursor =>
        val len = OffsetUnpacker.unpackBinaryHeader(cursor)
        OffsetUnpacker.readPayload(cursor, len)
      }
    }
  }

  def `support Timestamp`: Unit = {
    val posLong = Gen.chooseNum[Long](-31557014167219200L, 31556889864403199L)
    val posInt  = Gen.chooseNum(0, 1000000000 - 1) // NANOS_PER_SECOND
    forAll(posLong, posInt) { (second: Long, nano: Int) =>
      val v = Instant.ofEpochSecond(second, nano)
      roundtrip(v) { OffsetPacker.packTimestamp(_, _) } { OffsetUnpacker.unpackTimestamp(_) }
    }
    val secLessThan34bits = Gen.chooseNum[Long](0, 1L << 34)
    forAll(secLessThan34bits, posInt) { (second: Long, nano: Int) =>
      val v = Instant.ofEpochSecond(second, nano)
      roundtrip(v) { OffsetPacker.packTimestamp(_, _) } { OffsetUnpacker.unpackTimestamp(_) }
    }

    // Corner cases for u
    // sing uint32 nanoseq (out of int32 range)
    for (v <- Seq(
           Instant.ofEpochSecond(Instant.now().getEpochSecond, 123456789L),
           Instant.ofEpochSecond(-1302749144L, 0), // 1928-09-19T21:14:16Z
           Instant.ofEpochSecond(-747359729L, 0),  // 1946-04-27T00:04:31Z
           Instant.ofEpochSecond(4257387427L, 0)   // 2104-11-29T07:37:07Z
         )) {
      roundtrip(v) { OffsetPacker.packTimestamp(_, _) } { OffsetUnpacker.unpackTimestamp(_) }
    }
  }

  val headerSizes = Seq(1, 2, 4, 8, 16, 32, 128, 256, 1024, 1 << 16, 1 << 20)
  val sizeGen     = Gen.chooseNum[Int](0, Int.MaxValue)

  def `support ArrayHeader`: Unit = {
    for (size <- headerSizes) {
      roundtrip(size) { OffsetPacker.packArrayHeader(_, _) } { OffsetUnpacker.unpackArrayHeader(_) }
    }

    forAll(sizeGen) { (len: Int) =>
      roundtrip(len) { OffsetPacker.packArrayHeader(_, _) } { OffsetUnpacker.unpackArrayHeader(_) }
    }
  }

  def `support MapHeader`: Unit = {
    for (size <- headerSizes) {
      roundtrip(size) { OffsetPacker.packMapHeader(_, _) } { OffsetUnpacker.unpackMapHeader(_) }
    }
    forAll(sizeGen) { (len: Int) =>
      roundtrip(len) { OffsetPacker.packMapHeader(_, _) } { OffsetUnpacker.unpackMapHeader(_) }
    }
  }

  def `supprot RawStringHeader`: Unit = {
    for (size <- headerSizes) {
      roundtrip(size) { OffsetPacker.packRawStringHeader(_, _) } { OffsetUnpacker.unpackRawStringHeader(_) }
    }
    forAll(sizeGen) { (len: Int) =>
      roundtrip(len) { OffsetPacker.packRawStringHeader(_, _) } { OffsetUnpacker.unpackRawStringHeader(_) }
    }
  }

  def `support BinaryHeader`: Unit = {
    for (size <- headerSizes) {
      roundtrip(size) { OffsetPacker.packBinaryHeader(_, _) } { OffsetUnpacker.unpackBinaryHeader(_) }
    }
    forAll(sizeGen) { (len: Int) =>
      roundtrip(len) { OffsetPacker.packBinaryHeader(_, _) } { OffsetUnpacker.unpackBinaryHeader(_) }
    }
  }

  def `support ExtHeader`: Unit = {
    // For FIXEXT1, 2, 4, 8, 16, etc.
    for (i <- headerSizes) {
      roundtrip(ExtTypeHeader(1, i)) { OffsetPacker.packExtTypeHeader(_, _) } {
        OffsetUnpacker.unpackExtTypeHeader(_)
      }
    }
    forAll(Gen.posNum[Byte], sizeGen) { (v: Byte, len: Int) =>
      roundtrip(ExtTypeHeader(v, len)) { OffsetPacker.packExtTypeHeader(_, _) } {
        OffsetUnpacker.unpackExtTypeHeader(_)
      }
    }
  }
}
