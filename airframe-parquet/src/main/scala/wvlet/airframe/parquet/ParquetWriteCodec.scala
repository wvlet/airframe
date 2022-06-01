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
import org.apache.parquet.schema.{MessageType, Type}
import org.apache.parquet.schema.Type.Repetition
import wvlet.airframe.codec.MessageCodec
import wvlet.airframe.codec.PrimitiveCodec.{
  BooleanCodec,
  DoubleCodec,
  FloatCodec,
  IntCodec,
  LongCodec,
  StringCodec,
  ValueCodec
}
import wvlet.airframe.msgpack.spi.MsgPack
import wvlet.airframe.surface.Surface
import wvlet.log.LogSupport

trait ParquetWriteCodec {
  def asRoot: ParquetWriteCodec = this
  def write(recordConsumer: RecordConsumer, v: Any): Unit
  def writeMsgPack(recordConsumer: RecordConsumer, msgpack: MsgPack): Unit
}

/**
  * Convert object --[MessageCodec]--> msgpack --[MessageCodec]--> Parquet type --> RecordConsumer
  * @param tpe
  * @param index
  * @param codec
  */
abstract class PrimitiveParquetCodec(codec: MessageCodec[_]) extends ParquetWriteCodec with LogSupport {

  /**
    * The root method for actually writing an input value to the Parquet file
    * @param recordConsumer
    * @param msgpack
    */
  protected def writeValue(recordConsumer: RecordConsumer, msgpack: MsgPack): Unit

  override def write(recordConsumer: RecordConsumer, v: Any): Unit = {
    try {
      val msgpack = codec.asInstanceOf[MessageCodec[Any]].toMsgPack(v)
      writeMsgPack(recordConsumer, msgpack)
    } catch {
      case e: Throwable =>
        warn(e)
    }
  }

  override def writeMsgPack(recordConsumer: RecordConsumer, msgpack: MsgPack): Unit = {
    writeValue(recordConsumer, msgpack)
  }
}

object ParquetWriteCodec extends LogSupport {

  private[parquet] def parquetCodecOf(tpe: Type, surface: Surface, codec: MessageCodec[_]): ParquetWriteCodec = {
    if (tpe.isPrimitive) {
      val primitiveCodec = tpe.asPrimitiveType().getPrimitiveTypeName match {
        case PrimitiveTypeName.INT32 =>
          new PrimitiveParquetCodec(codec) {
            override protected def writeValue(recordConsumer: RecordConsumer, msgpack: MsgPack): Unit = {
              recordConsumer.addInteger(IntCodec.fromMsgPack(msgpack))
            }
          }
        case PrimitiveTypeName.INT64 =>
          new PrimitiveParquetCodec(codec) {
            override protected def writeValue(recordConsumer: RecordConsumer, msgpack: MsgPack): Unit = {
              recordConsumer.addLong(LongCodec.fromMsgPack(msgpack))
            }
          }
        case PrimitiveTypeName.BOOLEAN =>
          new PrimitiveParquetCodec(codec) {
            override protected def writeValue(recordConsumer: RecordConsumer, msgpack: MsgPack): Unit = {
              recordConsumer.addBoolean(BooleanCodec.fromMsgPack(msgpack))
            }
          }
        case PrimitiveTypeName.FLOAT =>
          new PrimitiveParquetCodec(codec) {
            override protected def writeValue(recordConsumer: RecordConsumer, msgpack: MsgPack): Unit = {
              recordConsumer.addFloat(FloatCodec.fromMsgPack(msgpack))
            }
          }
        case PrimitiveTypeName.DOUBLE =>
          new PrimitiveParquetCodec(codec) {
            override protected def writeValue(recordConsumer: RecordConsumer, msgpack: MsgPack): Unit = {
              recordConsumer.addDouble(DoubleCodec.fromMsgPack(msgpack))
            }
          }
        case PrimitiveTypeName.BINARY if tpe.getLogicalTypeAnnotation == stringType =>
          new PrimitiveParquetCodec(codec) {
            override protected def writeValue(recordConsumer: RecordConsumer, msgpack: MsgPack): Unit = {
              recordConsumer.addBinary(Binary.fromString(StringCodec.fromMsgPack(msgpack)))
            }
          }
        case _ =>
          // For the other primitive type values
          new PrimitiveParquetCodec(codec) {
            override protected def writeValue(recordConsumer: RecordConsumer, msgpack: MsgPack): Unit = {
              recordConsumer.addBinary(Binary.fromConstantByteArray(msgpack))
            }
          }
      }
      tpe.getRepetition match {
        case Repetition.REPEATED =>
          new ParquetSeqWriter(primitiveCodec)
        case _ =>
          primitiveCodec
      }
    } else {
      if (surface.params.length > 0) {
        // group type
        val groupCodec = ParquetObjectWriter.buildFromSurface(surface, Parquet.toParquetSchema(surface))
        tpe match {
          case m: MessageType =>
            // MessageType is always a REPEATED type, so no need to wrap with SeqParquetCodec
            groupCodec
          case _ if tpe.isRepetition(Repetition.REPEATED) =>
            new ParquetSeqWriter(groupCodec)
          case _ =>
            groupCodec
        }
      } else {
        new PrimitiveParquetCodec(codec) {
          override protected def writeValue(recordConsumer: RecordConsumer, msgpack: MsgPack): Unit = {
            recordConsumer.addBinary(Binary.fromConstantByteArray(msgpack))
          }
        }
      }
    }
  }
}
