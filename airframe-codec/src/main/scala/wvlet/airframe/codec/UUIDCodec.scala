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
package wvlet.airframe.codec
import java.util.UUID

import wvlet.airframe.msgpack.spi.{Packer, Unpacker, ValueType}

/**
  */
object UUIDCodec extends MessageCodec[UUID] {

  override def pack(p: Packer, v: UUID): Unit = {
    p.packString(v.toString)
  }
  override def unpack(
      u: Unpacker,
      v: MessageContext
  ): Unit = {
    u.getNextValueType match {
      case ValueType.NIL =>
        u.unpackNil
        v.setNull
      case ValueType.STRING =>
        val s = u.unpackString
        try {
          v.setObject(UUID.fromString(s))
        } catch {
          case e: IllegalArgumentException =>
            v.setError(e)
        }
      case ValueType.BINARY =>
        val len  = u.unpackBinaryHeader
        val data = u.readPayload(len)
        try {
          v.setObject(Compat.readUUIDFromBytes(data))
        } catch {
          case e: Throwable => v.setError(e)
        }
      case _ =>
        u.skipValue
        v.setNull
    }
  }
}
