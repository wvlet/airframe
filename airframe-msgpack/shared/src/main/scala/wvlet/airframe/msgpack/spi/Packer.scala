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

import wvlet.airframe.msgpack.spi.Code._

/**
  * Write MessagePack code at a given position on the buffer and return the written byte length
  */
object Packer {
  def packNil(cursor: WriteCursor) {
    cursor.writeByte(NIL)
  }

  def packBoolean(cursor: WriteCursor, v: Boolean) {
    cursor.writeByte(if (v) TRUE else FALSE)
  }

  def packByte(cursor: WriteCursor, v: Byte) {
    if (v < -(1 << 5)) {
      cursor.writeByteAndByte(INT8, v)
    } else {
      cursor.writeByte(v)
    }
  }

  def packShort(cursor: WriteCursor, v: Short) {
    if (v < -(1 << 5)) {
      if (v < -(1 << 7)) {
        cursor.writeByteAndShort(INT16, v)
      } else {
        cursor.writeByteAndByte(INT8, v.toByte)
      }
    } else if (v < (1 << 7)) {
      cursor.writeByte(v.toByte)
    } else if (v < (1 << 8)) {
      cursor.writeByteAndByte(UINT8, v.toByte)
    } else {
      cursor.writeByteAndShort(UINT16, v)
    }
  }

  def packInt(cursor: WriteCursor, r: Int) {
    if (r < -(1 << 5)) {
      if (r < -(1 << 15)) {
        cursor.writeByteAndInt(INT32, r)
      } else if (r < -(1 << 7)) {
        cursor.writeByteAndShort(INT16, r.toShort)
      } else {
        cursor.writeByteAndByte(INT8, r.toByte)
      }
    } else if (r < (1 << 7)) {
      cursor.writeByte(r.toByte)
    } else if (r < (1 << 8)) {
      cursor.writeByteAndByte(UINT8, r.toByte)
    } else if (r < (1 << 16)) {
      cursor.writeByteAndShort(UINT16, r.toShort)
    } else { // unsigned 32
      cursor.writeByteAndInt(UINT32, r)
    }
  }

  def packLong(cursor: WriteCursor, v: Long) {
    if (v < -(1L << 5)) {
      if (v < -(1L << 15)) {
        if (v < -(1L << 31))
          cursor.writeByteAndLong(INT64, v)
        else
          cursor.writeByteAndInt(INT32, v.toInt)
      } else if (v < -(1 << 7)) {
        cursor.writeByteAndShort(INT16, v.toShort)
      } else {
        cursor.writeByteAndByte(INT8, v.toByte)
      }
    } else if (v < (1 << 7)) { // fixnum
      cursor.writeByte(v.toByte)
    } else if (v < (1L << 16)) {
      if (v < (1 << 8))
        cursor.writeByteAndByte(UINT8, v.toByte)
      else
        cursor.writeByteAndShort(UINT16, v.toShort)
    } else if (v < (1L << 32))
      cursor.writeByteAndInt(UINT32, v.toInt)
    else
      cursor.writeByteAndLong(UINT64, v)
  }

  def packBigInteger(cursor: WriteCursor, bi: BigInteger) {
    if (bi.bitLength <= 63) {
      packLong(cursor, bi.longValue)
    } else if (bi.bitLength == 64 && bi.signum == 1) {
      cursor.writeByteAndLong(UINT64, bi.longValue)
    } else {
      throw new IllegalArgumentException("MessagePack cannot serialize BigInteger larger than 2^64-1")
    }
  }

  def packFloat(cursor: WriteCursor, v: Float) {
    cursor.writeByteAndFloat(FLOAT32, v)
  }

  def packDouble(cursor: WriteCursor, v: Double) {
    cursor.writeByteAndDouble(FLOAT64, v)
  }

  def packString(cursor: WriteCursor, s: String) {
    val bytes = s.getBytes(StandardCharsets.UTF_8)
    // Write the length and payload of small string to the buffer so that it avoids an extra flush of buffer
    packRawStringHeader(cursor, bytes.length)
    writePayload(cursor, bytes)
  }

  def packTimestampEpochSecond(cursor: WriteCursor, epochSecond: Long, nanoAdjustment: Int) {
    val sec  = Math.addExact(epochSecond, Math.floorDiv(nanoAdjustment, 1000000000L))
    val nsec = Math.floorMod(nanoAdjustment, 1000000000L).toInt

    if ((sec >>> 34) == 0L) { // sec can be serialized in 34 bits.
      val data64 = (nsec << 34) | sec
      if ((data64 & 0xffffffff00000000L) == 0L) { // sec can be serialized in 32 bits and nsec is 0.
        // use timestamp 32
        writeTimestamp32(cursor, sec.asInstanceOf[Int])
      } else { // sec exceeded 32 bits or nsec is not 0.
        // use timestamp 64
        writeTimestamp64(cursor, data64)
      }
    } else { // use timestamp 96 format
      writeTimestamp96(cursor, sec, nsec)
    }
  }

  private def writeTimestamp32(cursor: WriteCursor, sec: Int) { // timestamp 32 in fixext 4
    cursor.ensureCapacity(6)

    cursor.writeByte(FIXEXT4)
    cursor.writeByte(EXT_TIMESTAMP)
    cursor.writeInt(sec)
  }

  private def writeTimestamp64(cursor: WriteCursor, data64: Long) { // timestamp 64 in fixext 8
    cursor.ensureCapacity(10)

    cursor.writeByte(FIXEXT8)
    cursor.writeByte(EXT_TIMESTAMP)
    cursor.writeLong(data64)
  }

  private def writeTimestamp96(cursor: WriteCursor, sec: Long, nsec: Int) { // timestamp 96 in ext 8
    cursor.ensureCapacity(15)

    cursor.writeByte(EXT8)
    cursor.writeByte(12.toByte)
    cursor.writeByte(EXT_TIMESTAMP)
    cursor.writeInt(nsec)
    cursor.writeLong(nsec)
  }

  def packRawStringHeader(cursor: WriteCursor, len: Int) {
    if (len < (1 << 5)) {
      cursor.writeByte((FIXSTR_PREFIX | len).toByte)
    } else if (len < (1 << 8)) {
      cursor.writeByteAndByte(STR8, len.toByte)
    } else if (len < (1 << 16)) {
      cursor.writeByteAndShort(STR16, len.toShort)
    } else {
      cursor.writeByteAndInt(STR32, len)
    }
  }

  def packArrayHeader(cursor: WriteCursor, arraySize: Int) {
    if (arraySize < 0)
      throw new IllegalArgumentException("array size must be >= 0")

    if (arraySize < (1 << 4))
      cursor.writeByte((FIXARRAY_PREFIX | arraySize).toByte)
    else if (arraySize < (1 << 16))
      cursor.writeByteAndShort(ARRAY16, arraySize.toShort)
    else
      cursor.writeByteAndInt(ARRAY32, arraySize)
  }

  def packMapHeader(cursor: WriteCursor, mapSize: Int) {
    if (mapSize < 0)
      throw new IllegalArgumentException("map size must be >= 0")

    if (mapSize < (1 << 4)) {
      cursor.writeByte((FIXMAP_PREFIX | mapSize).toByte)
    } else if (mapSize < (1 << 16)) {
      cursor.writeByteAndShort(MAP16, mapSize.toShort)
    } else {
      cursor.writeByteAndInt(MAP32, mapSize)
    }
  }

  def packExtTypeHeader(cursor: WriteCursor, extType: Byte, payloadLen: Int) {
    if (payloadLen < (1 << 8)) {
      if (payloadLen > 0 && (payloadLen & (payloadLen - 1)) == 0) { // check whether dataLen == 2^x
        if (payloadLen == 1)
          cursor.writeByteAndByte(FIXEXT1, extType)
        else if (payloadLen == 2)
          cursor.writeByteAndByte(FIXEXT2, extType)
        else if (payloadLen == 4)
          cursor.writeByteAndByte(FIXEXT4, extType)
        else if (payloadLen == 8)
          cursor.writeByteAndByte(FIXEXT8, extType)
        else if (payloadLen == 16)
          cursor.writeByteAndByte(FIXEXT16, extType)
        else {
          cursor.writeByteAndByte(EXT8, payloadLen.toByte)
          cursor.writeByte(extType)
        }
      } else {
        cursor.writeByteAndByte(EXT8, payloadLen.toByte)
        cursor.writeByte(extType)
      }
    } else if (payloadLen < (1 << 16)) {
      cursor.writeByteAndShort(EXT16, payloadLen.toShort)
      cursor.writeByte(extType)
    } else {
      cursor.writeByteAndInt(EXT32, payloadLen)
      cursor.writeByte(extType)
      // TODO support dataLen > 2^31 - 1
    }
  }

  def packBinaryHeader(cursor: WriteCursor, len: Int) {
    if (len < (1 << 8)) {
      cursor.writeByteAndByte(BIN8, len.toByte)
    } else if (len < (1 << 16)) {
      cursor.writeByteAndShort(BIN16, len.toShort)
    } else {
      cursor.writeByteAndInt(BIN32, len)
    }
  }

  def writePayload(cursor: WriteCursor, v: Array[Byte]) {
    cursor.writeBytes(v)
  }

  def writePayload(cursor: WriteCursor, v: Array[Byte], vOffset: Int, length: Int) {
    cursor.writeBytes(v, vOffset, length)
  }
}
