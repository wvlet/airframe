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
import wvlet.airframe.msgpack.spi.{Packer, Unpacker}
import wvlet.airframe.surface.Surface
import wvlet.airframe.surface.reflect.TypeConverter
import wvlet.log.LogSupport

/**
  * A codec for Enum-like case objects that can be instantiated with unapply(String)
  */
class StringUnapplyCodec[A](codec: Surface) extends MessageCodec[A] with LogSupport {
  override def pack(p: Packer, v: A): Unit = {
    p.packString(v.toString)
  }
  override def unpack(u: Unpacker, v: MessageContext): Unit = {
    val s = u.unpackString
    TypeConverter.convertToCls(s, codec.rawType) match {
      case Some(x) =>
        v.setObject(x)
      case None =>
        v.setNull
    }
  }
}
