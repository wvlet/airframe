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

  "Packer/Unpacker" should {
    "satisfy roundtrip" in {
      When("Int")
      forAll { (v: Int) =>
        roundtrip(v) { Packer.packInt(_, _) } { Unpacker.unpackInt(_) }
      }
      When("Boolean")
      forAll { (v: Boolean) =>
        roundtrip(v) { Packer.packBoolean(_, _) } { Unpacker.unpackBoolean(_) }
      }
      When("Byte")
      forAll { (v: Byte) =>
        roundtrip(v) { Packer.packByte(_, _) } { Unpacker.unpackByte(_) }
      }
      When("Short")
      forAll { (v: Short) =>
        roundtrip(v) { Packer.packShort(_, _) } { Unpacker.unpackShort(_) }
      }
      When("Long")
      forAll { (v: Long) =>
        roundtrip(v) { Packer.packLong(_, _) } { Unpacker.unpackLong(_) }
      }
      When("Float")
      forAll { (v: Float) =>
        roundtrip(v) { Packer.packFloat(_, _) } { Unpacker.unpackFloat(_) }
      }
      When("Double")
      forAll { (v: Double) =>
        roundtrip(v) { Packer.packDouble(_, _) } { Unpacker.unpackDouble(_) }
      }
      When("String")
      forAll { (v: String) =>
        roundtrip(v) { Packer.packString(_, _) } { Unpacker.unpackString(_) }
      }
      When("RawString")
      forAll { (v: String) =>
        val b = v.getBytes(StandardCharsets.UTF_8)
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
      When(s"BigInteger")
      forAll { (l: Long) =>
        val v = BigInteger.valueOf(l)
        roundtrip(v) { Packer.packBigInteger(_, _) } { Unpacker.unpackBigInteger(_) }
      }
      When(s"Timestamp")
      val posLong = Gen.chooseNum[Long](-31557014167219200L, 31556889864403199L)
      val posInt  = Gen.chooseNum(0, 1000000000) // NANOS_PER_SECOND
      forAll(posLong, posInt) { (second: Long, nano: Int) =>
        val v = Instant.ofEpochSecond(second, nano)
        roundtrip(v) { Packer.packTimestamp(_, _) } { Unpacker.unpackTimestamp(_) }
      }
    }
  }

}
