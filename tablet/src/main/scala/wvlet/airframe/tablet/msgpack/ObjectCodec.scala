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
package wvlet.airframe.tablet.msgpack

import org.msgpack.core.{MessagePacker, MessageUnpacker}
import org.msgpack.value.{ValueFactory, ValueType, Variable}
import wvlet.log.LogSupport
import wvlet.surface.reflect.TypeConverter
import wvlet.surface.{Surface, Zero}

import scala.collection.JavaConverters._

/**
  *
  */
case class ObjectCodec[A](surface: Surface, paramCodec: Seq[MessageCodec[_]]) extends MessageCodec[A] with LogSupport {

  private lazy val codecTable = surface.params.zip(paramCodec).map { case (p, c) => p.name -> c }.toMap[String, MessageCodec[_]]

  override def pack(p: MessagePacker, v: A): Unit = {
    val numParams = surface.params.length
    // Use array format [p1, p2, ....]
    p.packArrayHeader(numParams)
    for ((param, codec) <- surface.params.zip(paramCodec)) {
      val paramValue = param.get(v)
      codec.asInstanceOf[MessageCodec[Any]].pack(p, paramValue)
    }
  }

  override def unpack(u: MessageUnpacker, v: MessageHolder): Unit = {
    val numParams = surface.params.length

    u.getNextFormat.getValueType match {
      case ValueType.ARRAY =>
        val numElems = u.unpackArrayHeader()
        if (numParams != numElems) {
          u.skipValue(numElems)
          v.setNull
        } else {
          var index = 0
          val b     = Seq.newBuilder[Any]
          while (index < numElems && index < numParams) {
            // TODO reuse message holders
            paramCodec(index).unpack(u, v)
            // TODO handle null value?
            // TODO Use more efficient type conversion
            val arg = TypeConverter.convert(v.getLastValue, surface.params(index).surface).getOrElse(null)
            b += arg
            index += 1
          }
          val args = b.result()
          trace(s"Building $surface with args:[${args.map(x => s"${x}:${x.getClass.getName}")}]")
          surface.objectFactory
            .map(_.newInstance(args))
            .map(x => v.setObject(x))
            .getOrElse(v.setNull)
        }
      case ValueType.MAP =>
        val m = Map.newBuilder[String, Any]

        // { key:value, ...} -> record
        val mapSize  = u.unpackMapHeader()
        val keyValue = new Variable
        for (i <- 0 until mapSize) {
          // Read key
          u.unpackValue(keyValue)
          val keyString = keyValue.toString

          // Read value
          // TODO Use CName for parameter names?
          codecTable.get(keyString) match {
            case Some(codec) =>
              codec.unpack(u, v)
              m += (keyString -> v.getLastValue)
            case None =>
              // unknown parameter
              u.skipValue()
          }
        }
        val map = m.result()
        val args = for (i <- 0 until numParams) yield {
          val p         = surface.params(i)
          val paramName = p.name
          map.get(paramName) match {
            case Some(x) => x
            case None =>
              p.getDefaultValue.getOrElse(Zero.zeroOf(surface))
          }
        }
        surface.objectFactory
          .map(_.newInstance(args))
          .map(x => v.setObject(x))
          .getOrElse(v.setNull)
      case other =>
        u.skipValue()
        v.setIncompatibleFormatException(s"Expected ARRAY or MAP type input for ${surface}")
    }
  }

  def unpack(u: MessageUnpacker, v: MessageHolder, params: Seq[String]) {}
}

/**
  * Codec for recording name of the parameters in an object
  * @param surface
  */
case class SurfaceCodec(surface: Surface) extends MessageCodec[Surface] {
  import ValueFactory._

  override def pack(p: MessagePacker, v: Surface): Unit = {
    val m = newMapBuilder()
      .put(newString("fullName"), newString(surface.fullName))
      .put(newString("params"), newArray(surface.params.map(_.name).map(newString(_)).asJava))
      .build()
    p.packValue(m)
  }

  override def unpack(u: MessageUnpacker, v: MessageHolder): Unit = {
    val m = u.unpackValue().asMapValue().map.asScala
    m.get(newString("params")) match {
      case Some(arr) =>
        val columns = for (param <- arr.asArrayValue().list().asScala) yield {
          param.toString
        }
        v.setObject(columns.toIndexedSeq)
      case None =>
        v.setIncompatibleFormatException("")
    }
  }
}
