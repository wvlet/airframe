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

import wvlet.airframe.msgpack.spi.{InsufficientBufferException, MessageException}

object ArrayBuffer {
  def apply(a: Array[Byte]): ArrayBuffer = ArrayBuffer(a, 0, a.length)

  def fromArray(a: Array[Byte], offset: Int, size: Int): ArrayBuffer = {
    require(offset + size <= a.length, s"input array is smaller than offset:${offset} + size:${size}: ${a.length}")
    ArrayBuffer(a, offset, size)
  }
}

/**
  *
  */
case class ArrayBuffer(a: Array[Byte], offset: Int, size: Int) extends InputBuffer with OutputBuffer {
  require(offset > 0, s"baseOffset ${offset} < 0")
  require(offset + size <= a.length, s"insufficient buffer size baseOffset:${offset} + size:${size} <= array size:${a.length}")

  override def slice(position: Int, newSize: Int): InputBuffer = {
    require(position + newSize <= size, s"Insufficient array length (${a.length}, offset:${offset}, size:${size}) for slice(${position}, ${newSize})")
    ArrayBuffer(a, offset + position, newSize)
  }

  def ensureCapacity(position: Int, requestedLength: Int): Unit = {
    if (position + requestedLength < size) {
      throw new InsufficientBufferException(position, requestedLength)
    }
  }

  override def readByte(position: Int): Byte = {
    ensureCapacity(position, 1)
    val pos = offset + position
    a(pos)
  }

  override def readShort(position: Int): Short = {
    ensureCapacity(position, 2)
    val pos = offset + position
    ((a(pos) << 8) | (a(pos + 1) & 0xFF)).toShort
  }

  override def readInt(position: Int): Int = {
    ensureCapacity(position, 4)
    val pos = offset + position
    ((a(pos).toInt << 24) |
      ((a(pos + 1) & 0xFF) << 16) |
      ((a(pos + 2) & 0xFF) << 8) |
      (a(pos + 3) & 0xFF))
  }

  override def readLong(position: Int): Long = {
    ensureCapacity(position, 8)
    val pos = offset + position
    ((a(pos).toLong << 56) |
      ((a(pos + 1).toLong & 0xFF) << 48) |
      ((a(pos + 2).toLong & 0xFF) << 40) |
      ((a(pos + 3).toLong & 0xFF) << 32) |
      ((a(pos + 4).toLong & 0xFF) << 24) |
      ((a(pos + 5).toLong & 0xFF) << 16) |
      ((a(pos + 6).toLong & 0xFF) << 8) |
      (a(pos + 7).toLong & 0xFF))
  }

  override def readBytes(position: Int, length: Int) = {
    ensureCapacity(position, length)
    val dest = new Array[Byte](length)
    Array.copy(a, offset + position, dest, 0, length)
    dest
  }

  override def readBytes(position: Int, length: Int, dest: Array[Byte], destOffset: Int): Unit = {
    ensureCapacity(position, length)
    Array.copy(a, offset + position, dest, destOffset, length)
  }

  override def readBytes(position: Int, length: Int, dest: OutputBuffer, destIndex: Int): Unit = {
    ensureCapacity(position, length)
    dest.writeBytes(destIndex, a, offset + position, length)
  }

  def writeByte(position: Int, v: Byte): Int = {
    ensureCapacity(position, 1)
    val pos = offset + position
    a(pos) = v
    1
  }

  def writeBytes(position: Int, source: Array[Byte], sourceOffset: Int, length: Int): Int = {
    require(source != null, "source is null")
    require(sourceOffset + length <= source.length, s"Insufficient input buffer size ${source.length} for reading ${sourceOffset}+${length} bytes")

    ensureCapacity(position, length)
    Array.copy(source, sourceOffset, a, offset + position, length)
    length
  }

  override def writeBytes(position: Int, src: InputBuffer, srcPosition: Int, length: Int): Int = {
    require(src != null, "source is nul")
    ensureCapacity(position, length)
    src.readBytes(srcPosition, length, a, offset + position)
    length
  }

  def writeShort(position: Int, v: Short): Int = {
    ensureCapacity(position, 2)
    val pos = offset + position
    // Use big-endian order
    a(pos) = ((v & 0xFF00) >> 8).toByte
    a(pos + 1) = (v & 0xFF).toByte
    2
  }

  def writeInt(position: Int, v: Int): Int = {
    ensureCapacity(position, 4)
    val pos = offset + position
    // Use big-endian order
    a(pos) = ((v & 0xFF000000) >> 24).toByte
    a(pos + 1) = ((v & 0xFF0000) >> 16).toByte
    a(pos + 2) = ((v & 0xFF00) >> 8).toByte
    a(pos + 3) = (v & 0xFF).toByte
    4
  }

  def writeLong(position: Int, v: Long): Int = {
    ensureCapacity(position, 8)
    val pos = offset + position
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
