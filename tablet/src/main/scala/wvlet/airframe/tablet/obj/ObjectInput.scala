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
package wvlet.airframe.tablet.obj

import org.msgpack.core.{MessagePack, MessagePacker}
import wvlet.airframe.tablet._
import wvlet.airframe.tablet.msgpack.MessageFormatter
import wvlet.log.LogSupport
import wvlet.surface._
import wvlet.surface.reflect.{ReflectTypeUtil, SurfaceFactory}

import scala.reflect.runtime.{universe => ru}

class ObjectInput(codec: Map[Surface, MessageFormatter[_]] = Map.empty) extends LogSupport {

  def packValue(packer: MessagePacker, v: Any, valueType: Surface) {
    trace(s"packValue: ${v}, ${valueType}, ${valueType.getClass}")
    if (v == null) {
      packer.packNil()
    } else if (codec.contains(valueType)) {
      codec(valueType.rawType).asInstanceOf[MessageFormatter[Any]].pack(packer, v)
    } else {
      valueType.dealias match {
        case Primitive.Byte | Primitive.Short | Primitive.Int | Primitive.Long =>
          packer.packLong(v.toString.toLong)
        case Primitive.Float | Primitive.Double =>
          packer.packDouble(v.toString.toDouble)
        case Primitive.Boolean =>
          packer.packBoolean(v.toString.toBoolean)
        case Primitive.Char | Primitive.String =>
          packer.packString(v.toString)
        case OptionSurface(cl, elemType) =>
          val opt = v.asInstanceOf[Option[_]]
          if (opt.isEmpty) {
            packer.packNil()
          } else {
            val content = opt.get
            packValue(packer, content, elemType)
          }
        case ArraySurface(cl, elemType) =>
          v match {
            case a: Array[String] =>
              packer.packArrayHeader(a.length)
              a.foreach(packer.packString(_))
            case a: Array[Int] =>
              packer.packArrayHeader(a.length)
              a.foreach(packer.packInt(_))
            case a: Array[Float] =>
              packer.packArrayHeader(a.length)
              a.foreach(packer.packFloat(_))
            case a: Array[Double] =>
              packer.packArrayHeader(a.length)
              a.foreach(packer.packDouble(_))
            case a: Array[Boolean] =>
              packer.packArrayHeader(a.length)
              a.foreach(packer.packBoolean(_))
            case _ =>
              throw new UnsupportedOperationException(s"Reading array type of ${valueType} is not supported: ${v}")
          }
        case g: GenericSurface if ReflectTypeUtil.isMap(g.rawType) =>
          val keyType   = g.typeArgs(0)
          val valueType = g.typeArgs(1)
          val m         = v.asInstanceOf[Map[_, _]]
          packer.packMapHeader(m.size)
          for ((k, v) <- m.seq) {
            packValue(packer, k, keyType)
            packValue(packer, v, valueType)
          }
        case g: GenericSurface if ReflectTypeUtil.isSeq(g.rawType) =>
          val elemType = g.typeArgs(0)
          val seq      = v.asInstanceOf[Seq[_]]
          packer.packArrayHeader(seq.length)
          for (s <- seq) {
            packValue(packer, s, elemType)
          }
        case enum if v.getClass.isInstanceOf[Enum[_]] =>
          packer.packString(v.asInstanceOf[Enum[_]].name())
        case other if valueType.name == "DateTime" =>
          // TODO Allow having custom serde
          packer.packString(v.toString())
        case other =>
          // Nested objects
          packObj(packer, v, other)
      }
    }
  }

  private def packObj(packer: MessagePacker, obj: Any, surface: Surface) {
    if (codec.contains(surface)) {
      trace(s"Using codec for ${surface}")
      codec(surface).asInstanceOf[MessageFormatter[Any]].pack(packer, obj)
    } else {
      if (surface.isPrimitive) {
        packValue(packer, obj, surface)
      } else {
        // TODO polymorphic types (e.g., B extends A, C extends B)
        // TODO add parameter values not in the schema
        packer.packArrayHeader(surface.params.length)
        for (p <- surface.params) {
          val v = p.get(obj)
          trace(s"packing ${p.name}, ${p.surface}")
          packValue(packer, v, p.surface)
        }
      }
    }
  }

  def read[A: ru.TypeTag](record: A): Record = {
    val packer = MessagePack.newDefaultBufferPacker()
    if (record == null) {
      packer.packArrayHeader(0) // empty array
    } else {
      packObj(packer, record, SurfaceFactory.of[A])
    }
    MessagePackRecord(packer.toByteArray)
  }
}
