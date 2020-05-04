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

import wvlet.airframe.msgpack.spi.Code._

/**
  * Write MessagePack code at a given position on the buffer and return the written byte length
  */
object OffsetPacker {
  def packValue(cursor: WriteCursor, v: Value): Unit = {
    v match {
      case Value.NilValue =>
        packNil(cursor)
      case Value.BooleanValue(v) =>
        packBoolean(cursor, v)
      case Value.LongValue(v) =>
        packLong(cursor, v)
      case Value.BigIntegerValue(v) =>
        packBigInteger(cursor, v)
      case Value.DoubleValue(v) =>
        packDouble(cursor, v)
      case Value.StringValue(v) =>
        packString(cursor, v)
      case Value.BinaryValue(v) =>
        packBinaryHeader(cursor, v.length)
        writePayload(cursor, v)
      case Value.ExtensionValue(extType, v) =>
        packExtTypeHeader(cursor, extType, v.length)
        writePayload(cursor, v)
      case Value.TimestampValue(v) =>
        packTimestamp(cursor, v)
      case a @ Value.ArrayValue(elems) =>
        packArrayHeader(cursor, a.size)
        elems.foreach(packValue(cursor, _))
      case m @ Value.MapValue(entries) =>
        packMapHeader(cursor, m.size)
        entries.toIndexedSeq.foreach { x =>
          packValue(cursor, x._1)
          packValue(cursor, x._2)
        }
    }
  }

  def packNil(cursor: WriteCursor): Unit = {
    cursor.writeByte(NIL)
  }

  def packBoolean(cursor: WriteCursor, v: Boolean): Unit = {
    cursor.writeByte(if (v) TRUE else FALSE)
  }

  private[msgpack] def packFIXNUM(cursor: WriteCursor, v: Byte): Unit = {
    cursor.writeByte(v)
  }
  private[msgpack] def packINT8(cursor: WriteCursor, v: Byte): Unit = {
    cursor.writeByteAndByte(INT8, v)
  }
  private[msgpack] def packINT16(cursor: WriteCursor, v: Short): Unit = {
    cursor.writeByteAndShort(INT16, v)
  }
  private[msgpack] def packINT32(cursor: WriteCursor, v: Int): Unit = {
    cursor.writeByteAndInt(INT32, v)
  }
  private[msgpack] def packINT64(cursor: WriteCursor, v: Long): Unit = {
    cursor.writeByteAndLong(INT64, v)
  }
  private[msgpack] def packUINT8(cursor: WriteCursor, v: Byte): Unit = {
    cursor.writeByteAndByte(UINT8, v)
  }
  private[msgpack] def packUINT16(cursor: WriteCursor, v: Short): Unit = {
    cursor.writeByteAndShort(UINT16, v)
  }
  private[msgpack] def packUINT32(cursor: WriteCursor, v: Int): Unit = {
    cursor.writeByteAndInt(UINT32, v)
  }
  private[msgpack] def packUINT64(cursor: WriteCursor, v: Long): Unit = {
    cursor.writeByteAndLong(UINT64, v)
  }
  private[msgpack] def packFLOAT32(cursor: WriteCursor, v: Float): Unit = {
    cursor.writeByteAndFloat(FLOAT32, v)
  }
  private[msgpack] def packFLOAT64(cursor: WriteCursor, v: Double): Unit = {
    cursor.writeByteAndDouble(FLOAT64, v)
  }

  def packByte(cursor: WriteCursor, v: Byte): Unit = {
    if (v < -(1 << 5)) {
      packINT8(cursor, v)
    } else {
      packFIXNUM(cursor, v)
    }
  }

  def packShort(cursor: WriteCursor, v: Short): Unit = {
    if (v < -(1 << 5)) {
      if (v < -(1 << 7)) {
        packINT16(cursor, v)
      } else {
        packINT8(cursor, v.toByte)
      }
    } else if (v < (1 << 7)) {
      cursor.writeByte(v.toByte)
    } else if (v < (1 << 8)) {
      packUINT8(cursor, v.toByte)
    } else {
      packUINT16(cursor, v)
    }
  }

  def packInt(cursor: WriteCursor, r: Int): Unit = {
    if (r < -(1 << 5)) {
      if (r < -(1 << 15)) {
        packINT32(cursor, r)
      } else if (r < -(1 << 7)) {
        packINT16(cursor, r.toShort)
      } else {
        packINT8(cursor, r.toByte)
      }
    } else if (r < (1 << 7)) {
      packFIXNUM(cursor, r.toByte)
    } else if (r < (1 << 8)) {
      packUINT8(cursor, r.toByte)
    } else if (r < (1 << 16)) {
      packUINT16(cursor, r.toShort)
    } else { // unsigned 32
      packUINT32(cursor, r)
    }
  }

  def packLong(cursor: WriteCursor, v: Long): Unit = {
    if (v < -(1L << 5)) {
      if (v < -(1L << 15)) {
        if (v < -(1L << 31))
          packINT64(cursor, v)
        else
          packINT32(cursor, v.toInt)
      } else if (v < -(1 << 7)) {
        packINT16(cursor, v.toShort)
      } else {
        packINT8(cursor, v.toByte)
      }
    } else if (v < (1 << 7)) { // fixnum
      packFIXNUM(cursor, v.toByte)
    } else if (v < (1L << 16)) {
      if (v < (1 << 8))
        packUINT8(cursor, v.toByte)
      else
        packUINT16(cursor, v.toShort)
    } else if (v < (1L << 32))
      packUINT32(cursor, v.toInt)
    else
      packUINT64(cursor, v)
  }

  def packBigInteger(cursor: WriteCursor, bi: BigInteger): Unit = {
    if (bi.bitLength <= 63) {
      packLong(cursor, bi.longValue)
    } else if (bi.bitLength == 64 && bi.signum == 1) {
      packUINT64(cursor, bi.longValue)
    } else {
      throw new IllegalArgumentException(s"MessagePack cannot serialize BigInteger larger than 2^64-1: ${bi}")
    }
  }

  def packFloat(cursor: WriteCursor, v: Float): Unit = {
    packFLOAT32(cursor, v)
  }

  def packDouble(cursor: WriteCursor, v: Double): Unit = {
    packFLOAT64(cursor, v)
  }

  def packString(cursor: WriteCursor, s: String): Unit = {
    val bytes = s.getBytes(StandardCharsets.UTF_8)
    // Write the length and payload of small string to the buffer so that it avoids an extra flush of buffer
    packRawStringHeader(cursor, bytes.length)
    writePayload(cursor, bytes)
  }

  def packTimestamp(cursor: WriteCursor, v: Instant): Unit = {
    packTimestampEpochSecond(cursor, v.getEpochSecond, v.getNano)
  }

  private val NANOS_PER_SECOND = 1000000000L
  def packTimestampEpochSecond(cursor: WriteCursor, epochSecond: Long, nanoAdjustment: Int): Unit = {
    val sec  = Math.addExact(epochSecond, Math.floorDiv(nanoAdjustment, NANOS_PER_SECOND))
    val nsec = Math.floorMod(nanoAdjustment.toLong, NANOS_PER_SECOND)

    if ((sec >>> 34) == 0L) { // sec can be serialized in 34 bits.
      val data64: Long = (nsec << 34) | sec
      if ((data64 & 0xffffffff00000000L) == 0L) { // sec can be serialized in 32 bits and nsec is 0.
        // use timestamp 32
        packTimestamp32(cursor, sec.toInt)
      } else { // sec exceeded 32 bits or nsec is not 0.
        // use timestamp 64
        packTimestamp64(cursor, data64)
      }
    } else { // use timestamp 96 format
      packTimestamp96(cursor, sec, nsec.toInt)
    }
  }

  private[msgpack] def packTimestamp32(cursor: WriteCursor, sec: Int): Unit = { // timestamp 32 in fixext 4
    cursor.ensureCapacity(6)

    cursor.writeByte(FIXEXT4)
    cursor.writeByte(EXT_TIMESTAMP)
    cursor.writeInt(sec)
  }

  private[msgpack] def packTimestamp64(cursor: WriteCursor, data64: Long): Unit = { // timestamp 64 in fixext 8
    cursor.ensureCapacity(10)

    cursor.writeByte(FIXEXT8)
    cursor.writeByte(EXT_TIMESTAMP)
    cursor.writeLong(data64)
  }

  private[msgpack] def packTimestamp96(cursor: WriteCursor, sec: Long, nsec: Int): Unit = { // timestamp 96 in ext 8
    cursor.ensureCapacity(15)

    cursor.writeByte(EXT8)
    cursor.writeByte(12.toByte)
    cursor.writeByte(EXT_TIMESTAMP)
    cursor.writeInt(nsec)
    cursor.writeLong(sec)
  }

  def packRawStringHeader(cursor: WriteCursor, len: Int): Unit = {
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

  def packArrayHeader(cursor: WriteCursor, arraySize: Int): Unit = {
    if (arraySize < 0)
      throw new IllegalArgumentException("array size must be >= 0")

    if (arraySize < (1 << 4))
      cursor.writeByte((FIXARRAY_PREFIX | arraySize).toByte)
    else if (arraySize < (1 << 16))
      cursor.writeByteAndShort(ARRAY16, arraySize.toShort)
    else
      cursor.writeByteAndInt(ARRAY32, arraySize)
  }

  def packArray32Header(cursor: WriteCursor, arraySize: Int): Unit = {
    cursor.writeByteAndInt(ARRAY32, arraySize)
  }

  def packMapHeader(cursor: WriteCursor, mapSize: Int): Unit = {
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

  def packMap32Header(cursor: WriteCursor, mapSize: Int) = {
    cursor.writeByteAndInt(MAP32, mapSize)
  }

  def packExtTypeHeader(cursor: WriteCursor, extTypeHeader: ExtTypeHeader): Unit = {
    packExtTypeHeader(cursor, extTypeHeader.extType, extTypeHeader.byteLength)
  }

  def packExtTypeHeader(cursor: WriteCursor, extType: Byte, payloadLen: Int): Unit = {
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

  def packBinaryHeader(cursor: WriteCursor, len: Int): Unit = {
    if (len < (1 << 8)) {
      cursor.writeByteAndByte(BIN8, len.toByte)
    } else if (len < (1 << 16)) {
      cursor.writeByteAndShort(BIN16, len.toShort)
    } else {
      cursor.writeByteAndInt(BIN32, len)
    }
  }

  def writePayload(cursor: WriteCursor, v: Array[Byte]): Unit = {
    cursor.writeBytes(v)
  }

  def writePayload(cursor: WriteCursor, v: Array[Byte], vOffset: Int, length: Int): Unit = {
    cursor.writeBytes(v, vOffset, length)
  }
}
