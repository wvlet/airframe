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
import wvlet.airframe.codec.MessageCodec
import wvlet.airframe.codec.PrimitiveCodec.AnyCodec
import wvlet.airframe.msgpack.spi.MsgPack
import wvlet.airframe.surface.{Parameter, Surface}
import wvlet.log.LogSupport

import scala.jdk.CollectionConverters._

trait FieldCodec extends LogSupport {
  def index: Int
  def name: String
  def parquetCodec: ParquetWriteCodec
  // Surface parameter
  def param: Parameter

  def write(recordConsumer: RecordConsumer, v: Any): Unit
}

/**
  * A codec for writing an object parameter as a Parquet field
  */
case class ParameterCodec(index: Int, name: String, param: Parameter, parquetCodec: ParquetWriteCodec)
    extends FieldCodec {
  def write(recordConsumer: RecordConsumer, v: Any): Unit = {
    try {
      recordConsumer.startField(name, index)
      parquetCodec.write(recordConsumer, v)
    } finally {
      recordConsumer.endField(name, index)
    }
  }
}

/**
  * Object parameter codec for Option[X] type. This codec is used for skipping startField() and endField() calls at all
  */
class OptionParameterCodec(parameterCodec: ParameterCodec) extends FieldCodec {
  override def index: Int                      = parameterCodec.index
  override def name: String                    = parameterCodec.name
  override def parquetCodec: ParquetWriteCodec = parameterCodec.parquetCodec
  override def param: Parameter                = parameterCodec.param

  override def write(recordConsumer: RecordConsumer, v: Any): Unit = {
    v match {
      case Some(x) =>
        parameterCodec.write(recordConsumer, x)
      case _ => // None or null
      // Skip writing Optional parameter
    }
  }
}

class SeqParquetCodec(elementCodec: ParquetWriteCodec) extends ParquetWriteCodec with LogSupport {
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
        val msgpack = AnyCodec.toMsgPack(v)
        recordConsumer.addBinary(Binary.fromConstantByteArray(msgpack))
    }
  }

  override def writeMsgPack(recordConsumer: RecordConsumer, msgpack: MsgPack): Unit = ???
}

case class ObjectParquetWriteCodec(paramCodecs: Seq[FieldCodec], isRoot: Boolean = false)
    extends ParquetWriteCodec
    with LogSupport {
  def asRoot: ObjectParquetWriteCodec = this.copy(isRoot = true)

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
          paramCodecs.foreach { p =>
            p.param.get(v) match {
              case null =>
              case paramValue =>
                trace(s"Write ${p.name}: ${paramValue}")
                p.write(recordConsumer, paramValue)
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

  override def writeMsgPack(recordConsumer: RecordConsumer, msgpack: MsgPack): Unit = ???
}

object ObjectParquetWriteCodec {
  def buildFromSurface(surface: Surface, schema: MessageType): ObjectParquetWriteCodec = {
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
      val pc = ParameterCodec(
        param.index,
        param.name,
        param,
        ParquetWriteCodec.parquetCodecOf(tpe, elementSurface, elementCodec)
      )
      tpe.getRepetition match {
        case Repetition.OPTIONAL =>
          new OptionParameterCodec(pc)
        case _ =>
          pc
      }
    }
    ObjectParquetWriteCodec(paramCodecs)
  }
}
