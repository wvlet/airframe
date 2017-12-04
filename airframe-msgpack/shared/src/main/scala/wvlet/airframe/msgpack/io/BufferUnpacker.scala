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

import java.nio.ByteBuffer
import java.util.Locale

import wvlet.airframe.msgpack.spi.ErrorCode.{INSUFFICIENT_BUFFER, INVALID_TYPE, NEVER_USED_FORMAT}
import wvlet.airframe.msgpack.spi.{Code, InsufficientBufferException, MessageFormat, MessagePackException}

/**
  * Read a value from the buffer
  */
object BufferUnpacker {

  def ensureCapacity(buf: ByteBuffer, minimumLength: Int): Unit = {
    if (buf.remaining() < minimumLength) {
      throw InsufficientBufferException(minimumLength)
    }
  }

  private def unexpected(expectedCode: String, actual: Byte) = {
    val f = Code.messageFormatOf(actual)
    if (f == MessageFormat.NEVER_USED) {
      throw new MessagePackException(NEVER_USED_FORMAT, s"Expected ${expectedCode}, but found 0xC1 (NEVER_USED) byte")
    } else {
      val name     = f.getValueType.name()
      val typeName = name.substring(0, 1) + name.substring(1).toLowerCase(Locale.ENGLISH)
      throw new MessagePackException(INVALID_TYPE, f"Expected ${expectedCode}, but got ${typeName} (${actual}%02x)")
    }
  }

  def unpackNil(buf: ByteBuffer) {
    ensureCapacity(buf, 1)
    buf.get()
  }

  def tryUnpackNil(buf: ByteBuffer): Boolean = {
    ensureCapacity(buf, 1)
    val pos = buf.position()
    buf.get() match {
      case Code.NIL =>
        true
      case _ =>
        buf.position(pos)
        false
    }
  }

  def unpackBoolean(buf: ByteBuffer): Boolean = {
    ensureCapacity(buf, 1)
    buf.get() match {
      case Code.FALSE => false
      case Code.TRUE  => true
      case other      => unexpected("boolean", other)
    }
  }

  def unpackByte(buf: ByteBuffer): Byte = {
    ensureCapacity(buf, 1)

  }

}
