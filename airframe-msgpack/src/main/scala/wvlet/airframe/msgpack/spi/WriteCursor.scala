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
  */
case class WriteCursor(var buf: WriteBuffer, var position: Int) {
  private var offset: Int = 0

  def setOffset(offset: Int): Unit = {
    this.offset = offset
  }

  def lastWrittenBytes: Int = offset

  def ensureCapacity(size: Int): Unit = {
    buf.ensureCapacity(position + offset, size)
  }

  def writeByte(b: Byte): Unit = {
    offset += buf.writeByte(position + offset, b)
  }

  def writeByteAndByte(a: Byte, b: Byte): Unit = {
    offset += buf.writeByteAndByte(position + offset, a, b)
  }

  def writeByteAndShort(a: Byte, b: Short): Unit = {
    offset += buf.writeByteAndShort(position + offset, a, b)
  }

  def writeByteAndInt(a: Byte, b: Int): Unit = {
    offset += buf.writeByteAndInt(position + offset, a, b)
  }

  def writeByteAndLong(a: Byte, b: Long): Unit = {
    offset += buf.writeByteAndLong(position + offset, a, b)
  }

  def writeByteAndFloat(a: Byte, b: Float): Unit = {
    offset += buf.writeByteAndFloat(position + offset, a, b)
  }
  def writeByteAndDouble(a: Byte, b: Double): Unit = {
    offset += buf.writeByteAndDouble(position + offset, a, b)
  }

  def writeInt(a: Int): Unit = {
    offset += buf.writeInt(position + offset, a)
  }
  def writeLong(a: Long): Unit = {
    offset += buf.writeLong(position + offset, a)
  }

  def writeBytes(a: Array[Byte]): Unit = {
    offset += buf.writeBytes(position + offset, a)
  }
  def writeBytes(src: Array[Byte], srcOffset: Int, length: Int): Unit = {
    offset += buf.writeBytes(position + offset, src, srcOffset, length)
  }
}
