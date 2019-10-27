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

import wvlet.airframe.msgpack.spi.{
  ExtTypeHeader,
  MessageFormat,
  OffsetUnpacker,
  ReadBuffer,
  ReadCursor,
  Unpacker,
  Value,
  ValueType
}

/**
  *
  */
class PureScalaBufferUnpacker(buf: ReadBuffer) extends Unpacker {
  private var cursor = ReadCursor(buf, 0)

  override def hasNext: Boolean = {
    cursor.lastReadLength < buf.size
  }

  override def getNextFormat: MessageFormat = {
    ???
  }
  override def getNextValueType: ValueType = ???
  override def skipValue: Unit             = ???
  override def skipValue(count: Int): Unit = ???
  override def unpackNil: Unit = {
    OffsetUnpacker.unpackNil(cursor)
  }
  override def tryUnpackNil: Boolean = {
    OffsetUnpacker.tryUnpackNil(cursor)
  }
  override def unpackBoolean: Boolean                                        = ???
  override def unpackByte: Byte                                              = ???
  override def unpackShort: Short                                            = ???
  override def unpackInt: Int                                                = ???
  override def unpackLong: Long                                              = ???
  override def unpackBigInteger: BigInteger                                  = ???
  override def unpackFloat: Float                                            = ???
  override def unpackDouble: Double                                          = ???
  override def unpackString: String                                          = ???
  override def unpackTimestamp: Instant                                      = ???
  override def unpackTimestamp(extTypeHeader: ExtTypeHeader): Instant        = ???
  override def unpackArrayHeader: Int                                        = ???
  override def unpackMapHeader: Int                                          = ???
  override def unpackExtTypeHeader: ExtTypeHeader                            = ???
  override def unpackExtValue(extTypeHeader: ExtTypeHeader): Value           = ???
  override def unpackRawStringHeader: Int                                    = ???
  override def unpackBinaryHeader: Int                                       = ???
  override def unpackValue: Value                                            = ???
  override def readPayload(dst: Array[Byte]): Unit                           = ???
  override def readPayload(dst: Array[Byte], offset: Int, length: Int): Unit = ???
  override def readPayload(length: Int): Array[Byte]                         = ???
  override def close(): Unit = {
    // buf.close
  }
}
