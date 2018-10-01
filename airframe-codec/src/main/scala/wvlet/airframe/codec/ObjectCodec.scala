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

import org.msgpack.core.{MessagePack, MessagePacker, MessageUnpacker}
import org.msgpack.value.{ValueType, Variable}
import wvlet.log.LogSupport
import wvlet.airframe.surface.reflect.CName
import wvlet.airframe.surface.{Parameter, Surface, Zero}

import scala.util.{Failure, Success, Try}

object ParamListCodec {
  val defaultEmptyParamBinder = { s: Surface =>
    Zero.zeroOf(s)
  }
}

/**
  * A generic codec for parameter lists:
  * - array form: [v1, v2, ...]
  * - map form: {k1:v1, k2:v2, ..}
  * @param name
  * @param params
  * @param paramCodec
  * @param emptyParamBinder
  */
class ParamListCodec(name: String,
                     params: IndexedSeq[Parameter],
                     paramCodec: Seq[MessageCodec[_]],
                     emptyParamBinder: Surface => Any = ParamListCodec.defaultEmptyParamBinder)
    extends MessageCodec[Seq[Any]]
    with LogSupport {
  private lazy val codecTable =
    params
      .zip(paramCodec)
      .map { case (p, c) => CName.toCanonicalName(p.name) -> c }
      .toMap[String, MessageCodec[_]]

  override def pack(p: MessagePacker, v: Seq[Any]): Unit = {
    packAsArray(p, v)
  }

  def packAsArray(p: MessagePacker, paramValueList: Seq[Any]): Unit = {
    val numParams = params.length
    // Use array format [p1, p2, ....]
    p.packArrayHeader(numParams)
    for ((paramValue, codec) <- paramValueList.zip(paramCodec)) {
      if (paramValue == null) {
        p.packNil()
      } else {
        codec.asInstanceOf[MessageCodec[Any]].pack(p, paramValue)
      }
    }
  }

  def packAsMap(p: MessagePacker, obj: Any): Unit = {
    val numParams = params.length
    // Use array format [p1, p2, ....]
    p.packMapHeader(numParams)
    for ((param, codec) <- params.zip(paramCodec)) {
      val paramValue = param.get(obj)
      p.packString(param.name)
      codec.asInstanceOf[MessageCodec[Any]].pack(p, paramValue)
    }
  }

  override def unpack(u: MessageUnpacker, v: MessageHolder): Unit = {
    val numParams = params.length

    u.getNextFormat.getValueType match {
      case ValueType.ARRAY =>
        val numElems = u.unpackArrayHeader()
        var index    = 0
        val b        = Seq.newBuilder[Any]
        while (index < numElems && index < numParams) {
          val p = params(index)
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
          val p = params(index)
          b += p.getDefaultValue.getOrElse(emptyParamBinder(p.surface))
          index += 1
        }
        // Ignore additional args
        while (index < numElems) {
          u.skipValue()
          index += 1
        }
        v.setObject(b.result())
      case ValueType.MAP =>
        val m = Map.newBuilder[String, Any]

        // { key:value, ...} -> record
        val mapSize  = u.unpackMapHeader()
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
              if (!v.isNull) {
                m += (cKey -> v.getLastValue)
              }
            case None =>
              // unknown parameter
              u.skipValue()
          }
        }
        val map = m.result()
        val args = for (i <- 0 until numParams) yield {
          val p         = params(i)
          val paramName = CName.toCanonicalName(p.name)
          map.get(paramName) match {
            case Some(x) => x
            case None =>
              p.getDefaultValue.getOrElse(emptyParamBinder(p.surface))
          }
        }
        v.setObject(args)
      case other =>
        u.skipValue()
        v.setIncompatibleFormatException(this, s"Expected ARRAY or MAP type input for ${name}")
    }
  }
}

/**
  *
  */
case class ObjectCodec[A](surface: Surface, paramCodec: Seq[MessageCodec[_]]) extends MessageCodec[A] with LogSupport {

  private val paramListCodec = new ParamListCodec(surface.name, surface.params.toIndexedSeq, paramCodec)

  override def pack(p: MessagePacker, v: A): Unit = {
    val paramValues = surface.params.map(p => p.get(v))
    paramListCodec.packAsArray(p, paramValues)
  }

  def packAsMap(p: MessagePacker, v: A): Unit = {
    paramListCodec.packAsMap(p, v)
  }

  override def unpack(u: MessageUnpacker, v: MessageHolder): Unit = {
    paramListCodec.unpack(u, v)
    if (!v.isNull) {
      val args = v.getLastValue.asInstanceOf[Seq[Any]]
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
    }
  }
}

/**
  * ObjectCodec for generating map values. This is suited to JSON object generation
  * @param surface
  * @param paramCodec
  * @tparam A
  */
case class ObjectMapCodec[A](surface: Surface, paramCodec: Seq[MessageCodec[_]])
    extends MessageCodec[A]
    with LogSupport {
  private val paramListCodec = new ParamListCodec(surface.name, surface.params.toIndexedSeq, paramCodec)

  override def pack(p: MessagePacker, v: A): Unit = {
    paramListCodec.packAsMap(p, v)
  }

  override def unpack(u: MessageUnpacker, v: MessageHolder): Unit = {
    paramListCodec.unpack(u, v)
    if (!v.isNull) {
      val args = v.getLastValue.asInstanceOf[Seq[Any]]
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
    }
  }
}
