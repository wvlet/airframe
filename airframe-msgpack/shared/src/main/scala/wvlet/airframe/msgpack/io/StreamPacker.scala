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

import java.math.BigInteger

import wvlet.airframe.msgpack.spi.{Packer, Value}

/**
  *
  */
class StreamPacker(sink: MessageSink) extends Packer {

  private var position: Int       = 0
  private var buffer: WriteBuffer = null

  private var totalFlushBytes: Long = 0

  private def totalWrittenBytes: Unit = {
    totalFlushBytes + position
  }

  private def ensureBuffer(minimumSize: Int): Unit = {
    if (buffer == null) {
      buffer = sink.next(minimumSize)
    } else if (position + minimumSize >= buffer.size) {
      flushBuffer
      buffer = sink.next(minimumSize)
    }
  }

  private def flushBuffer: Unit = {
    sink.writeBuffer(position)
    totalFlushBytes += position
    buffer = null
    position = 0
  }

  override def packNil = {
    ensureBuffer(1)
    position += OffsetPacker.packNil(buffer, position)
    this
  }

  override def packBoolean(v: Boolean) = {
    ensureBuffer(1)
    position += OffsetPacker.packBoolean(buffer, position, v)
    this
  }

  override def packByte(v: Byte) = {
    ensureBuffer(2)
    position += OffsetPacker.packByte(buffer, position, v)
    this
  }

  override def packShort(v: Short) = {
    ensureBuffer(3)
    position += OffsetPacker.packShort(buffer, position, v)
    this
  }

  override def packInt(v: Int) = {
    ensureBuffer(5)
    position += OffsetPacker.packInt(buffer, position, v)
    this
  }

  override def packLong(v: Long) = {
    ensureBuffer(9)
    position += OffsetPacker.packLong(buffer, position, v)
    this
  }

  override def packBigInteger(v: BigInteger) = {
    ensureBuffer(9)
    position += OffsetPacker.packBigInteger(buffer, position, v)
    this
  }

  override def packFloat(v: Float) = {
    ensureBuffer(5)
    position += OffsetPacker.packFloat(buffer, position, v)
    this
  }

  override def packDouble(v: Double) = {
    ensureBuffer(9)
    position += OffsetPacker.packDouble(buffer, position, v)
    this
  }

  override def packString(v: String) = {
    // ensure capacity for 2-byte raw string header + the maximum string size (+ 1 byte for fallback code)
    ensureBuffer(2 + v.length * StreamPacker.UTF_8_MAX_CHAR_SIZE + 1)
    position += OffsetPacker.packString(buffer, position, v)
    this
  }

  override def packTimestamp(epochSecond: Long, nanoAdjustment: Int) = {
    ensureBuffer(15) // maximum timestamp size
    position += OffsetPacker.packTimestampEpochSecond(buffer, position, epochSecond, nanoAdjustment)
    this
  }

  override def packArrayHeader(arraySize: Int) = {
    ensureBuffer(5)
    position += OffsetPacker.packArrayHeader(buffer, position, arraySize)
    this
  }

  override def packMapHeader(mapSize: Int) = {
    ensureBuffer(5)
    position += OffsetPacker.packMapHeader(buffer, position, mapSize)
    this
  }

  override def packExtensionTypeHeader(extType: Byte, payloadLen: Int) = {
    ensureBuffer(6)
    position += OffsetPacker.packExtTypeHeader(buffer, position, extType, payloadLen)
    this
  }

  override def packBinaryHeader(len: Int) = {
    ensureBuffer(5)
    position += OffsetPacker.packBinaryHeader(buffer, position, len)
    this
  }

  override def packRawStringHeader(len: Int) = {
    ensureBuffer(5)
    position += OffsetPacker.packRawStringHeader(buffer, position, len)
    this
  }

  override def packValue(v: Value) = {
    // TODO create a new buffer packer so that we can preserve the position

    // TODO reuse buffer
    val valuePacker: BufferPacker = null
    v.writeTo(valuePacker)
    val result = valuePacker.toByteArray

    // TODO output the result directly to Sink
    position += OffsetPacker.writePayload(buffer, position, result)
    this
  }

  override def writePayload(src: Array[Byte])                           = ???
  override def writePayload(src: Array[Byte], offset: Int, length: Int) = ???
  override def addPayload(src: Array[Byte])                             = ???
  override def addPayload(src: Array[Byte], offset: Int, length: Int)   = ???
}

object StreamPacker {
  val UTF_8_MAX_CHAR_SIZE = 6
}
