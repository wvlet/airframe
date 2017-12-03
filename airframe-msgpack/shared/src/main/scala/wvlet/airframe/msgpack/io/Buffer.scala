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

import wvlet.airframe.msgpack.spi.ErrorCode.INSUFFICIENT_BUFFER
import wvlet.airframe.msgpack.spi.MessagePackException

/**
  * Write data a given position and return the written byte length
  */
trait Buffer {
  @throws[MessagePackException]
  def ensureCapacity(offset: Int, requestedLength: Int): Unit

  def writeByte(offset: Int, v: Byte): Int
  def writeBytes(offset: Int, v: Array[Byte]): Int = writeBytes(offset, v, 0, v.length)
  def writeBytes(offset: Int, v: Array[Byte], vOffset: Int, length: Int): Int

  def writeShort(offset: Int, v: Short): Int
  def writeInt(offset: Int, v: Int): Int
  def writeLong(offset: Int, v: Long): Int

  def writeByteAndByte(offset: Int, b: Byte, v: Byte): Int = {
    ensureCapacity(offset, 2)
    writeByte(offset, b)
    1 + writeByte(offset + 1, v)
  }

  def writeByteAndShort(offset: Int, b: Byte, v: Short): Int = {
    ensureCapacity(offset, 2)
    writeByte(offset, b)
    1 + writeShort(offset + 1, v)
  }

  def writeByteAndInt(offset: Int, b: Byte, v: Int): Int = {
    ensureCapacity(offset, 5)
    writeByte(offset, b)
    1 + writeInt(offset + 1, v)
  }

  def writeByteAndLong(offset: Int, b: Byte, v: Long): Int = {
    ensureCapacity(offset, 9)
    writeByte(offset, b)
    1 + writeLong(offset + 1, v)
  }

  def writeByteAndFloat(offset: Int, b: Byte, v: Float): Int = {
    writeByteAndInt(offset, b, java.lang.Float.floatToRawIntBits(v))
  }

  def writeByteAndDouble(offset: Int, b: Byte, v: Double): Int = {
    writeByteAndLong(offset, b, java.lang.Double.doubleToRawLongBits(v))
  }
}

class ArrayBuffer(a: Array[Byte], baseOffset: Int, size: Int) extends Buffer {
  require(baseOffset > 0, s"baseOffset ${baseOffset} < 0")
  require(baseOffset + size <= a.length, s"insufficient buffer size baseOffset:${baseOffset} + size:${size} <= array size:${a.length}")

  def ensureCapacity(offset: Int, requestedLength: Int): Unit = {
    if (offset < size) {
      throw new MessagePackException(INSUFFICIENT_BUFFER, "")
    }
  }

  def writeByte(offset: Int, v: Byte): Int = {
    ensureCapacity(offset, 1)
    val pos = baseOffset + offset
    a(pos) = v
    1
  }

  def writeBytes(offset: Int, v: Array[Byte], vOffset: Int, length: Int): Int = {
    ensureCapacity(offset, length)
    Array.copy(v, vOffset, a, baseOffset + offset, length)
    length
  }

  def writeShort(offset: Int, v: Short): Int = {
    ensureCapacity(offset, 2)
    val pos = baseOffset + offset
    // Use big-endian order
    a(pos) = ((v & 0xFF00) >> 8).toByte
    a(pos + 1) = (v & 0xFF).toByte
    2
  }

  def writeInt(offset: Int, v: Int): Int = {
    ensureCapacity(offset, 4)
    val pos = baseOffset + offset
    // Use big-endian order
    a(pos) = ((v & 0xFF000000) >> 24).toByte
    a(pos + 1) = ((v & 0xFF0000) >> 16).toByte
    a(pos + 2) = ((v & 0xFF00) >> 8).toByte
    a(pos + 3) = (v & 0xFF).toByte
    4
  }

  def writeLong(offset: Int, v: Long): Int = {
    ensureCapacity(offset, 8)
    val pos = baseOffset + offset
    // Use big-endian order
    a(pos) = ((v & 0xFF00000000000000L) >> 56).toByte
    a(pos + 1) = ((v & 0xFF000000000000L) >> 48).toByte
    a(pos + 2) = ((v & 0xFF0000000000L) >> 40).toByte
    a(pos + 3) = ((v & 0xFF00000000L) >> 32).toByte
    a(pos + 4) = ((v & 0xFF000000L) >> 24).toByte
    a(pos + 5) = ((v & 0xFF0000L) >> 16).toByte
    a(pos + 6) = ((v & 0xFF00L) >> 8).toByte
    a(pos + 7) = (v & 0xFFL).toByte
    8
  }
}
