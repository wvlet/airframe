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
  * Message Packer interface
  */
trait Packer {
  def packNil: this.type
  def packBoolean(v: Boolean): this.type
  def packByte(v: Byte): this.type
  def packShort(v: Short): this.type
  def packInt(v: Int): this.type
  def packLong(v: Long): this.type
  def packBigInteger(v: BigInteger): this.type
  def packFloat(v: Float): this.type
  def packDouble(v: Double): this.type
  def packString(v: String): this.type
  def packTimestamp(v: Instant): this.type = packTimestamp(v.getEpochSecond, v.getNano)
  def packTimestamp(epochSecond: Long, nanoAdjustment: Int): this.type

  def packArrayHeader(arraySize: Int): this.type
  def packMapHeader(mapSize: Int): this.type

  def packExtensionTypeHeader(extType: Byte, payloadLen: Int): this.type
  def packExtensionTypeHeader(extensionTypeHeader: ExtTypeHeader): this.type = packExtensionTypeHeader(extensionTypeHeader.extType, extensionTypeHeader.byteLength)
  def packBinaryHeader(len: Int): this.type
  def packRawStringHeader(len: Int): this.type

  def packValue(v: Value): this.type

  def writePayload(src: Array[Byte]): this.type
  def writePayload(src: Array[Byte], offset: Int, length: Int): this.type
  def addPayload(src: Array[Byte]): this.type
  def addPayload(src: Array[Byte], offset: Int, length: Int): this.type
}
