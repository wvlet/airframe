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
package wvlet.airframe.parquet

import org.apache.parquet.io.api.{Binary, RecordConsumer}
import org.apache.parquet.schema.Type.Repetition
import org.apache.parquet.schema.{MessageType, Type}
import wvlet.airframe.codec.{JSONCodec, MessageCodec, MessageCodecException}
import wvlet.airframe.codec.PrimitiveCodec.{AnyCodec, ValueCodec}
import wvlet.airframe.json.JSONParseException
import wvlet.airframe.msgpack.spi.Value.{ArrayValue, BinaryValue, MapValue, NilValue, StringValue}
import wvlet.airframe.msgpack.spi.{Code, MessagePack, MsgPack, Value}
import wvlet.airframe.surface.{CName, Parameter, Surface}
import wvlet.log.LogSupport

import scala.jdk.CollectionConverters.*

trait ParquetFieldWriter extends LogSupport {
  def index: Int
  def name: String
  def parquetCodec: ParquetWriteCodec
  // Surface parameter
  // def param: Parameter

  def write(recordConsumer: RecordConsumer, v: Any): Unit
  def writeMsgPack(recordConsumer: RecordConsumer, v: MsgPack): Unit
}

/**
  * A codec for writing an object parameter as a Parquet field
  */
case class ParquetParameterWriter(index: Int, name: String, parquetCodec: ParquetWriteCodec)
    extends ParquetFieldWriter {

  def write(recordConsumer: RecordConsumer, v: Any): Unit = {
    // if v is empty, Parquet requires skipping to write the field completely
    v match {
      case s: Seq[_] if s.isEmpty    =>
      case a: Array[_] if a.isEmpty  =>
      case m: Map[_, _] if m.isEmpty =>
      case None                      =>
      case _ =>
        try {
          recordConsumer.startField(name, index)
          parquetCodec.write(recordConsumer, v)
        } finally {
          recordConsumer.endField(name, index)
        }
    }
  }

  def writeMsgPack(recordConsumer: RecordConsumer, v: MsgPack): Unit = {
    try {
      recordConsumer.startField(name, index)
      parquetCodec.writeMsgPack(recordConsumer, v)
    } finally {
      recordConsumer.endField(name, index)
    }
  }
}

/**
  * Object parameter codec for Option[X] type. This codec is used for skipping startField() and endField() calls at all
  */
class ParquetOptionWriter(parameterCodec: ParquetParameterWriter) extends ParquetFieldWriter {
  override def index: Int                      = parameterCodec.index
  override def name: String                    = parameterCodec.name
  override def parquetCodec: ParquetWriteCodec = parameterCodec.parquetCodec

  override def write(recordConsumer: RecordConsumer, v: Any): Unit = {
    v match {
      case Some(x) =>
        parameterCodec.write(recordConsumer, x)
      case _ => // None or null
      // Skip writing Optional parameter
    }
  }

  override def writeMsgPack(recordConsumer: RecordConsumer, v: MsgPack): Unit = {
    if (v != null && (v.length == 1 && v(0) != Code.NIL)) {
      parameterCodec.writeMsgPack(recordConsumer, v)
    }
  }
}

class ParquetSeqWriter(elementCodec: ParquetWriteCodec) extends ParquetWriteCodec with LogSupport {
  override def write(recordConsumer: RecordConsumer, v: Any): Unit = {
    v match {
      case s: Seq[_] =>
        for (elem <- s) {
          elementCodec.write(recordConsumer, elem)
        }
      case a: Array[_] =>
        for (elem <- a) {
          elementCodec.write(recordConsumer, elem)
        }
      case javaSeq: java.util.Collection[_] =>
        for (elem <- javaSeq.asScala) {
          elementCodec.write(recordConsumer, elem)
        }
      case _ =>
        // Write unknown value as binary
        val msgpack = AnyCodec.default.toMsgPack(v)
        recordConsumer.addBinary(Binary.fromConstantByteArray(msgpack))
    }
  }

  override def writeMsgPack(recordConsumer: RecordConsumer, msgpack: MsgPack): Unit = ???
}

case class ParquetObjectWriter(paramWriters: Seq[ParquetFieldWriter], params: Seq[Parameter], isRoot: Boolean = false)
    extends ParquetWriteCodec
    with LogSupport {
  override def asRoot: ParquetObjectWriter = this.copy(isRoot = true)

  def write(recordConsumer: RecordConsumer, v: Any): Unit = {
    try {
      trace(s"Write object: ${v}")
      if (isRoot) {
        recordConsumer.startMessage()
      } else {
        recordConsumer.startGroup()
      }
      v match {
        case null =>
        // No output
        case _ =>
          paramWriters.zip(params).foreach { case (paramWriter, p) =>
            p.get(v) match {
              case null =>
              case paramValue =>
                trace(s"Write ${p.name}: ${paramValue}")
                paramWriter.write(recordConsumer, paramValue)
            }
          }
      }
    } finally {
      if (isRoot) {
        recordConsumer.endMessage()
      } else {
        recordConsumer.endGroup()
      }
    }
  }

  private def schema           = s"[${paramWriters.map(_.name).mkString(", ")}]"
  private lazy val columnNames = paramWriters.map(x => CName.toCanonicalName(x.name)).toIndexedSeq
  private lazy val parquetCodecTable: Map[String, ParquetFieldWriter] = {
    paramWriters.map(x => CName.toCanonicalName(x.name) -> x).toMap
  }

  override def writeMsgPack(recordConsumer: RecordConsumer, msgpack: MsgPack): Unit = {
    val value = ValueCodec.fromMsgPack(msgpack)

    if (isRoot) {
      recordConsumer.startMessage()
    } else {
      recordConsumer.startGroup()
    }
    try {
      packValue(recordConsumer, value)
    } finally {
      if (isRoot) {
        recordConsumer.endMessage()
      } else {
        recordConsumer.endGroup()
      }
    }
  }

  private def packValue(recordConsumer: RecordConsumer, value: Value): Unit = {
    def writeColumnValue(columnName: String, v: Value): Unit = {
      trace(s"write column value: ${columnName} -> ${v}")
      parquetCodecTable.get(columnName) match {
        case Some(parameterCodec) =>
          v match {
            case NilValue =>
            // skip
            case m: MapValue if m.isEmpty =>
            // skip
            case a: ArrayValue if a.isEmpty =>
            // skip
            case _ =>
              parameterCodec.writeMsgPack(recordConsumer, v.toMsgpack)
          }
        case None =>
        // No record. Skip the value
      }
    }
    trace(s"packValue: ${value}")

    value match {
      case arr: ArrayValue =>
        // Array value
        if (arr.size == paramWriters.length) {
          for ((e, colIndex) <- arr.elems.zipWithIndex) {
            val colName = columnNames(colIndex)
            writeColumnValue(colName, e)
          }
        } else {
          // Invalid shape
          throw new IllegalArgumentException(s"${arr} size doesn't match with ${paramWriters}")
        }
      case m: MapValue =>
        for ((k, v) <- m.entries) {
          val keyValue = k.toString
          val cKey     = CName.toCanonicalName(keyValue)
          writeColumnValue(cKey, v)
        }
      case b: BinaryValue =>
        // Assume it's a message pack value
        try {
          val v = ValueCodec.fromMsgPack(b.v)
          packValue(recordConsumer, v)
        } catch {
          case e: MessageCodecException =>
            invalidInput(b, e)
        }
      case s: StringValue =>
        val str = s.toString
        if (str.startsWith("{") || str.startsWith("[")) {
          // Assume the input is a json object or an array
          try {
            val msgpack = JSONCodec.toMsgPack(str)
            val value   = ValueCodec.fromMsgPack(msgpack)
            packValue(recordConsumer, value)
          } catch {
            case e: JSONParseException =>
              // Not a json value.
              invalidInput(s, e)
          }
        } else {
          invalidInput(s, null)
        }
      case _ =>
        invalidInput(value, null)
    }
  }

  private def invalidInput(v: Value, cause: Throwable): Nothing = {
    throw new IllegalArgumentException(
      s"The input for ${schema} must be Map[String, Any], Array, MsgPack, or JSON strings: ${v}",
      cause
    )
  }
}

object ParquetObjectWriter {
  def buildFromSurface(surface: Surface, schema: MessageType): ParquetObjectWriter = {
    val paramCodecs = surface.params.zip(schema.getFields.asScala).map { case (param, tpe) =>
      // Resolve the element type X of Option[X], Seq[X], etc.
      val elementSurface = tpe.getRepetition match {
        case Repetition.OPTIONAL if param.surface.isOption =>
          param.surface.typeArgs(0)
        case Repetition.REPEATED if param.surface.typeArgs.length == 1 =>
          param.surface.typeArgs(0)
        case _ =>
          param.surface
      }
      val elementCodec = MessageCodec.ofSurface(elementSurface)
      val pc = ParquetParameterWriter(
        param.index,
        param.name,
        ParquetWriteCodec.parquetCodecOf(tpe, elementSurface, elementCodec)
      )
      tpe.getRepetition match {
        case Repetition.OPTIONAL =>
          new ParquetOptionWriter(pc)
        case _ =>
          pc
      }
    }
    ParquetObjectWriter(paramCodecs, surface.params)
  }
}
