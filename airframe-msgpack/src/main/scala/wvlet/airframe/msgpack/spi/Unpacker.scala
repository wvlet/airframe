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

import java.math.BigInteger
import java.time.Instant

/**
  * MessageUnpacker interface
  */
trait Unpacker extends AutoCloseable {
  def hasNext: Boolean
  def getNextFormat: MessageFormat
  def getNextValueType: ValueType

  def skipValue: Unit
  def skipValue(count: Int): Unit

  def unpackNil: Unit

  /**
    * Peeks a Nil byte and read it if the next byte is actually a Nil value, then proceed the cursor 1 byte and return
    * true.
    *
    * If the next byte is not Nil, it will return false and the cursor position will not be changed.
    *
    * @return
    *   true if a nil value is read and the cursor is proceeded 1 bytes. false if the next value is not Nil and the
    *   cursor position will not change.
    */
  def tryUnpackNil: Boolean
  def unpackBoolean: Boolean
  def unpackByte: Byte
  def unpackShort: Short
  def unpackInt: Int
  def unpackLong: Long
  def unpackBigInteger: BigInteger
  def unpackFloat: Float
  def unpackDouble: Double
  def unpackString: String
  def unpackTimestamp: Instant
  def unpackTimestamp(extTypeHeader: ExtTypeHeader): Instant

  def unpackArrayHeader: Int
  def unpackMapHeader: Int
  def unpackExtTypeHeader: ExtTypeHeader
  def unpackExtValue(extTypeHeader: ExtTypeHeader): Value
  def unpackRawStringHeader: Int
  def unpackBinaryHeader: Int

  def unpackValue: Value

  //def skipPayload(numBytes: Int): Unit
  def readPayload(dst: Array[Byte]): Unit
  def readPayload(dst: Array[Byte], offset: Int, length: Int): Unit
  def readPayload(length: Int): Array[Byte]
}

case class ExtTypeHeader(extType: Byte, byteLength: Int)
