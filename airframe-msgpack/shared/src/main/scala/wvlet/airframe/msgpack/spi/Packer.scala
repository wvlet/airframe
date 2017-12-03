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

/**
  *
  */
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

  def packValue(v: Value): Packer

  def writePayload(src: Array[Byte]): Packer
  def writePayload(src: Array[Byte], offset: Int, length: Int): Packer
  def addPayload(src: Array[Byte]): Packer
  def addPayload(src: Array[Byte], offset: Int, length: Int): Packer
}
