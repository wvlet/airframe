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
  * Mutable cursor for Unpacker.
  * @param buf
  * @param position
  */
case class ReadCursor(var buf: ReadBuffer, var position: Int) {
  private var offset: Int = 0

  def lastReadLength: Int = offset

  def reverseCursor: Unit = {
    offset -= 1
  }

  def proceedCursor: Int = {
    val diff = offset
    this.position += diff
    offset = 0
    diff
  }

  def skipBytes(n: Int): Unit = {
    offset += n
  }

  def peekByte: Byte = {
    buf.readByte(position + offset)
  }

  def readByte: Byte = {
    val v = buf.readByte(position + offset)
    offset += 1
    v
  }

  def readShort: Short = {
    val v = buf.readShort(position + offset)
    offset += 2
    v
  }

  def readInt: Int = {
    val v = buf.readInt(position + offset)
    offset += 4
    v
  }

  def readLong: Long = {
    val v = buf.readLong(position + offset)
    offset += 8
    v
  }

  def readFloat: Float = {
    val v = buf.readFloat(position + offset)
    offset += 4
    v
  }

  def readDouble: Double = {
    val v = buf.readDouble(position + offset)
    offset += 8
    v
  }

  def readBytes(len: Int): Array[Byte] = {
    val v = buf.readBytes(position + offset, len)
    offset += len
    v
  }

  def readBytes(len: Int, dest: Array[Byte], destOffset: Int): Unit = {
    val v = buf.readBytes(position + offset, len, dest, destOffset)
    offset += len
    v
  }

  def readBytes(len: Int, dest: WriteBuffer, destOffset: Int): Unit = {
    buf.readBytes(position + offset, len, dest, destOffset)
    offset += len
  }
}
