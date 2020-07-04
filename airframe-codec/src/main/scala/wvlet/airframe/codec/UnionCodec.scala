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
import wvlet.airframe.codec.PrimitiveCodec.StringCodec
import wvlet.airframe.msgpack.spi.{Packer, Unpacker}
import wvlet.airframe.surface.Union

/**
  */
object UnionCodec extends MessageCodec[Union] {
  override def pack(p: Packer, v: Union): Unit = {
    val cl = v.getElementClass
    wvlet.airframe.codec.Compat.codecOfClass(cl) match {
      case Some(codec) =>
        info(codec)
        codec.asInstanceOf[MessageCodec[Any]].pack(p, cl.cast(v))
      case None =>
        // Pack as a string
        StringCodec.pack(p, v.toString)
    }
  }
  override def unpack(u: Unpacker, v: MessageContext): Unit = {
    ???
  }
}
