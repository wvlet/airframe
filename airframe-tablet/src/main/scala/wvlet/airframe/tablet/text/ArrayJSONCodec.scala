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
import wvlet.airframe.codec.PrimitiveCodec.{
  BooleanArrayCodec,
  CharArrayCodec,
  DoubleArrayCodec,
  FloatArrayCodec,
  IntArrayCodec,
  LongArrayCodec,
  ShortArrayCodec,
  StringArrayCodec
}
import wvlet.airframe.codec.{JSONCodec, MessageCodec, MessageHolder}
import wvlet.airframe.msgpack.spi.MessagePack
import wvlet.airframe.surface.Surface
import wvlet.airframe.tablet.MessagePackRecord

import scala.reflect.runtime.{universe => ru}

sealed trait ArrayJSONCodec[T] {
  protected val codec: MessageCodec[Array[T]]
  def toJSON(v: Array[T]): String = {
    val bytes = codec.toMsgPack(v)
    JSONObjectPrinter.write(MessagePackRecord(bytes))
  }

  def fromJSON(json: String, v: MessageHolder): Unit = {
    val jsonMessage = MessagePackRecord(JSONCodec.toMsgPack(json))
    codec.unpack(jsonMessage.unpacker, v)
  }
}

/**
  * JSON array codec for the primitive type elements.
  */
object ArrayJSONCodec {
  implicit object ShortArrayJSONCodec extends ArrayJSONCodec[Short] {
    override protected val codec: MessageCodec[Array[Short]] = ShortArrayCodec
  }
  implicit object IntArrayJSONCodec extends ArrayJSONCodec[Int] {
    override protected val codec: MessageCodec[Array[Int]] = IntArrayCodec
  }
  implicit object LongArrayJSONCodec extends ArrayJSONCodec[Long] {
    override protected val codec: MessageCodec[Array[Long]] = LongArrayCodec
  }
  implicit object CharArrayJSONCodec extends ArrayJSONCodec[Char] {
    override protected val codec: MessageCodec[Array[Char]] = CharArrayCodec
  }
  implicit object FloatArrayJSONCodec extends ArrayJSONCodec[Float] {
    override protected val codec: MessageCodec[Array[Float]] = FloatArrayCodec
  }
  implicit object DoubleArrayJSONCodec extends ArrayJSONCodec[Double] {
    override protected val codec: MessageCodec[Array[Double]] = DoubleArrayCodec
  }
  implicit object BooleanArrayJSONCodec extends ArrayJSONCodec[Boolean] {
    override protected val codec: MessageCodec[Array[Boolean]] = BooleanArrayCodec
  }
  implicit object StringArrayJSONCodec extends ArrayJSONCodec[String] {
    override protected val codec: MessageCodec[Array[String]] = StringArrayCodec
  }
  def of[A: ru.TypeTag](implicit codec: ArrayJSONCodec[A]): ArrayJSONCodec[A] = {
    MessageCodec.of[Array[A]] match {
      case _: MessageCodec[Array[A]] => codec
      case _ =>
        throw new IllegalArgumentException(s"${Surface.of[A]} has no PrimitiveCodec for array")
    }
  }
}
