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
  def ensureCapacity(index: Int, requestedLength: Int): Unit

  def writeByte(index: Int, v: Byte): Int
  def writeBytes(index: Int, v: Array[Byte]): Int = writeBytes(index, v, 0, v.length)
  def writeBytes(index: Int, v: Array[Byte], vOffset: Int, length: Int): Int

  def writeShort(index: Int, v: Short): Int
  def writeInt(index: Int, v: Int): Int
  def writeLong(index: Int, v: Long): Int

  def writeByteAndByte(index: Int, b: Byte, v: Byte): Int = {
    ensureCapacity(index, 2)
    writeByte(index, b)
    1 + writeByte(index + 1, v)
  }

  def writeByteAndShort(index: Int, b: Byte, v: Short): Int = {
    ensureCapacity(index, 2)
    writeByte(index, b)
    1 + writeShort(index + 1, v)
  }

  def writeByteAndInt(index: Int, b: Byte, v: Int): Int = {
    ensureCapacity(index, 5)
    writeByte(index, b)
    1 + writeInt(index + 1, v)
  }

  def writeByteAndLong(index: Int, b: Byte, v: Long): Int = {
    ensureCapacity(index, 9)
    writeByte(index, b)
    1 + writeLong(index + 1, v)
  }

  def writeByteAndFloat(index: Int, b: Byte, v: Float): Int = {
    writeByteAndInt(index, b, java.lang.Float.floatToRawIntBits(v))
  }

  def writeByteAndDouble(index: Int, b: Byte, v: Double): Int = {
    writeByteAndLong(index, b, java.lang.Double.doubleToRawLongBits(v))
  }
}
