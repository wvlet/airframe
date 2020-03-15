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
package wvlet.airframe.http
import wvlet.airframe.codec.{MessageCodec, MessageContext}
import wvlet.airframe.msgpack.spi.{Packer, Unpacker}

/**
  *
  */
object HttpMultiMapCodec extends MessageCodec[HttpMultiMap] {
  private val codec = MessageCodec.of[Map[String, Any]]

  override def pack(p: Packer, v: HttpMultiMap): Unit = {
    codec.pack(p, v.map)
  }
  override def unpack(
      u: Unpacker,
      v: MessageContext
  ): Unit = {
    codec.unpack(u, v)
    if (!v.isNull) {
      val m = v.getLastValue.asInstanceOf[Map[String, Any]]
      v.setObject(HttpMultiMap(m))
    }
  }
}
