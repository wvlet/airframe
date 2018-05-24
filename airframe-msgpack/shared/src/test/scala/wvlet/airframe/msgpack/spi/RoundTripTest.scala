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

import org.scalacheck.Gen
import org.scalacheck.Arbitrary.arbitrary
import org.scalactic.anyvals.{PosInt, PosZInt}
import org.scalatest.prop.PropertyChecks
import wvlet.airframe.AirframeSpec
import wvlet.airframe.msgpack.io.ByteArrayBuffer

/**
  *
  */
class RoundTripTest extends AirframeSpec with PropertyChecks {

  val buf = ByteArrayBuffer.newBuffer(1024)

  def roundtrip[A](v: A)(pack: (WriteCursor, A) => Unit)(unpack: ReadCursor => A): Boolean = {
    try {
      val writeCursor = WriteCursor(buf, 0)
      pack(writeCursor, v)
      val readCursor = ReadCursor(buf, 0)
      val v2: A      = unpack(readCursor)
      v == v2
    } catch {
      case e: Exception =>
        warn(s"Failed roundtrip test for ${v}")
        throw e
    }
  }

  implicit val config = PropertyCheckConfiguration(minSuccessful = PosInt(3), minSize = PosZInt(1), sizeRange = PosZInt(100))

  private def testByte(v: Byte) {
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
    "satisfy roundtrip" in {
      When("Nil")
      roundtrip(null) { (cursor, v) =>
        Packer.packNil(cursor)
      } { cursor =>
        Unpacker.unpackNil(_); null
      }
      When("Boolean")
      forAll { (v: Boolean) =>
        roundtrip(v) { Packer.packBoolean(_, _) } { Unpacker.unpackBoolean(_) }
      }
      When("Fixnum")
      forAll(Gen.chooseNum[Byte](-32, 127)) { v: Byte =>
        testByte(v)
      }

      When("Byte")
      forAll { (v: Byte) =>
        testByte(v)
      }

      When("Short")
      forAll { v: Short =>
        testShort(v)
      }
      forAll(Gen.chooseNum[Short]((Byte.MaxValue.toShort + 1).toShort, (1 << 8).toShort)) { v: Short =>
        testShort(v)
      }

      When("Int")
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
      When("Long")
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
      When(s"BigInteger")
      forAll { (l: Long) =>
        val v = BigInteger.valueOf(l)
        roundtrip(v) { Packer.packBigInteger(_, _) } { Unpacker.unpackBigInteger(_) }
      }
      When("Float")
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
      When("Double")
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
      When("String")
      forAll(arbitrary[String]) { v: String => // Generate unicode strings
        roundtrip(v) { Packer.packString(_, _) } { Unpacker.unpackString(_) }
      }
      When("RawString")
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
      When("Binary")
      forAll { (v: Array[Byte]) =>
        roundtrip(v) { (cursor, v) =>
          Packer.packBinaryHeader(cursor, v.length)
          Packer.writePayload(cursor, v)
        } { cursor =>
          val len = Unpacker.unpackBinaryHeader(cursor)
          Unpacker.readPayload(cursor, len)
        }
      }
      When(s"Timestamp")
      val posLong = Gen.chooseNum[Long](-31557014167219200L, 31556889864403199L)
      val posInt  = Gen.chooseNum(0, 1000000000 - 1) // NANOS_PER_SECOND
      forAll(posLong, posInt) { (second: Long, nano: Int) =>
        val v = Instant.ofEpochSecond(second, nano)
        roundtrip(v) { Packer.packTimestamp(_, _) } { Unpacker.unpackTimestamp(_) }
      }
    }
  }

}
