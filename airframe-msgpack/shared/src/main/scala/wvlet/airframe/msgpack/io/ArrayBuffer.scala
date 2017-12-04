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
import wvlet.airframe.msgpack.spi.{InsufficientBufferException, MessageException}

/**
  *
  */
class ArrayBuffer(a: Array[Byte], offset: Int, val size: Int) extends Buffer {
  require(offset > 0, s"baseOffset ${offset} < 0")
  require(offset + size <= a.length, s"insufficient buffer size baseOffset:${offset} + size:${size} <= array size:${a.length}")

  def ensureCapacity(index: Int, requestedLength: Int): Unit = {
    if (index + requestedLength < size) {
      throw new InsufficientBufferException(requestedLength)
    }
  }

  override def readByte(index: Int): Byte = {
    ensureCapacity(index, 1)
    val pos = offset + index
    a(pos)
  }

  override def readShort(index: Int): Short = {
    ensureCapacity(index, 2)
    val pos = offset + index
    ((a(pos) << 8) | (a(pos + 1) & 0xFF)).toShort
  }

  override def readInt(index: Int): Int = {
    ensureCapacity(index, 4)
    val pos = offset + index
    ((a(pos).toInt << 24) |
      ((a(pos + 1) & 0xFF) << 16) |
      ((a(pos + 2) & 0xFF) << 8) |
      (a(pos + 3) & 0xFF))
  }

  override def readLong(index: Int): Long = {
    ensureCapacity(index, 8)
    val pos = offset + index
    ((a(pos).toLong << 56) |
      ((a(pos + 1).toLong & 0xFF) << 48) |
      ((a(pos + 2).toLong & 0xFF) << 40) |
      ((a(pos + 3).toLong & 0xFF) << 32) |
      ((a(pos + 4).toLong & 0xFF) << 24) |
      ((a(pos + 5).toLong & 0xFF) << 16) |
      ((a(pos + 6).toLong & 0xFF) << 8) |
      (a(pos + 7).toLong & 0xFF))
  }

  override def readBytes(index: Int, length: Int) = {
    ensureCapacity(index, length)
    val dest = new Array[Byte](length)
    Array.copy(a, offset + index, dest, 0, length)
    dest
  }

  def writeByte(index: Int, v: Byte): Int = {
    ensureCapacity(index, 1)
    val pos = offset + index
    a(pos) = v
    1
  }

  def writeBytes(index: Int, v: Array[Byte], vOffset: Int, length: Int): Int = {
    ensureCapacity(index, length)
    Array.copy(v, vOffset, a, offset + index, length)
    length
  }

  def writeShort(index: Int, v: Short): Int = {
    ensureCapacity(index, 2)
    val pos = offset + index
    // Use big-endian order
    a(pos) = ((v & 0xFF00) >> 8).toByte
    a(pos + 1) = (v & 0xFF).toByte
    2
  }

  def writeInt(index: Int, v: Int): Int = {
    ensureCapacity(index, 4)
    val pos = offset + index
    // Use big-endian order
    a(pos) = ((v & 0xFF000000) >> 24).toByte
    a(pos + 1) = ((v & 0xFF0000) >> 16).toByte
    a(pos + 2) = ((v & 0xFF00) >> 8).toByte
    a(pos + 3) = (v & 0xFF).toByte
    4
  }

  def writeLong(index: Int, v: Long): Int = {
    ensureCapacity(index, 8)
    val pos = offset + index
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
