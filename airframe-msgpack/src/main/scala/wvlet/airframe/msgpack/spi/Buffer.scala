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

/**
  * Buffer interface, which does not have any internal cursors unlike ByteBuffer of Java library.
  *
  * - The read methods read the buffer data from the given position.
  * - The write methods write data to the specified position in the buffer and return the written byte length.
  *
  * If the buffer capacity is insufficient, these read/write methods throw an [[InsufficientBufferException]].
  * If this exception is thrown, the user code should feed more data, and then resume reading.
  *
  */
trait Buffer {

  def size: Int

  /**
    * Return a (shallow) copy of the buffer.
    *
    * @param position
    * @param size
    * @return
    */
  def slice(position: Int, size: Int): ReadBuffer

  def hasCapacity(position: Int, requestedLength: Int): Boolean

  @throws[InsufficientBufferException]
  def ensureCapacity(position: Int, requestedLength: Int): Unit
}

import java.{lang => jl}

trait ReadBuffer extends Buffer {

  def readByte(position: Int): Byte
  def readShort(position: Int): Short
  def readInt(position: Int): Int
  def readLong(position: Int): Long
  def readFloat(position: Int): Float   = jl.Float.intBitsToFloat(readInt(position))
  def readDouble(position: Int): Double = jl.Double.longBitsToDouble(readLong(position))
  def readBytes(position: Int, length: Int): Array[Byte]
  def readBytes(position: Int, length: Int, dest: Array[Byte], destOffset: Int): Unit
  def readBytes(position: Int, length: Int, dest: WriteBuffer, destIndex: Int): Unit
}

trait WriteBuffer extends Buffer {
  def writeByte(position: Int, v: Byte): Int
  def writeShort(position: Int, v: Short): Int
  def writeInt(position: Int, v: Int): Int
  def writeLong(position: Int, v: Long): Int
  def writeFloat(position: Int, v: Float): Int   = writeInt(position, Compat.floatToIntBits(v))
  def writeDouble(position: Int, v: Double): Int = writeLong(position, Compat.doubleToLongBits(v))

  def writeBytes(position: Int, src: Array[Byte]): Int = writeBytes(position, src, 0, src.length)
  def writeBytes(position: Int, src: Array[Byte], srcOffset: Int, length: Int): Int
  def writeBytes(position: Int, src: ReadBuffer, srcPosition: Int, lenght: Int): Int

  def writeByteAndByte(position: Int, b: Byte, v: Byte): Int = {
    ensureCapacity(position, 2)
    writeByte(position, b)
    1 + writeByte(position + 1, v)
  }

  def writeByteAndShort(position: Int, b: Byte, v: Short): Int = {
    ensureCapacity(position, 2)
    writeByte(position, b)
    1 + writeShort(position + 1, v)
  }

  def writeByteAndInt(position: Int, b: Byte, v: Int): Int = {
    ensureCapacity(position, 5)
    writeByte(position, b)
    1 + writeInt(position + 1, v)
  }

  def writeByteAndLong(position: Int, b: Byte, v: Long): Int = {
    ensureCapacity(position, 9)
    writeByte(position, b)
    1 + writeLong(position + 1, v)
  }

  def writeByteAndFloat(position: Int, b: Byte, v: Float): Int = {
    writeByteAndInt(position, b, Compat.floatToIntBits(v))
  }

  def writeByteAndDouble(position: Int, b: Byte, v: Double): Int = {
    writeByteAndLong(position, b, Compat.doubleToLongBits(v))
  }
}
