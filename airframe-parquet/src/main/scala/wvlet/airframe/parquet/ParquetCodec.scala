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
import org.apache.parquet.schema.LogicalTypeAnnotation.stringType
import org.apache.parquet.schema.PrimitiveType.PrimitiveTypeName
import org.apache.parquet.schema.Type
import org.apache.parquet.schema.Type.Repetition
import wvlet.airframe.codec.MessageCodec
import wvlet.airframe.msgpack.spi.MsgPack
import wvlet.airframe.codec.PrimitiveCodec.{
  AnyCodec,
  BooleanCodec,
  DoubleCodec,
  FloatCodec,
  IntCodec,
  LongCodec,
  StringCodec,
  ValueCodec
}
import wvlet.airframe.surface.{Parameter, Surface}
import wvlet.log.LogSupport

import scala.jdk.CollectionConverters._

trait ParquetCodec {
  def write(recordConsumer: RecordConsumer, v: Any): Unit
  def writeMsgPack(recordConsumer: RecordConsumer, msgpack: MsgPack): Unit
}

case class ParameterCodec(index: Int, name: String, param: Parameter, parquetCodec: ParquetCodec)

class ObjectParquetCodec(paramCodecs: Seq[ParameterCodec], isRoot: Boolean) extends ParquetCodec {
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
            try {
              recordConsumer.startField(p.name, p.index)
              p.parquetCodec.write(recordConsumer, paramValue)
            } finally {
              recordConsumer.endField(p.name, p.index)
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

/**
  * Convert object --[MessageCodec]--> msgpack --[MessageCodec]--> Parquet type --> RecordConsumer
  * @param tpe
  * @param index
  * @param codec
  */
abstract class PrimitiveParquetCodec(tpe: Type, index: Int, protected val codec: MessageCodec[_]) extends ParquetCodec {
  protected def writeValue(recordConsumer: RecordConsumer, msgpack: MsgPack): Unit

  def write(recordConsumer: RecordConsumer, v: Any): Unit = {
    val msgpack = codec.asInstanceOf[MessageCodec[Any]].toMsgPack(v)
    writeMsgPack(recordConsumer, msgpack)
  }

  def writeMsgPack(recordConsumer: RecordConsumer, msgpack: MsgPack): Unit = {
    recordConsumer.startField(tpe.getName, index)
    writeValue(recordConsumer, msgpack)
    recordConsumer.endField(tpe.getName, index)
  }
}

class SeqParquetCodec(tpe: Type, index: Int, elementCodec: ParquetCodec) extends ParquetCodec with LogSupport {
  override def write(recordConsumer: RecordConsumer, v: Any): Unit = {
    info(s"write: ${v}")
    recordConsumer.startField(tpe.getName, index)
    v match {
      case s: Seq[_] =>
        for (elem <- s) {
          info(s"write elem: ${elem}")
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
    recordConsumer.endField(tpe.getName, index)
  }

  override def writeMsgPack(recordConsumer: RecordConsumer, msgpack: MsgPack): Unit = ???
}

object ParquetCodec extends LogSupport {

  def codecOf(surface: Surface): ParquetCodec = ???

  private[parquet] def parquetCodecOf(tpe: Type, index: Int, codec: MessageCodec[_]): ParquetCodec = {
    if (tpe.isPrimitive) {
      val primitiveCodec = tpe.asPrimitiveType().getPrimitiveTypeName match {
        case PrimitiveTypeName.INT32 =>
          new PrimitiveParquetCodec(tpe, index, codec) {
            override protected def writeValue(recordConsumer: RecordConsumer, msgpack: MsgPack): Unit = {
              recordConsumer.addInteger(IntCodec.fromMsgPack(msgpack))
            }
          }
        case PrimitiveTypeName.INT64 =>
          new PrimitiveParquetCodec(tpe, index, codec) {
            override protected def writeValue(recordConsumer: RecordConsumer, msgpack: MsgPack): Unit = {
              recordConsumer.addLong(LongCodec.fromMsgPack(msgpack))
            }
          }
        case PrimitiveTypeName.BOOLEAN =>
          new PrimitiveParquetCodec(tpe, index, codec) {
            override protected def writeValue(recordConsumer: RecordConsumer, msgpack: MsgPack): Unit = {
              recordConsumer.addBoolean(BooleanCodec.fromMsgPack(msgpack))
            }
          }
        case PrimitiveTypeName.FLOAT =>
          new PrimitiveParquetCodec(tpe, index, codec) {
            override protected def writeValue(recordConsumer: RecordConsumer, msgpack: MsgPack): Unit = {
              recordConsumer.addFloat(FloatCodec.fromMsgPack(msgpack))
            }
          }
        case PrimitiveTypeName.DOUBLE =>
          new PrimitiveParquetCodec(tpe, index, codec) {
            override protected def writeValue(recordConsumer: RecordConsumer, msgpack: MsgPack): Unit = {
              recordConsumer.addDouble(DoubleCodec.fromMsgPack(msgpack))
            }
          }
        case PrimitiveTypeName.BINARY if tpe.getLogicalTypeAnnotation == stringType =>
          new PrimitiveParquetCodec(tpe, index, codec) {
            override protected def writeValue(recordConsumer: RecordConsumer, msgpack: MsgPack): Unit = {
              recordConsumer.addBinary(Binary.fromString(StringCodec.fromMsgPack(msgpack)))
            }
          }
        case _ =>
          new PrimitiveParquetCodec(tpe, index, codec) {
            override protected def writeValue(recordConsumer: RecordConsumer, msgpack: MsgPack): Unit = {
              recordConsumer.addBinary(Binary.fromConstantByteArray(msgpack))
            }
          }
      }
      tpe.getRepetition match {
        case Repetition.REPEATED =>
          new SeqParquetCodec(tpe, index, primitiveCodec)
        case _ =>
          primitiveCodec
      }
    } else {
      new PrimitiveParquetCodec(tpe, index, codec) {
        override protected def writeValue(recordConsumer: RecordConsumer, msgpack: MsgPack): Unit = {
          recordConsumer.addBinary(Binary.fromConstantByteArray(msgpack))
        }
      }
    }
  }
}
