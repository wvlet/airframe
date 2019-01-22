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
abstract class Canvas {

  def readByte(offset: Long): Byte
  def readBoolean(offset: Long): Boolean
  def readShort(offset: Long): Short
  def readInt(offset: Long): Int
  def readLong(offset: Long): Long
  def readFloat(offset: Long): Float
  def readDouble(offset: Long): Double

  def readBytes(offset: Long, length: Long, dest: Canvas, destOffset: Long): Unit
  def readBytes(offset: Long, length: Long, dest: Array[Byte], destOffset: Int): Unit
  def readBytes(offset: Long, length: Long): Array[Byte] = {
    require(length.isValidInt, s"read length must be less than ${Int.MaxValue}")
    val b = new Array[Byte](length.toInt)
    readBytes(offset, length, b, 0)
    b
  }

  def writeByte(offset: Long, v: Byte): Unit
  def writeBoolean(offset: Long, v: Boolean): Unit
  def writeShort(offset: Long, v: Short): Unit
  def writeInt(offset: Long, v: Int): Unit
  def writeLong(offset: Long, v: Long): Unit
  def writeFloat(offset: Long, v: Float): Unit
  def writeDouble(offset: Long, v: Double): Unit

  def writeBytes(offset: Long, src: Array[Byte], srcOffset: Int, length: Int)
  def writeBytes(offset: Long, src: Canvas, srcOffset: Long, length: Long)
}

object Canvas {

  def newCanvas(size: Long): Canvas        = ???
  def newOffHeapCanvas(size: Long): Canvas = ???

  def wrappedCanvas(arr: Array[Byte], offset: Int, length: Int): Canvas = ???
  def wrappedCanvas(buf: ByteBuffer): Canvas                            = ???
}
