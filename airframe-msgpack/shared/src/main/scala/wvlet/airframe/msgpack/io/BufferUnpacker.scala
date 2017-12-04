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
import java.nio.ByteBuffer
import java.util.Locale

import wvlet.airframe.msgpack.spi.ErrorCode.{INSUFFICIENT_BUFFER, INVALID_TYPE, NEVER_USED_FORMAT, TOO_LARGE_MESSAGE}
import wvlet.airframe.msgpack.spi._

/**
  * Read a value from the buffer
  */
class BufferUnpacker {

  private var _lastReadByteLength: Int = 0

  def lastReadByteLength: Int = _lastReadByteLength

  private def unexpected(expectedCode: String, actual: Byte) = {
    val f = Code.messageFormatOf(actual)
    if (f == MessageFormat.NEVER_USED) {
      throw new MessagePackException(NEVER_USED_FORMAT, s"Expected ${expectedCode}, but found 0xC1 (NEVER_USED) byte")
    } else {
      val name     = f.getValueType.name()
      val typeName = name.substring(0, 1) + name.substring(1).toLowerCase(Locale.ENGLISH)
      throw new MessagePackException(INVALID_TYPE, f"Expected ${expectedCode}, but got ${typeName} (${actual}%02x)")
    }
  }

  def unpackNil(buf: Buffer, index: Int) {
    buf.readByte(index) match {
      case Code.NIL =>
        _lastReadByteLength = 1
      case other => unexpected("nil", other)
    }
  }

  def unpackBoolean(buf: Buffer, index: Int): Boolean = {
    buf.readByte(index) match {
      case Code.FALSE =>
        _lastReadByteLength = 1
        false
      case Code.TRUE =>
        _lastReadByteLength = 1
        true
      case other => unexpected("boolean", other)
    }
  }

  def unpackByte(buf: Buffer, index: Int): Byte = {
    val b = buf.readByte(index)
    b match {
      case b if Code.isFixInt(b) =>
        _lastReadByteLength = 1
        b
      case Code.UINT8 =>
        val u8 = buf.readByte(index + 1)
        if (u8 < 0) throw overflowU8(u8)
        _lastReadByteLength = 2
        u8
      case Code.UINT16 =>
        val u16 = buf.readShort(index + 1)
        if (u16 < 0 || !u16.isValidByte) throw overflowU16(u16)
        _lastReadByteLength = 3
        u16.toByte
      case Code.UINT32 =>
        val u32 = buf.readInt(index + 1)
        if (u32 < 0 || !u32.isValidByte) throw overflowU32(u32)
        _lastReadByteLength = 5
        u32.toByte
      case Code.UINT64 =>
        val u64 = buf.readLong(index + 1)
        if (u64 < 0 || !u64.isValidByte) throw overflowU64(u64)
        _lastReadByteLength = 9
        u64.toByte
      case Code.INT8 =>
        val i8 = buf.readByte(index)
        _lastReadByteLength = 2
        i8
      case Code.INT16 =>
        val i16 = buf.readShort(index)
        if (!i16.isValidByte) throw overflowI16(i16)
        _lastReadByteLength = 3
        i16.toByte
      case Code.INT32 =>
        val i32 = buf.readInt(index)
        if (!i32.isValidByte) throw overflowI32(i32)
        _lastReadByteLength = 5
        i32.toByte
      case Code.INT64 =>
        val i64 = buf.readLong(index)
        if (!i64.isValidByte) throw overflowI64(i64)
        _lastReadByteLength = 9
        i64.toByte
      case _ =>
        unexpected("Integer", b)
    }
  }

  def unpackShort(buf: Buffer, index: Int): Short = {
    val b = buf.readByte(index)
    b match {
      case b if Code.isFixInt(b) =>
        _lastReadByteLength = 1
        b.toShort
      case Code.UINT8 =>
        val u8 = buf.readByte(index + 1)
        _lastReadByteLength = 2
        (u8 & 0xff).toShort
      case Code.UINT16 =>
        val u16 = buf.readShort(index + 1)
        if (u16 < 0) throw overflowU16(u16)
        _lastReadByteLength = 3
        u16.toShort
      case Code.UINT32 =>
        val u32 = buf.readInt(index + 1)
        if (u32 < 0 || !u32.isValidShort) throw overflowU32(u32)
        _lastReadByteLength = 5
        u32.toShort
      case Code.UINT64 =>
        val u64 = buf.readLong(index + 1)
        if (u64 < 0 || !u64.isValidShort) throw overflowU64(u64)
        _lastReadByteLength = 9
        u64.toShort
      case Code.INT8 =>
        val i8 = buf.readByte(index)
        _lastReadByteLength = 2
        i8.toShort
      case Code.INT16 =>
        val i16 = buf.readShort(index)
        _lastReadByteLength = 3
        i16.toShort
      case Code.INT32 =>
        val i32 = buf.readInt(index)
        if (!i32.isValidShort) throw overflowI32(i32)
        _lastReadByteLength = 5
        i32.toShort
      case Code.INT64 =>
        val i64 = buf.readLong(index)
        if (!i64.isValidShort) throw overflowI64(i64)
        _lastReadByteLength = 9
        i64.toShort
      case _ =>
        unexpected("Integer", b)
    }
  }

  def overflowU8(u8: Byte)    = new IntegerOverflowException(BigInteger.valueOf((u8 & 0xFF).toLong))
  def overflowU16(u16: Short) = new IntegerOverflowException(BigInteger.valueOf((u16 & 0xFFFF).toLong))
  def overflowU32(u32: Int)   = new IntegerOverflowException(BigInteger.valueOf((u32 & 0xFFFFFFFF).toLong))
  def overflowU64(u64: Long)  = new IntegerOverflowException(BigInteger.valueOf(u64 + Long.MaxValue + 1L).setBit(63))

  def overflowI16(i16: Short) = new IntegerOverflowException(BigInteger.valueOf(i16.toLong))
  def overflowI32(i32: Int)   = new IntegerOverflowException(BigInteger.valueOf(i32.toLong))
  def overflowI64(i64: Long)  = new IntegerOverflowException(BigInteger.valueOf(i64))

  def overflowU32Size(u32: Int) = new TooLargeMessageException(((u32 & 0x7fffffff) + 0x80000000L).toLong)
}
