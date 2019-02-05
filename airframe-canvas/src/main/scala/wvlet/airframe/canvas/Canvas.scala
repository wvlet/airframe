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
package wvlet.airframe.canvas
import java.nio.ByteBuffer

/**
  * Canvas is an abstraction over large memory (heap or off-heap memory) buffer.
  */
abstract class Canvas extends AutoCloseable {
  def size: Long

  def readByte(offset: Long): Byte
  def readBoolean(offset: Long): Boolean
  def readShort(offset: Long): Short
  def readInt(offset: Long): Int
  def readIntBigEndian(offset: Long): Int
  def readLong(offset: Long): Long
  def readLongBigEndian(offset: Long): Long
  def readFloat(offset: Long): Float
  def readDouble(offset: Long): Double

  def readBytes(offset: Long, dest: Canvas, destOffset: Long, length: Long): Unit
  def readBytes(offset: Long, dest: Array[Byte], destOffset: Int, length: Int): Unit
  def readBytes(offset: Long, length: Long): Array[Byte] = {
    require(length.isValidInt, s"read length must be less than ${Int.MaxValue}")
    val len = length.toInt
    val b   = new Array[Byte](len)
    readBytes(offset, b, 0, len)
    b
  }
  def writeByte(offset: Long, v: Byte): Unit
  def writeBoolean(offset: Long, v: Boolean): Unit
  def writeShort(offset: Long, v: Short): Unit
  def writeInt(offset: Long, v: Int): Unit
  def writeIntBigEndian(offset: Long, v: Int): Unit
  def writeLong(offset: Long, v: Long): Unit
  def writeLongBigEndian(offset: Long, v: Long): Unit
  def writeFloat(offset: Long, v: Float): Unit
  def writeDouble(offset: Long, v: Double): Unit

  def writeBytes(offset: Long, src: Array[Byte]): Unit = {
    writeBytes(offset, src, 0, src.length)
  }
  def writeBytes(offset: Long, src: Array[Byte], srcOffset: Int, length: Int): Unit
  def writeBytes(offset: Long, src: Canvas, srcOffset: Long, length: Long): Unit

  def close(): Unit = release
  def release: Unit

  def slice(offset: Long, length: Long): Canvas
  def toByteArray: Array[Byte]
}

object Canvas {

  private[canvas] val defaultCanvasAllocator = new OffHeapMemoryAllocator

  /**
    * Create a new canvas backed by a heap byte array
    * @param size
    */
  def newCanvas(size: Int): Canvas = {
    wrap(new Array[Byte](size))
  }

  def newOffHeapCanvas(size: Long): Canvas = {
    val m = defaultCanvasAllocator.allocate(size)
    new UnsafeCanvas(null, m.address, m.size, m)
  }

  def wrap(arr: Array[Byte]): Canvas                           = wrap(arr, 0, arr.length)
  def wrap(arr: Array[Byte], offset: Int, length: Int): Canvas = UnsafeCanvas.wrap(arr, offset, length)
  def wrap(buf: ByteBuffer): Canvas                            = UnsafeCanvas.wrap(buf)
}
