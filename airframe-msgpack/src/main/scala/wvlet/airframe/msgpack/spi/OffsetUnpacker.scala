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

import wvlet.airframe.msgpack.spi.ErrorCode.{INVALID_EXT_FORMAT, INVALID_TYPE, NEVER_USED_FORMAT}
import wvlet.airframe.msgpack.spi.MessageException._
import wvlet.airframe.msgpack.spi.MessageFormat._
import wvlet.airframe.msgpack.spi.Value._

import scala.collection.immutable.ListMap
import scala.collection.mutable

/**
  * Read a message pack data from a given offset in the buffer. The last read byte length can be checked by calling [[ReadCursor.lastReadLength]] method.
  */
object OffsetUnpacker {
  def peekNextFormat(cursor: ReadCursor): MessageFormat = {
    val b  = cursor.peekByte
    val mf = MessageFormat.of(b)
    mf
  }

  def skipValue(cursor: ReadCursor, skipCount: Int = 1): Unit = {
    var count = skipCount
    while (count > 0) {
      val b  = cursor.readByte
      val mf = MessageFormat.of(b)
      mf match {
        case POSFIXINT | NEGFIXINT | BOOLEAN | NIL =>
        case FIXMAP =>
          val mapLen = b & 0x0f
          count += mapLen * 2
        case FIXARRAY =>
          val arrayLen = b & 0x0f
          count += arrayLen
        case FIXSTR =>
          val strLen = b & 0x1f
          skipPayload(cursor, strLen)
        case INT8 | UINT8 =>
          skipPayload(cursor, 1)
        case INT16 | UINT16 =>
          skipPayload(cursor, 2)
        case INT32 | UINT32 | FLOAT32 =>
          skipPayload(cursor, 4)
        case INT64 | UINT64 | FLOAT64 =>
          skipPayload(cursor, 8)
        case BIN8 | STR8 =>
          skipPayload(cursor, readNextLength8(cursor))
        case BIN16 | STR16 =>
          skipPayload(cursor, readNextLength16(cursor))
        case BIN32 | STR32 =>
          skipPayload(cursor, readNextLength32(cursor))
        case FIXEXT1 =>
          skipPayload(cursor, 2)
        case FIXEXT2 =>
          skipPayload(cursor, 3)
        case FIXEXT4 =>
          skipPayload(cursor, 5)
        case FIXEXT8 =>
          skipPayload(cursor, 9)
        case FIXEXT16 =>
          skipPayload(cursor, 17)
        case EXT8 =>
          skipPayload(cursor, readNextLength8(cursor) + 1)
        case EXT16 =>
          skipPayload(cursor, readNextLength16(cursor) + 1)
        case EXT32 =>
          skipPayload(cursor, readNextLength32(cursor) + 1)
        case ARRAY16 =>
          count += readNextLength16(cursor)
        case ARRAY32 =>
          count += readNextLength32(cursor)
        case MAP16 =>
          count += readNextLength16(cursor) * 2
        case MAP32 =>
          count += readNextLength32(cursor) * 2
        case NEVER_USED =>
          throw new MessageException(NEVER_USED_FORMAT, s"Found 0xC1 (NEVER_USED) byte while skipping a value")
      }
      count -= 1
    }
  }

  def skipPayload(cursor: ReadCursor, numBytes: Int): Unit = {
    cursor.skipBytes(numBytes)
  }

  def unpackValue(cursor: ReadCursor): Value = {
    val b  = cursor.peekByte
    val mf = MessageFormat.of(b)
    mf.valueType match {
      case ValueType.NIL =>
        cursor.skipBytes(1)
        NilValue
      case ValueType.BOOLEAN =>
        Value.BooleanValue(unpackBoolean(cursor))
      case ValueType.INTEGER =>
        mf match {
          case MessageFormat.UINT64 =>
            BigIntegerValue(unpackBigInteger(cursor))
          case _ =>
            LongValue(unpackLong(cursor))
        }
      case ValueType.FLOAT =>
        DoubleValue(unpackDouble(cursor))
      case ValueType.STRING =>
        StringValue(unpackString(cursor))
      case ValueType.BINARY =>
        val binaryLength = unpackBinaryHeader(cursor)
        val data         = readPayload(cursor, binaryLength)
        BinaryValue(data)
      case ValueType.EXTENSION =>
        val extHeader = unpackExtTypeHeader(cursor)
        unpackExt(extHeader, cursor)
      case ValueType.ARRAY =>
        val arrayLength = unpackArrayHeader(cursor)
        val arr         = IndexedSeq.newBuilder[Value]
        arr.sizeHint(arrayLength)
        var i = 0
        while (i < arrayLength) {
          arr += unpackValue(cursor)
          i += 1
        }
        ArrayValue(arr.result())
      case ValueType.MAP =>
        var readLen   = 0
        val mapLength = unpackMapHeader(cursor)
        val map       = ListMap.newBuilder[Value, Value]
        map.sizeHint(mapLength)
        var i = 0
        while (i < mapLength) {
          val key   = unpackValue(cursor)
          val value = unpackValue(cursor)
          map += (key -> value)
          i += 1
        }
        MapValue(map.result)
    }
  }

  def unpackExt(extHeader: ExtTypeHeader, cursor: ReadCursor): Value = {
    if (extHeader.extType == Code.EXT_TIMESTAMP) {
      val instant = unpackTimestamp(extHeader, cursor)
      TimestampValue(instant)
    } else {
      val extData = readPayload(cursor, extHeader.byteLength)
      ExtensionValue(extHeader.extType, extData)
    }
  }

  def unpackNil(cursor: ReadCursor): Unit = {
    cursor.readByte match {
      case Code.NIL => // OK
      case other =>
        cursor.reverseCursor
        unexpected(ValueType.NIL, other)
    }
  }

  def tryUnpackNil(cursor: ReadCursor): Boolean = {
    cursor.readByte match {
      case Code.NIL =>
        true
      case other =>
        cursor.reverseCursor
        false
    }
  }

  def unpackBoolean(cursor: ReadCursor): Boolean = {
    cursor.readByte match {
      case Code.FALSE =>
        false
      case Code.TRUE =>
        true
      case other =>
        cursor.reverseCursor
        unexpected(ValueType.BOOLEAN, other)
    }
  }

  def unpackByte(cursor: ReadCursor): Byte = {
    val b = cursor.readByte
    b match {
      case b if Code.isFixInt(b) =>
        b
      case Code.UINT8 =>
        val u8 = cursor.readByte
        if (u8 < 0) throw overflowU8(u8)
        u8
      case Code.UINT16 =>
        val u16 = cursor.readShort
        if (u16 < 0 || !u16.isValidByte) throw overflowU16(u16)
        u16.toByte
      case Code.UINT32 =>
        val u32 = cursor.readInt
        if (u32 < 0 || !u32.isValidByte) throw overflowU32(u32)
        u32.toByte
      case Code.UINT64 =>
        val u64 = cursor.readLong
        if (u64 < 0 || !u64.isValidByte) throw overflowU64(u64)
        u64.toByte
      case Code.INT8 =>
        val i8 = cursor.readByte
        i8
      case Code.INT16 =>
        val i16 = cursor.readShort
        if (!i16.isValidByte) throw overflowI16(i16)
        i16.toByte
      case Code.INT32 =>
        val i32 = cursor.readInt
        if (!i32.isValidByte) throw overflowI32(i32)
        i32.toByte
      case Code.INT64 =>
        val i64 = cursor.readLong
        if (!i64.isValidByte) throw overflowI64(i64)
        i64.toByte
      case _ =>
        cursor.reverseCursor
        unexpected(ValueType.INTEGER, b)
    }
  }

  def unpackShort(cursor: ReadCursor): Short = {
    val b = cursor.readByte
    b match {
      case b if Code.isFixInt(b) =>
        b.toShort
      case Code.UINT8 =>
        val u8 = cursor.readByte
        (u8 & 0xff).toShort
      case Code.UINT16 =>
        val u16 = cursor.readShort
        if (u16 < 0) throw overflowU16(u16)
        u16.toShort
      case Code.UINT32 =>
        val u32 = cursor.readInt
        if (u32 < 0 || !u32.isValidShort) throw overflowU32(u32)
        u32.toShort
      case Code.UINT64 =>
        val u64 = cursor.readLong
        if (u64 < 0 || !u64.isValidShort) throw overflowU64(u64)
        u64.toShort
      case Code.INT8 =>
        val i8 = cursor.readByte
        i8.toShort
      case Code.INT16 =>
        val i16 = cursor.readShort
        i16.toShort
      case Code.INT32 =>
        val i32 = cursor.readInt
        if (!i32.isValidShort) throw overflowI32(i32)
        i32.toShort
      case Code.INT64 =>
        val i64 = cursor.readLong
        if (!i64.isValidShort) throw overflowI64(i64)
        i64.toShort
      case _ =>
        cursor.reverseCursor
        unexpected(ValueType.INTEGER, b)
    }
  }

  def unpackInt(cursor: ReadCursor): Int = {
    val b = cursor.readByte
    b match {
      case b if Code.isFixInt(b) =>
        b.toInt
      case Code.UINT8 =>
        val u8 = cursor.readByte
        u8 & 0xff
      case Code.UINT16 =>
        val u16 = cursor.readShort
        u16 & 0xffff
      case Code.UINT32 =>
        val u32 = cursor.readInt
        if (u32 < 0) throw overflowU32(u32)
        u32
      case Code.UINT64 =>
        val u64 = cursor.readLong
        if (u64 < 0 || !u64.isValidInt) throw overflowU64(u64)
        u64.toInt
      case Code.INT8 =>
        val i8 = cursor.readByte
        i8.toInt
      case Code.INT16 =>
        val i16 = cursor.readShort
        i16.toInt
      case Code.INT32 =>
        val i32 = cursor.readInt
        i32.toInt
      case Code.INT64 =>
        val i64 = cursor.readLong
        if (!i64.isValidInt) throw overflowI64(i64)
        i64.toInt
      case _ =>
        cursor.reverseCursor
        unexpected(ValueType.INTEGER, b)
    }
  }

  def unpackLong(cursor: ReadCursor): Long = {
    val b = cursor.readByte
    b match {
      case b if Code.isFixInt(b) =>
        b.toLong
      case Code.UINT8 =>
        val u8 = cursor.readByte
        (u8 & 0xff).toLong
      case Code.UINT16 =>
        val u16 = cursor.readShort
        (u16 & 0xffff).toLong
      case Code.UINT32 =>
        val u32 = cursor.readInt
        if (u32 < 0) {
          (u32 & 0x7fffffff).toLong + 0x80000000L
        } else {
          u32.toLong
        }
      case Code.UINT64 =>
        val u64 = cursor.readLong
        if (u64 < 0) throw overflowU64(u64)
        u64.toLong
      case Code.INT8 =>
        val i8 = cursor.readByte
        i8.toLong
      case Code.INT16 =>
        val i16 = cursor.readShort
        i16.toLong
      case Code.INT32 =>
        val i32 = cursor.readInt
        i32.toLong
      case Code.INT64 =>
        val i64 = cursor.readLong
        i64
      case _ =>
        cursor.reverseCursor
        unexpected(ValueType.INTEGER, b)
    }
  }

  def unpackBigInteger(cursor: ReadCursor): BigInteger = {
    val b = cursor.readByte
    b match {
      case b if Code.isFixInt(b) =>
        BigInteger.valueOf(b.toLong)
      case Code.UINT8 =>
        val u8 = cursor.readByte
        BigInteger.valueOf((u8 & 0xff).toLong)
      case Code.UINT16 =>
        val u16 = cursor.readShort
        BigInteger.valueOf((u16 & 0xffff).toLong)
      case Code.UINT32 =>
        val u32 = cursor.readInt
        if (u32 < 0) {
          BigInteger.valueOf((u32 & 0x7fffffff).toLong + 0x80000000L)
        } else {
          BigInteger.valueOf(u32.toLong)
        }
      case Code.UINT64 =>
        val u64 = cursor.readLong
        if (u64 < 0) {
          BigInteger.valueOf(u64 + Long.MaxValue + 1L).setBit(63)
        } else {
          BigInteger.valueOf(u64.toLong)
        }
      case Code.INT8 =>
        val i8 = cursor.readByte
        BigInteger.valueOf(i8.toLong)
      case Code.INT16 =>
        val i16 = cursor.readShort
        BigInteger.valueOf(i16.toLong)
      case Code.INT32 =>
        val i32 = cursor.readInt
        BigInteger.valueOf(i32.toLong)
      case Code.INT64 =>
        val i64 = cursor.readLong
        BigInteger.valueOf(i64)
      case _ =>
        cursor.reverseCursor
        unexpected(ValueType.INTEGER, b)
    }
  }

  def unpackFloat(cursor: ReadCursor): Float = {
    cursor.readByte match {
      case Code.FLOAT32 =>
        val f = cursor.readFloat
        f
      case Code.FLOAT64 =>
        val d = cursor.readDouble
        d.toFloat
      case other =>
        cursor.reverseCursor
        throw unexpected(ValueType.FLOAT, other)
    }
  }

  def unpackDouble(cursor: ReadCursor): Double = {
    cursor.readByte match {
      case Code.FLOAT32 =>
        val f = cursor.readFloat
        f.toDouble
      case Code.FLOAT64 =>
        val d = cursor.readDouble
        d
      case other =>
        cursor.reverseCursor
        throw unexpected(ValueType.FLOAT, other)
    }
  }

  private def readNextLength8(cursor: ReadCursor): Int = {
    val u8 = cursor.readByte
    u8 & 0xff
  }

  private def readNextLength16(cursor: ReadCursor): Int = {
    val u16 = cursor.readShort
    u16 & 0xffff
  }

  private def readNextLength32(cursor: ReadCursor): Int = {
    val u32 = cursor.readInt
    if (u32 < 0) {
      cursor.reverseCursor
      throw overflowU32Size(u32)
    }
    u32
  }

  private def tryReadStringHeader(b: Byte, cursor: ReadCursor) =
    b match {
      case Code.STR8 =>
        readNextLength8(cursor)
      case Code.STR16 =>
        readNextLength16(cursor)
      case Code.STR32 =>
        readNextLength32(cursor)
      case _ =>
        -1
    }

  private def tryReadBinaryHeader(b: Byte, cursor: ReadCursor) =
    b match {
      case Code.BIN8 => // bin 8
        readNextLength8(cursor)
      case Code.BIN16 => // bin 16
        readNextLength16(cursor)
      case Code.BIN32 => // bin 32
        readNextLength32(cursor)
      case _ =>
        -1
    }

  def unpackRawStringHeader(cursor: ReadCursor): Int = {
    val b = cursor.readByte
    if (Code.isFixedRaw(b)) {
      b & 0x1f
    } else {
      val slen = tryReadStringHeader(b, cursor)
      if (slen >= 0) {
        slen
      } else {
        val blen = tryReadBinaryHeader(b, cursor)
        if (blen >= 0) {
          blen
        } else {
          cursor.reverseCursor
          throw unexpected(ValueType.STRING, b)
        }
      }
    }
  }

  def unpackBinaryHeader(cursor: ReadCursor): Int = {
    val b = cursor.readByte
    if (Code.isFixedRaw(b)) {
      b & 0x1f
    } else {
      val blen = tryReadBinaryHeader(b, cursor)
      if (blen >= 0) {
        blen
      } else {
        val slen = tryReadStringHeader(b, cursor)
        if (slen >= 0) {
          slen
        } else {
          cursor.reverseCursor
          throw unexpected(ValueType.BINARY, b)
        }
      }
    }
  }

  def unpackString(cursor: ReadCursor): String = {
    val len       = unpackRawStringHeader(cursor)
    val headerLen = cursor.lastReadLength
    if (len == 0) {
      EMPTY_STRING
    } else if (len >= Integer.MAX_VALUE) {
      cursor.reverseCursor
      throw new TooLargeMessageException(len)
    } else {
      // TODO reduce memory copy (e.g., by getting a memory reference of the Buffer)
      val str = cursor.readBytes(len)
      new String(str, 0, str.length, StandardCharsets.UTF_8)
    }
  }

  def unpackArrayHeader(cursor: ReadCursor): Int = {
    val b = cursor.readByte
    b match {
      case b if Code.isFixedArray(b) =>
        b & 0x0f
      case Code.ARRAY16 =>
        val len = readNextLength16(cursor)
        len
      case Code.ARRAY32 =>
        val len = readNextLength32(cursor)
        len
      case _ =>
        cursor.reverseCursor
        throw unexpected(ValueType.ARRAY, b)
    }
  }

  def unpackMapHeader(cursor: ReadCursor): Int = {
    val b = cursor.readByte
    b match {
      case b if Code.isFixedMap(b) =>
        b & 0x0f
      case Code.MAP16 =>
        val len = readNextLength16(cursor)
        len
      case Code.MAP32 =>
        val len = readNextLength32(cursor)
        len
      case _ =>
        cursor.reverseCursor
        throw unexpected(ValueType.MAP, b)
    }
  }

  def unpackExtTypeHeader(cursor: ReadCursor): ExtTypeHeader = {
    cursor.readByte match {
      case Code.FIXEXT1 =>
        val tpe = cursor.readByte
        ExtTypeHeader(tpe, 1)
      case Code.FIXEXT2 =>
        val tpe = cursor.readByte
        ExtTypeHeader(tpe, 2)
      case Code.FIXEXT4 =>
        val tpe = cursor.readByte
        ExtTypeHeader(tpe, 4)
      case Code.FIXEXT8 =>
        val tpe = cursor.readByte
        ExtTypeHeader(tpe, 8)
      case Code.FIXEXT16 =>
        val tpe = cursor.readByte
        ExtTypeHeader(tpe, 16)
      case Code.EXT8 =>
        val u8  = cursor.readByte
        val len = u8 & 0xff
        val tpe = cursor.readByte
        ExtTypeHeader(tpe, len)
      case Code.EXT16 =>
        val u16 = cursor.readShort
        val len = u16 & 0xffff
        val tpe = cursor.readByte
        ExtTypeHeader(tpe, len)
      case Code.EXT32 =>
        val u32 = cursor.readInt
        if (u32 < 0) {
          cursor.reverseCursor
          throw overflowU32Size(u32)
        }
        val tpe = cursor.readByte
        ExtTypeHeader(tpe, u32)
      case other =>
        cursor.reverseCursor
        throw unexpected(ValueType.EXTENSION, other)
    }
  }

  def unpackTimestamp(cursor: ReadCursor): Instant = {
    val extTypeHeader = unpackExtTypeHeader(cursor)
    val instant       = unpackTimestamp(extTypeHeader, cursor)
    instant
  }

  def unpackTimestamp(extTypeHeader: ExtTypeHeader, cursor: ReadCursor): Instant = {
    if (extTypeHeader.extType != Code.EXT_TIMESTAMP) {
      cursor.reverseCursor
      throw unexpected(ValueType.EXTENSION, extTypeHeader.extType)
    }
    val instant = extTypeHeader.byteLength match {
      case 4 =>
        val u32 = cursor.readInt & 0xffffffffL
        Instant.ofEpochSecond(u32)
      case 8 =>
        val d64  = cursor.readLong
        val sec  = d64 & 0x00000003ffffffffL
        val nsec = (d64 >>> 34).toInt
        Instant.ofEpochSecond(sec, nsec)
      case 12 =>
        val nsecU32 = cursor.readInt & 0xffffffffL
        val sec     = cursor.readLong
        Instant.ofEpochSecond(sec, nsecU32)
      case other =>
        cursor.reverseCursor
        throw new MessageException(
          INVALID_EXT_FORMAT,
          s"Timestamp type expects 4, 8, or 12 bytes of payload but got ${other} bytes"
        )
    }
    instant
  }

  def readPayload(cursor: ReadCursor, length: Int): Array[Byte] = {
    val data = cursor.readBytes(length)
    data
  }

  def readPayload(cursor: ReadCursor, length: Int, dest: Array[Byte], destOffset: Int): Unit = {
    cursor.readBytes(length, dest, destOffset)
  }

  /**
    * Read a payload of the given length from the given buffer[position], and write the result to the destination buffer.
    *
    * @param cursor
    * @param length
    * @param dest
    * @param destIndex
    * @return A slice (shallow copy) of the destination buffer
    */
  def readPayload(cursor: ReadCursor, length: Int, dest: WriteBuffer, destIndex: Int): ReadBuffer = {
    cursor.readBytes(length, dest, destIndex)
    dest.slice(destIndex, length)
  }

  private val EMPTY_STRING: String = ""

  private[spi] def unexpected(expectedCode: ValueType, actual: Byte): Nothing = {
    val f = MessageFormat.of(actual)
    if (f == MessageFormat.NEVER_USED) {
      throw new MessageException(NEVER_USED_FORMAT, s"Expected ${expectedCode}, but found 0xC1 (NEVER_USED) byte")
    } else {
      val name     = f.valueType.name
      val typeName = name.substring(0, 1) + name.substring(1).toLowerCase
      throw new MessageException(INVALID_TYPE, f"Expected ${expectedCode}, but got ${typeName} (${actual}%02x)")
    }
  }
}
