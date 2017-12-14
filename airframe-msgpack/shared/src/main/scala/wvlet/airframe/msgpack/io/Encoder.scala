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
package wvlet.airframe.msgpack.io

import java.math.BigInteger
import java.nio.charset.StandardCharsets

import wvlet.airframe.msgpack.spi.Code._

/**
  * Write MessagePack code at a given position on the buffer and return the written byte length
  */
object Encoder {
  def packNil(buf: WriteBuffer, position: Int): Int = {
    buf.writeByte(position, NIL)
  }

  def packBoolean(buf: WriteBuffer, position: Int, v: Boolean): Int = {
    buf.writeByte(position, if (v) TRUE else FALSE)
  }

  def packByte(buf: WriteBuffer, position: Int, v: Byte): Int = {
    if (v < -(1 << 5)) {
      buf.writeByteAndByte(position, INT8, v)
    } else {
      buf.writeByte(position, v)
    }
  }

  def packShort(buf: WriteBuffer, position: Int, v: Short): Int = {
    if (v < -(1 << 5)) {
      if (v < -(1 << 7)) {
        buf.writeByteAndShort(position, INT16, v)
      } else {
        buf.writeByteAndByte(position, INT8, v.toByte)
      }
    } else if (v < (1 << 7)) {
      buf.writeByte(position, v.toByte)
    } else if (v < (1 << 8)) {
      buf.writeByteAndByte(position, UINT8, v.toByte)
    } else {
      buf.writeByteAndShort(position, UINT16, v)
    }
  }

  def packInt(buf: WriteBuffer, position: Int, r: Int): Int = {
    if (r < -(1 << 5)) {
      if (r < -(1 << 15)) {
        buf.writeByteAndInt(position, INT32, r)
      } else if (r < -(1 << 7)) {
        buf.writeByteAndShort(position, INT16, r.toShort)
      } else {
        buf.writeByteAndByte(position, INT8, r.toByte)
      }
    } else if (r < (1 << 7)) {
      buf.writeByte(position, r.toByte)
    } else if (r < (1 << 8)) {
      buf.writeByteAndByte(position, UINT8, r.toByte)
    } else if (r < (1 << 16)) {
      buf.writeByteAndShort(position, UINT16, r.toShort)
    } else { // unsigned 32
      buf.writeByteAndInt(position, UINT32, r)
    }
  }

  def packLong(buf: WriteBuffer, position: Int, v: Long): Int = {
    if (v < -(1L << 5)) {
      if (v < -(1L << 15)) {
        if (v < -(1L << 31))
          buf.writeByteAndLong(position, INT64, v)
        else
          buf.writeByteAndInt(position, INT32, v.toInt)
      } else if (v < -(1 << 7)) {
        buf.writeByteAndShort(position, INT16, v.toShort)
      } else {
        buf.writeByteAndByte(position, INT8, v.toByte)
      }
    } else if (v < (1 << 7)) { // fixnum
      buf.writeByte(position, v.toByte)
    } else if (v < (1L << 16)) {
      if (v < (1 << 8))
        buf.writeByteAndByte(position, UINT8, v.toByte)
      else
        buf.writeByteAndShort(position, UINT16, v.toShort)
    } else if (v < (1L << 32))
      buf.writeByteAndInt(position, UINT32, v.toInt)
    else
      buf.writeByteAndLong(position, UINT64, v)
  }

  def packBigInteger(buf: WriteBuffer, position: Int, bi: BigInteger): Int = {
    if (bi.bitLength <= 63) {
      packLong(buf, position, bi.longValue)
    } else if (bi.bitLength == 64 && bi.signum == 1) {
      buf.writeByteAndLong(position, UINT64, bi.longValue)
    } else {
      throw new IllegalArgumentException("MessagePack cannot serialize BigInteger larger than 2^64-1")
    }
  }

  def packFloat(buf: WriteBuffer, position: Int, v: Float): Int = {
    buf.writeByteAndFloat(position, FLOAT32, v)
  }

  def packDouble(buf: WriteBuffer, position: Int, v: Double): Int = {
    buf.writeByteAndDouble(position, FLOAT64, v)
  }

  def packString(buf: WriteBuffer, position: Int, s: String): Int = {
    val bytes = s.getBytes(StandardCharsets.UTF_8)
    // Write the length and payload of small string to the buffer so that it avoids an extra flush of buffer
    val len = packRawStringHeader(buf, position, bytes.length)
    writePayload(buf, position + len, bytes)
    len + bytes.length
  }

  def packTimestampEpochSecond(buf: WriteBuffer, position: Int, epochSecond: Long, nanoAdjustment: Int): Int = {
    val sec  = Math.addExact(epochSecond, Math.floorDiv(nanoAdjustment, 1000000000L))
    val nsec = Math.floorMod(nanoAdjustment, 1000000000L).toInt

    if (sec >>> 34 eq 0) { // sec can be serialized in 34 bits.
      val data64 = (nsec << 34) | sec
      if ((data64 & 0xffffffff00000000L) == 0L) { // sec can be serialized in 32 bits and nsec is 0.
        // use timestamp 32
        writeTimestamp32(buf, position, sec.asInstanceOf[Int])
      } else { // sec exceeded 32 bits or nsec is not 0.
        // use timestamp 64
        writeTimestamp64(buf, position, data64)
      }
    } else { // use timestamp 96 format
      writeTimestamp96(buf, position, sec, nsec)
    }
  }

  private def writeTimestamp32(buf: WriteBuffer, position: Int, sec: Int): Int = { // timestamp 32 in fixext 4
    buf.ensureCapacity(position, 6)

    buf.writeByte(position, FIXEXT4)
    buf.writeByte(position + 1, EXT_TIMESTAMP)
    buf.writeInt(position + 2, sec)
    6
  }

  private def writeTimestamp64(buf: WriteBuffer, position: Int, data64: Long): Int = { // timestamp 64 in fixext 8
    buf.ensureCapacity(position, 10)

    buf.writeByte(position, FIXEXT8)
    buf.writeByte(position + 1, EXT_TIMESTAMP)
    buf.writeLong(position + 2, data64)
    10
  }

  private def writeTimestamp96(buf: WriteBuffer, position: Int, sec: Long, nsec: Int): Int = { // timestamp 96 in ext 8
    buf.ensureCapacity(15, position)

    buf.writeByte(position, EXT8)
    buf.writeByte(position + 1, 12.toByte)
    buf.writeByte(position + 2, EXT_TIMESTAMP)
    buf.writeInt(position + 3, nsec)
    buf.writeLong(position + 7, nsec)
    15
  }

  def packRawStringHeader(buf: WriteBuffer, position: Int, len: Int): Int = {
    if (len < (1 << 5)) {
      buf.writeByte(position, (FIXSTR_PREFIX | len).toByte)
    } else if (len < (1 << 8)) {
      buf.writeByteAndByte(position, STR8, len.toByte)
    } else if (len < (1 << 16)) {
      buf.writeByteAndShort(position, STR16, len.toShort)
    } else {
      buf.writeByteAndInt(position, STR32, len)
    }
  }

  def packArrayHeader(buf: WriteBuffer, position: Int, arraySize: Int): Int = {
    if (arraySize < 0)
      throw new IllegalArgumentException("array size must be >= 0")

    if (arraySize < (1 << 4))
      buf.writeByte(position, (FIXARRAY_PREFIX | arraySize).toByte)
    else if (arraySize < (1 << 16))
      buf.writeByteAndShort(position, ARRAY16, arraySize.toShort)
    else
      buf.writeByteAndInt(position, ARRAY32, arraySize)
  }

  def packMapHeader(buf: WriteBuffer, position: Int, mapSize: Int): Int = {
    if (mapSize < 0)
      throw new IllegalArgumentException("map size must be >= 0")

    if (mapSize < (1 << 4)) {
      buf.writeByte(position, (FIXMAP_PREFIX | mapSize).toByte)
    } else if (mapSize < (1 << 16)) {
      buf.writeByteAndShort(position, MAP16, mapSize.toShort)
    } else {
      buf.writeByteAndInt(position, MAP32, mapSize)
    }
  }

  def packExtensionTypeHeader(buf: WriteBuffer, position: Int, extType: Byte, payloadLen: Int): Int = {
    if (payloadLen < (1 << 8)) {
      if (payloadLen > 0 && (payloadLen & (payloadLen - 1)) == 0) { // check whether dataLen == 2^x
        if (payloadLen == 1)
          buf.writeByteAndByte(position, FIXEXT1, extType)
        else if (payloadLen == 2)
          buf.writeByteAndByte(position, FIXEXT2, extType)
        else if (payloadLen == 4)
          buf.writeByteAndByte(position, FIXEXT4, extType)
        else if (payloadLen == 8)
          buf.writeByteAndByte(position, FIXEXT8, extType)
        else if (payloadLen == 16)
          buf.writeByteAndByte(position, FIXEXT16, extType)
        else {
          buf.writeByteAndByte(position, EXT8, payloadLen.toByte)
          buf.writeByte(position + 2, extType)
          3
        }
      } else {
        buf.writeByteAndByte(position, EXT8, payloadLen.toByte)
        buf.writeByte(position + 2, extType)
        3
      }
    } else if (payloadLen < (1 << 16)) {
      buf.writeByteAndShort(position, EXT16, payloadLen.toShort)
      buf.writeByte(position + 3, extType)
      4
    } else {
      buf.writeByteAndInt(position, EXT32, payloadLen)
      buf.writeByte(position + 5, extType)
      // TODO support dataLen > 2^31 - 1
      6
    }
  }

  def packBinaryHeader(buf: WriteBuffer, position: Int, len: Int): Int = {
    if (len < (1 << 8)) {
      buf.writeByteAndByte(position, BIN8, len.toByte)
    } else if (len < (1 << 16)) {
      buf.writeByteAndShort(position, BIN16, len.toShort)
    } else {
      buf.writeByteAndInt(position, BIN32, len)
    }
  }

  def writePayload(buf: WriteBuffer, position: Int, v: Array[Byte]): Int = {
    buf.writeBytes(position, v)
  }

  def writePayload(buf: WriteBuffer, position: Int, v: Array[Byte], vOffset: Int, length: Int): Int = {
    buf.writeBytes(position, v, vOffset, length)
  }

}
