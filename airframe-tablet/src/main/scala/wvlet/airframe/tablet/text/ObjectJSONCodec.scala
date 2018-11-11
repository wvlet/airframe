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
package wvlet.airframe.tablet.text

import wvlet.airframe.codec.{JSONCodec, MessageCodec, MessageHolder, ObjectCodec}
import wvlet.airframe.msgpack.spi.MessagePack
import wvlet.airframe.tablet.MessagePackRecord
import wvlet.airframe.{msgpack, surface}

import scala.reflect.runtime.{universe => ru}

/**
  * Codec for Object -> JSON String -> Object
  */
case class ObjectJSONCodec[A](codec: ObjectCodec[A]) {

  def toJSON(v: A): String = {
    val packer = MessagePack.newBufferPacker
    codec.packAsMap(packer, v)
    val bytes = packer.toByteArray
    JSONObjectPrinter.write(MessagePackRecord(bytes))
  }

  def fromJSON(json: String, v: MessageHolder): Unit = {
    val jsonMessage = MessagePackRecord(JSONCodec.toMsgPack(json))
    codec.unpack(jsonMessage.unpacker, v)
  }
}

object ObjectJSONCodec {

  def of[A: ru.TypeTag]: ObjectJSONCodec[A] = {
    MessageCodec.of[A] match {
      case oc: ObjectCodec[A] => new ObjectJSONCodec[A](oc)
      case _ =>
        throw new IllegalArgumentException(s"${surface.of[A]} has no ObjectCodec")
    }
  }
}
