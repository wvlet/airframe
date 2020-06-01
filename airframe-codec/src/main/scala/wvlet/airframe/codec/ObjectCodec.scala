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

import wvlet.airframe.msgpack.spi.{Packer, Unpacker, ValueType}
import wvlet.airframe.surface._
import wvlet.log.LogSupport

import scala.util.{Failure, Success, Try}

trait PackAsMapSupport[A] { self: MessageCodec[A] =>
  def packAsMap(p: Packer, v: A): Unit
}

/**
  * A generic codec for parameter lists:
  * - array form: [v1, v2, ...]
  * - map form: {k1:v1, k2:v2, ..}
  *
  * @param name
  * @param params
  * @param paramCodec
  * @param methodOwner
  */
class ParamListCodec(
    name: String,
    params: IndexedSeq[Parameter],
    paramCodec: Seq[MessageCodec[_]],
    methodOwner: Option[Any] = None
) extends MessageCodec[Seq[Any]]
    with LogSupport {
  private lazy val codecTable =
    params
      .zip(paramCodec)
      .map { case (p, c) => CName.toCanonicalName(p.name) -> c }
      .toMap[String, MessageCodec[_]]

  override def pack(p: Packer, v: Seq[Any]): Unit = {
    packAsArray(p, v)
  }

  def packAsArray(p: Packer, paramValueList: Seq[Any]): Unit = {
    val numParams = params.length
    // Use array format [p1, p2, ....]
    p.packArrayHeader(numParams)
    for ((paramValue, codec) <- paramValueList.zip(paramCodec)) {
      if (paramValue == null) {
        p.packNil
      } else {
        codec.asInstanceOf[MessageCodec[Any]].pack(p, paramValue)
      }
    }
  }

  def packAsMap(p: Packer, obj: Any): Unit = {
    def hasValue(param: Parameter): Boolean = {
      if (!param.surface.isOption) {
        true
      } else {
        param.get(obj) match {
          case None => false
          case _    => true
        }
      }
    }

    // Count the number of non-None values
    val numParams = params.count(hasValue)
    // Use map format {k1:p1, k2:p2, ....}
    p.packMapHeader(numParams)
    for ((param, codec) <- params.zip(paramCodec)) {
      // If the parameter value is None of Option type, we can suppress its key-value output.
      if (hasValue(param)) {
        val paramValue = param.get(obj)
        p.packString(param.name)
        codec.asInstanceOf[MessageCodec[Any]].pack(p, paramValue)
      }
    }
  }

  private def getParamDefaultValue(p: Parameter): Any = {
    def returnZero: Any = {
      if (p.isRequired) {
        // If the parameter has @required annotation, we can't use the Zero value
        throw new MessageCodecException(
          MISSING_PARAMETER,
          this,
          s"Parameter ${name}.${p.name} is missing in the input"
        )
      } else {
        Zero.zeroOf(p.surface)
      }
    }

    p match {
      case m: MethodParameter =>
        methodOwner
          .flatMap { owner =>
            // If the method owner instance is provided, we can resolve method arg default values
            m.getMethodArgDefaultValue(owner)
          }
          .orElse(p.getDefaultValue)
          .getOrElse(returnZero)
      case other =>
        p.getDefaultValue.getOrElse(returnZero)
    }
  }

  override def unpack(u: Unpacker, v: MessageContext): Unit = {
    val numParams = params.length

    u.getNextFormat.getValueType match {
      case ValueType.ARRAY =>
        val numElems = u.unpackArrayHeader
        var index    = 0
        val b        = Seq.newBuilder[Any]
        while (index < numElems && index < numParams) {
          val p = params(index)
          paramCodec(index).unpack(u, v)
          val arg = if (v.isNull) {
            trace(v.getError)
            getParamDefaultValue(p)
          } else {
            v.getLastValue
          }
          b += arg
          index += 1
        }
        // Populate args with the default or zero value
        while (index < numParams) {
          val p = params(index)
          val v = getParamDefaultValue(p)
          b += v
          index += 1
        }
        // Ignore additional args
        while (index < numElems) {
          u.skipValue
          index += 1
        }
        v.setObject(b.result())
      case ValueType.MAP =>
        val m = Map.newBuilder[String, Any]

        // { key:value, ...} -> record
        val mapSize = u.unpackMapHeader
        for (i <- 0 until mapSize) {
          // Read key
          val keyValue = u.unpackValue

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
              u.skipValue
          }
        }
        val map = m.result()
        val args = for (i <- 0 until numParams) yield {
          val p         = params(i)
          val paramName = CName.toCanonicalName(p.name)
          map
            .get(paramName)
            .getOrElse(getParamDefaultValue(p))
        }
        trace(s"map:${map.mkString(",")}, args:${args.mkString(", ")}")
        v.setObject(args)
      case other =>
        u.skipValue
        v.setIncompatibleFormatException(this, s"Expected ARRAY or MAP type input for ${name}")
    }
  }
}

sealed trait ObjectCodecBase {
  def paramCodec: Seq[MessageCodec[_]]
}

/**
  *
  */
case class ObjectCodec[A](surface: Surface, paramCodec: Seq[MessageCodec[_]])
    extends MessageCodec[A]
    with ObjectCodecBase
    with PackAsMapSupport[A]
    with LogSupport {
  private val paramListCodec = new ParamListCodec(surface.name, surface.params.toIndexedSeq, paramCodec)

  override def pack(p: Packer, v: A): Unit = {
    val paramValues = surface.params.map(p => p.get(v))
    paramListCodec.packAsArray(p, paramValues)
  }

  def packAsMap(p: Packer, v: A): Unit = {
    paramListCodec.packAsMap(p, v)
  }

  override def unpack(u: Unpacker, v: MessageContext): Unit = {
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
  *
  * @param surface
  * @param paramCodec
  * @tparam A
  */
case class ObjectMapCodec[A](surface: Surface, paramCodec: Seq[MessageCodec[_]])
    extends MessageCodec[A]
    with ObjectCodecBase
    with PackAsMapSupport[A]
    with LogSupport {
  private val paramListCodec = new ParamListCodec(surface.name, surface.params.toIndexedSeq, paramCodec)

  override def pack(p: Packer, v: A): Unit = {
    paramListCodec.packAsMap(p, v)
  }
  override def packAsMap(p: Packer, v: A): Unit = pack(p, v)

  override def unpack(u: Unpacker, v: MessageContext): Unit = {
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
