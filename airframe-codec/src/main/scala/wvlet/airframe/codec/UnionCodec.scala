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
import wvlet.airframe.surface.{Surface, Union}

/**
  * Codec for union classes (e.g., A or B)
  * This codec is necessary for defining OpenAPI's model classes
  */
case class UnionCodec(codecs: Seq[MessageCodec[_]]) extends MessageCodec[Union] {
  override def pack(p: Packer, v: Union): Unit = {
    val cl = v.getElementClass
    wvlet.airframe.codec.Compat.codecOfClass(cl) match {
      case Some(codec) =>
        codec.asInstanceOf[MessageCodec[Any]].pack(p, v)
      case None =>
        // Pack as a string
        StringCodec.pack(p, v.toString)
    }
  }
  override def unpack(u: Unpacker, v: MessageContext): Unit = {
    // Read the value first
    val msgPack = u.unpackValue.toMsgpack
    // Try each codec
    val found = codecs.find { x =>
      x.unpackMsgPack(msgPack).map { a: Any =>
          v.setObject(a)
        }.isDefined
    }.isDefined
    if (!found) {
      v.setError(
        new MessageCodecException(
          INVALID_DATA,
          this,
          s"No corresponding type is found for data: ${JSONCodec.fromMsgPack(msgPack)}"
        )
      )
    }
  }
}
