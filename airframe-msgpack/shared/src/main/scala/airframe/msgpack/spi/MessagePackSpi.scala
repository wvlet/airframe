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
package airframe.msgpack.spi

/**
  *
  */
import java.math.BigInteger

sealed trait ValueType

object ValueType {
  case object NIL       extends ValueType
  case object BOOLEAN   extends ValueType
  case object INTEGER   extends ValueType
  case object FLOAT     extends ValueType
  case object STRING    extends ValueType
  case object BINARY    extends ValueType
  case object ARRAY     extends ValueType
  case object MAP       extends ValueType
  case object EXTENSION extends ValueType
}

/**
  * Message Packer interface
  */
trait Packer {
  def packNil: Packer
  def packBoolean(v: Boolean): Packer
  def packByte(v: Byte): Packer
  def packShort(v: Short): Packer
  def packInt(v: Int): Packer
  def packLong(v: Long): Packer
  def packBigInteger(v: BigInteger): Packer
  def packFloat(v: Float): Packer
  def packDouble(v: Double): Packer
  def packString(v: String): Packer
  def packArrayHeader(arraySize: Int): Packer
  def packMapHeader(mapSize: Int): Packer

  def packExtensionTypeHeader(extType: Byte, payloadLen: Int): Packer
  def packBinaryHeader(len: Int): Packer
  def packRawStringHeader(len: Int): Packer

  def writePayload(src: Array[Byte]): Packer
  def writePayload(src: Array[Byte], offset: Int, length: Int): Packer
  def addPayload(src: Array[Byte]): Packer
  def addPayload(src: Array[Byte], offset: Int, length: Int): Packer
}

case class ExtensionTypeHeader(extType: Byte, byteLength: Int)

/**
  * MessageUnpacker interface
  */
trait Unpacker {
  def hasNext: Boolean
  def getNextCode: Int
  def getNextValueType: ValueType

  def skipValue: Unit
  def skipValue(count: Int): Unit

  def unpackNil: Unit
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
  def unpackArrayHeader: Int
  def unpackMapHeader: Int
  def unpackExtensionTypeHeader: ExtensionTypeHeader
  def unpackRawStringHeader: Int
  def unpackBinaryHeader: Int

  def skipPayload(numBytes: Int)
  def readPayload(dst: Array[Byte])
  def readPayload(dst: Array[Byte], offset: Int, length: Int)
  def readPayload(length: Int): Array[Byte]
}
