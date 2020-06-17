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
import java.time.Instant

import wvlet.airframe.msgpack.spi._

/**
  */
class PureScalaBufferUnpacker(buf: ReadBuffer) extends Unpacker {
  private var cursor = ReadCursor(buf, 0)

  override def hasNext: Boolean = {
    cursor.lastReadLength < buf.size
  }

  override def getNextFormat: MessageFormat = {
    OffsetUnpacker.peekNextFormat(cursor)
  }

  override def getNextValueType: ValueType = {
    getNextFormat.valueType
  }
  override def skipValue: Unit = {
    OffsetUnpacker.skipValue(cursor)
  }
  override def skipValue(count: Int): Unit = {
    OffsetUnpacker.skipPayload(cursor, count)
  }
  override def unpackNil: Unit = {
    OffsetUnpacker.unpackNil(cursor)
  }
  override def tryUnpackNil: Boolean = {
    OffsetUnpacker.tryUnpackNil(cursor)
  }
  override def unpackBoolean: Boolean = {
    OffsetUnpacker.unpackBoolean(cursor)
  }
  override def unpackByte: Byte = {
    OffsetUnpacker.unpackByte(cursor)
  }
  override def unpackShort: Short = {
    OffsetUnpacker.unpackShort(cursor)
  }
  override def unpackInt: Int = {
    OffsetUnpacker.unpackInt(cursor)
  }
  override def unpackLong: Long = {
    OffsetUnpacker.unpackLong(cursor)
  }
  override def unpackBigInteger: BigInteger = {
    OffsetUnpacker.unpackBigInteger(cursor)
  }
  override def unpackFloat: Float = {
    OffsetUnpacker.unpackFloat(cursor)
  }
  override def unpackDouble: Double = {
    OffsetUnpacker.unpackDouble(cursor)
  }
  override def unpackString: String = {
    OffsetUnpacker.unpackString(cursor)
  }
  override def unpackTimestamp: Instant = {
    OffsetUnpacker.unpackTimestamp(cursor)
  }
  override def unpackTimestamp(extTypeHeader: ExtTypeHeader): Instant = {
    OffsetUnpacker.unpackTimestamp(extTypeHeader, cursor)
  }
  override def unpackArrayHeader: Int = {
    OffsetUnpacker.unpackArrayHeader(cursor)
  }
  override def unpackMapHeader: Int = {
    OffsetUnpacker.unpackMapHeader(cursor)
  }
  override def unpackExtTypeHeader: ExtTypeHeader = {
    OffsetUnpacker.unpackExtTypeHeader(cursor)
  }
  override def unpackExtValue(extTypeHeader: ExtTypeHeader): Value = {
    OffsetUnpacker.unpackExt(extTypeHeader, cursor)
  }
  override def unpackRawStringHeader: Int = {
    OffsetUnpacker.unpackRawStringHeader(cursor)
  }
  override def unpackBinaryHeader: Int = {
    OffsetUnpacker.unpackBinaryHeader(cursor)
  }
  override def unpackValue: Value = {
    OffsetUnpacker.unpackValue(cursor)
  }
  override def readPayload(dst: Array[Byte]): Unit = {
    OffsetUnpacker.readPayload(cursor, dst.length, dst, 0)
  }
  override def readPayload(dst: Array[Byte], offset: Int, length: Int): Unit = {
    OffsetUnpacker.readPayload(cursor, length, dst, offset)
  }
  override def readPayload(length: Int): Array[Byte] = {
    OffsetUnpacker.readPayload(cursor, length)
  }
  override def close(): Unit = {
    // buf.close
  }
}
