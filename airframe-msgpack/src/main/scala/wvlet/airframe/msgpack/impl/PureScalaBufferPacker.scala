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
package wvlet.airframe.msgpack.impl

import java.math.BigInteger

import wvlet.airframe.msgpack.io.{ByteArrayBuffer, ByteArrayBufferBase, InfiniteByteArrayBuffer}
import wvlet.airframe.msgpack.spi.{BufferPacker, OffsetPacker, Packer, Value, WriteCursor}

/**
  */
class PureScalaBufferPacker extends BufferPacker {
  private val buf    = new InfiniteByteArrayBuffer
  private var cursor = WriteCursor(buf, 0)

  override def toByteArray: Array[Byte] = {
    buf.toByteArray(0, cursor.lastWrittenBytes)
  }
  override def clear: Unit = {
    cursor = WriteCursor(buf, 0)
  }
  override private[msgpack] def totalByteSize: Long = {
    cursor.position + cursor.lastWrittenBytes
  }
  override def packNil: this.type = {
    OffsetPacker.packNil(cursor)
    this
  }
  override def packBoolean(v: Boolean): this.type = {
    OffsetPacker.packBoolean(cursor, v)
    this
  }
  override def packByte(v: Byte): this.type = {
    OffsetPacker.packByte(cursor, v)
    this
  }
  override def packShort(v: Short): this.type = {
    OffsetPacker.packShort(cursor, v)
    this
  }
  override def packInt(v: Int): this.type = {
    OffsetPacker.packInt(cursor, v)
    this
  }
  override def packLong(v: Long): this.type = {
    OffsetPacker.packLong(cursor, v)
    this
  }
  override def packBigInteger(v: BigInteger): this.type = {
    OffsetPacker.packBigInteger(cursor, v)
    this
  }
  override def packFloat(v: Float): this.type = {
    OffsetPacker.packFloat(cursor, v)
    this
  }
  override def packDouble(v: Double): this.type = {
    OffsetPacker.packDouble(cursor, v)
    this
  }
  override def packString(v: String): this.type = {
    OffsetPacker.packString(cursor, v)
    this
  }
  override def packTimestamp(epochSecond: Long, nanoAdjustment: Int): this.type = {
    OffsetPacker.packTimestampEpochSecond(cursor, epochSecond, nanoAdjustment)
    this
  }
  override def packArrayHeader(arraySize: Int): this.type = {
    OffsetPacker.packArrayHeader(cursor, arraySize)
    this
  }
  override def packMapHeader(mapSize: Int): this.type = {
    OffsetPacker.packMapHeader(cursor, mapSize)
    this
  }
  override def packExtensionTypeHeader(extType: Byte, payloadLen: Int): this.type = {
    OffsetPacker.packExtTypeHeader(cursor, extType, payloadLen)
    this
  }
  override def packBinaryHeader(len: Int): this.type = {
    OffsetPacker.packBinaryHeader(cursor, len)
    this
  }
  override def packRawStringHeader(len: Int): this.type = {
    OffsetPacker.packRawStringHeader(cursor, len)
    this
  }
  override def packValue(v: Value): this.type = {
    OffsetPacker.packValue(cursor, v)
    this
  }
  override def writePayload(src: Array[Byte]): this.type = {
    OffsetPacker.writePayload(cursor, src)
    this
  }
  override def writePayload(src: Array[Byte], offset: Int, length: Int): this.type = {
    OffsetPacker.writePayload(cursor, src, offset, length)
    this
  }
  override def addPayload(src: Array[Byte]): this.type = {
    // TODO optimization
    writePayload(src)
  }
  override def addPayload(src: Array[Byte], offset: Int, length: Int): this.type = {
    // TODO optimization
    writePayload(src, offset, length)
  }
  override def close(): Unit = {}
}
