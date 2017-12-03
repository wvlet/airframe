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
object BufferPacker {
  def packNil(buf: Buffer, offset: Int): Int = {
    buf.writeByte(offset, NIL)
  }

  def packBoolean(buf: Buffer, offset: Int, v: Boolean): Int = {
    buf.writeByte(offset, if (v) TRUE else FALSE)
  }

  def packByte(buf: Buffer, offset: Int, v: Byte): Int = {
    if (v < -(1 << 5)) {
      buf.writeByteAndByte(offset, INT8, v)
    } else {
      buf.writeByte(offset, v)
    }
  }

  def packShort(buf: Buffer, offset: Int, v: Short): Int = {
    if (v < -(1 << 5)) {
      if (v < -(1 << 7)) {
        buf.writeByteAndShort(offset, INT16, v)
      } else {
        buf.writeByteAndByte(offset, INT8, v.toByte)
      }
    } else if (v < (1 << 7)) {
      buf.writeByte(offset, v.toByte)
    } else if (v < (1 << 8)) {
      buf.writeByteAndByte(offset, UINT8, v.toByte)
    } else {
      buf.writeByteAndShort(offset, UINT16, v)
    }
  }

  def packInt(buf: Buffer, offset: Int, r: Int): Int = {
    if (r < -(1 << 5)) {
      if (r < -(1 << 15)) {
        buf.writeByteAndInt(offset, INT32, r)
      } else if (r < -(1 << 7)) {
        buf.writeByteAndShort(offset, INT16, r.toShort)
      } else {
        buf.writeByteAndByte(offset, INT8, r.toByte)
      }
    } else if (r < (1 << 7)) {
      buf.writeByte(offset, r.toByte)
    } else if (r < (1 << 8)) {
      buf.writeByteAndByte(offset, UINT8, r.toByte)
    } else if (r < (1 << 16)) {
      buf.writeByteAndShort(offset, UINT16, r.toShort)
    } else { // unsigned 32
      buf.writeByteAndInt(offset, UINT32, r)
    }
  }

  def packLong(buf: Buffer, offset: Int, v: Long): Int = {
    if (v < -(1L << 5)) {
      if (v < -(1L << 15)) {
        if (v < -(1L << 31))
          buf.writeByteAndLong(offset, INT64, v)
        else
          buf.writeByteAndInt(offset, INT32, v.toInt)
      } else if (v < -(1 << 7)) {
        buf.writeByteAndShort(offset, INT16, v.toShort)
      } else {
        buf.writeByteAndByte(offset, INT8, v.toByte)
      }
    } else if (v < (1 << 7)) { // fixnum
      buf.writeByte(offset, v.toByte)
    } else if (v < (1L << 16)) {
      if (v < (1 << 8))
        buf.writeByteAndByte(offset, UINT8, v.toByte)
      else
        buf.writeByteAndShort(offset, UINT16, v.toShort)
    } else if (v < (1L << 32))
      buf.writeByteAndInt(offset, UINT32, v.toInt)
    else
      buf.writeByteAndLong(offset, UINT64, v)
  }

  def packBigInteger(buf: Buffer, offset: Int, bi: BigInteger): Int = {
    if (bi.bitLength <= 63) {
      packLong(buf, offset, bi.longValue)
    } else if (bi.bitLength == 64 && bi.signum == 1) {
      buf.writeByteAndLong(offset, UINT64, bi.longValue)
    } else {
      throw new IllegalArgumentException("MessagePack cannot serialize BigInteger larger than 2^64-1")
    }
  }

  def packFloat(buf: Buffer, offset: Int, v: Float): Int = {
    buf.writeByteAndFloat(offset, FLOAT32, v)
  }

  def packDouble(buf: Buffer, offset: Int, v: Double): Int = {
    buf.writeByteAndDouble(offset, FLOAT64, v)
  }

  def packString(buf: Buffer, offset: Int, s: String): Int = {
    val bytes = s.getBytes(StandardCharsets.UTF_8)
    // Write the length and payload of small string to the buffer so that it avoids an extra flush of buffer
    val len = packRawStringHeader(buf, offset, bytes.length)
    writePayload(buf, offset + len, bytes)
    len + bytes.length
  }

  def packRawStringHeader(buf: Buffer, offset: Int, len: Int): Int = {
    if (len < (1 << 5)) {
      buf.writeByte(offset, (FIXSTR_PREFIX | len).toByte)
    } else if (len < (1 << 8)) {
      buf.writeByteAndByte(offset, STR8, len.toByte)
    } else if (len < (1 << 16)) {
      buf.writeByteAndShort(offset, STR16, len.toShort)
    } else {
      buf.writeByteAndInt(offset, STR32, len)
    }
  }

  def packArrayHeader(buf: Buffer, offset: Int, arraySize: Int): Int = {
    if (arraySize < 0)
      throw new IllegalArgumentException("array size must be >= 0")

    if (arraySize < (1 << 4))
      buf.writeByte(offset, (FIXARRAY_PREFIX | arraySize).toByte)
    else if (arraySize < (1 << 16))
      buf.writeByteAndShort(offset, ARRAY16, arraySize.toShort)
    else
      buf.writeByteAndInt(offset, ARRAY32, arraySize)
  }

  def packMapHeader(buf: Buffer, offset: Int, mapSize: Int): Int = {
    if (mapSize < 0)
      throw new IllegalArgumentException("map size must be >= 0")

    if (mapSize < (1 << 4)) {
      buf.writeByte(offset, (FIXMAP_PREFIX | mapSize).toByte)
    } else if (mapSize < (1 << 16)) {
      buf.writeByteAndShort(offset, MAP16, mapSize.toShort)
    } else {
      buf.writeByteAndInt(offset, MAP32, mapSize)
    }
  }

  def packExtensionTypeHeader(buf: Buffer, offset: Int, extType: Byte, payloadLen: Int): Int = {
    if (payloadLen < (1 << 8)) {
      if (payloadLen > 0 && (payloadLen & (payloadLen - 1)) == 0) { // check whether dataLen == 2^x
        if (payloadLen == 1)
          buf.writeByteAndByte(offset, FIXEXT1, extType)
        else if (payloadLen == 2)
          buf.writeByteAndByte(offset, FIXEXT2, extType)
        else if (payloadLen == 4)
          buf.writeByteAndByte(offset, FIXEXT4, extType)
        else if (payloadLen == 8)
          buf.writeByteAndByte(offset, FIXEXT8, extType)
        else if (payloadLen == 16)
          buf.writeByteAndByte(offset, FIXEXT16, extType)
        else {
          buf.writeByteAndByte(offset, EXT8, payloadLen.toByte)
          buf.writeByte(offset + 2, extType)
          3
        }
      } else {
        buf.writeByteAndByte(offset, EXT8, payloadLen.toByte)
        buf.writeByte(offset + 2, extType)
        3
      }
    } else if (payloadLen < (1 << 16)) {
      buf.writeByteAndShort(offset, EXT16, payloadLen.toShort)
      buf.writeByte(offset + 3, extType)
      4
    } else {
      buf.writeByteAndInt(offset, EXT32, payloadLen)
      buf.writeByte(offset + 5, extType)
      // TODO support dataLen > 2^31 - 1
      6
    }
  }

  def packBinaryHeader(buf: Buffer, offset: Int, len: Int): Int = {
    if (len < (1 << 8)) {
      buf.writeByteAndByte(offset, BIN8, len.toByte)
    } else if (len < (1 << 16)) {
      buf.writeByteAndShort(offset, BIN16, len.toShort)
    } else {
      buf.writeByteAndInt(offset, BIN32, len)
    }
  }

  def writePayload(buf: Buffer, offset: Int, v: Array[Byte]): Int = {
    buf.writeBytes(offset, v)
  }

  def writePayload(buf: Buffer, offset: Int, v: Array[Byte], vOffset: Int, length: Int): Int = {
    buf.writeBytes(offset, v, vOffset, length)
  }

}
