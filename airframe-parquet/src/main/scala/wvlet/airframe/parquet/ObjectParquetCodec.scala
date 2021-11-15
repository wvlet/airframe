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
import org.apache.parquet.schema.{MessageType, Type}
import wvlet.airframe.codec.MessageCodec
import wvlet.airframe.codec.PrimitiveCodec.AnyCodec
import wvlet.airframe.msgpack.spi.MsgPack
import wvlet.airframe.surface.{Parameter, Surface}
import wvlet.log.LogSupport
import scala.jdk.CollectionConverters._

/**
  * A codec for writing an object parameter as a Parquet field
  */
case class ParameterCodec(index: Int, name: String, param: Parameter, parquetCodec: ParquetCodec) {
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
class OptionParameterCodec(elementCodec: ParameterCodec) extends ParquetCodec {
  override def write(recordConsumer: RecordConsumer, v: Any): Unit = {
    v match {
      case None =>
      // Skip writing Optional parameter
      case _ =>
        elementCodec.write(recordConsumer, v)
    }
  }
}

class SeqParquetCodec(elementCodec: ParquetCodec) extends ParquetCodec with LogSupport {
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

case class ObjectParquetCodec(paramCodecs: Seq[ParameterCodec], isRoot: Boolean = false) extends ParquetCodec {
  def asRoot: ObjectParquetCodec = this.copy(isRoot = true)

  def write(recordConsumer: RecordConsumer, v: Any): Unit = {
    try {
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
            val paramValue = p.param.get(v)
            p.write(recordConsumer, paramValue)
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

object ObjectParquetCodec {
  def buildFromSurface(surface: Surface): (MessageType, ObjectParquetCodec) = {
    val schema = Parquet.toParquetSchema(surface)
    val paramCodecs = surface.params.zip(schema.getFields.asScala).map { case (param, tpe) =>
      val codec = MessageCodec.ofSurface(param.surface)
      ParameterCodec(param.index, param.name, param, ParquetCodec.parquetCodecOf(tpe, codec))
    }
    (schema, ObjectParquetCodec(paramCodecs).asRoot)
  }
}
