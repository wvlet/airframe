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

import wvlet.airframe.msgpack.spi.{Packer, Unpacker, ValueType}
import wvlet.airframe.ulid.ULID

/**
  */
object ULIDCodec extends MessageCodec[ULID] {
  override def pack(p: Packer, v: ULID): Unit = {
    p.packString(v.toString)
  }
  override def unpack(
      u: Unpacker,
      v: MessageContext
  ): Unit = {
    u.getNextValueType match {
      case ValueType.STRING =>
        val s = u.unpackString
        try {
          v.setObject(ULID.fromString(s))
        } catch {
          case e: IllegalArgumentException =>
            v.setError(e)
        }
      case _ =>
        u.skipValue
        v.setNull
    }
  }
}
