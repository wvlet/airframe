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

import wvlet.airframe.msgpack.spi.MessagePackApi._
import wvlet.log.LogSupport
import wvlet.surface.reflect.CName
import wvlet.surface.{Surface, Zero}

import scala.util.{Failure, Success, Try}

/**
  *
  */
case class ObjectCodec[A](surface: Surface, paramCodec: Seq[MessageCodec[_]]) extends MessageCodec[A] with LogSupport {

  private lazy val codecTable = surface.params.zip(paramCodec).map { case (p, c) => CName.toCanonicalName(p.name) -> c }.toMap[String, MessageCodec[_]]

  override def pack(p: Packer, v: A): Unit = {
    val numParams = surface.params.length
    // Use array format [p1, p2, ....]
    p.packArrayHeader(numParams)
    for ((param, codec) <- surface.params.zip(paramCodec)) {
      val paramValue = param.get(v)
      if (paramValue == null) {
        p.packNil
      } else {
        codec.asInstanceOf[MessageCodec[Any]].pack(p, paramValue)
      }
    }
  }

  def packAsMap(p: Packer, v: A): Unit = {
    val numParams = surface.params.length
    // Use array format [p1, p2, ....]
    p.packMapHeader(numParams)
    for ((param, codec) <- surface.params.zip(paramCodec)) {
      val paramValue = param.get(v)
      p.packString(param.name)
      codec.asInstanceOf[MessageCodec[Any]].pack(p, paramValue)
    }
  }

  override def unpack(u: Unpacker, v: MessageHolder): Unit = {
    val numParams = surface.params.length

    u.getNextValueType match {
      case ValueType.ARRAY =>
        val numElems = u.unpackArrayHeader
        var index    = 0
        val b        = Seq.newBuilder[Any]
        while (index < numElems && index < numParams) {
          val p = surface.params(index)
          paramCodec(index).unpack(u, v)
          val arg = if (v.isNull) {
            trace(v.getError)
            p.getDefaultValue.getOrElse(Zero.zeroOf(p.surface))
          } else {
            v.getLastValue
          }
          b += arg
          index += 1
        }
        // Populate args with the default or zero value
        while (index < numParams) {
          val p = surface.params(index)
          b += p.getDefaultValue.getOrElse(Zero.zeroOf(p.surface))
          index += 1
        }
        // Ignore additional args
        while (index < numElems) {
          u.skipValue
          index += 1
        }
        val args = b.result()
        trace(s"Building $surface with args:[${args.filter(_ != null).map(x => s"${x}:${x.getClass.getName}")}]")
        surface.objectFactory match {
          case Some(c) =>
            Try(c.newInstance(args)) match {
              case Success(x) => v.setObject(x)
              case Failure(e) => v.setError(e)
            }
          case None =>
            warn(s"No factory is found for ${surface}")
            v.setNull
        }
      case ValueType.MAP =>
        val m = Map.newBuilder[String, Any]

        // { key:value, ...} -> record
        val mapSize  = u.unpackMapHeader
        val keyValue = new Variable
        for (i <- 0 until mapSize) {
          // Read key
          u.unpackValue(keyValue)

          val keyString = keyValue.toString
          // Use CName for parameter names
          val cKey = CName.toCanonicalName(keyString)
          // Read value
          codecTable.get(cKey) match {
            case Some(codec) =>
              codec.unpack(u, v)
              m += (cKey -> v.getLastValue)
            case None =>
              // unknown parameter
              u.skipValue
          }
        }
        val map = m.result()
        val args = for (i <- 0 until numParams) yield {
          val p         = surface.params(i)
          val paramName = CName.toCanonicalName(p.name)
          map.get(paramName) match {
            case Some(x) => x
            case None =>
              p.getDefaultValue.getOrElse(Zero.zeroOf(p.surface))
          }
        }
        surface.objectFactory match {
          case Some(c) =>
            Try(c.newInstance(args)) match {
              case Success(x) => v.setObject(x)
              case Failure(e) => v.setError(e)
            }
          case None =>
            warn(s"No factory is found for ${surface}")
            v.setNull
        }
      case other =>
        u.skipValue
        v.setIncompatibleFormatException(this, s"Expected ARRAY or MAP type input for ${surface}")
    }
  }
}
