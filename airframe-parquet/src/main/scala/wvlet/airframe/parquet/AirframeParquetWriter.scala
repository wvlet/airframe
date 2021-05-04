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

import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.Path
import org.apache.parquet.hadoop.{ParquetFileWriter, ParquetWriter}
import org.apache.parquet.hadoop.api.WriteSupport
import org.apache.parquet.hadoop.api.WriteSupport.WriteContext
import org.apache.parquet.hadoop.metadata.CompressionCodecName
import org.apache.parquet.hadoop.util.HadoopOutputFile
import org.apache.parquet.io.OutputFile
import org.apache.parquet.io.api.{Binary, RecordConsumer}
import org.apache.parquet.schema.LogicalTypeAnnotation.stringType
import org.apache.parquet.schema.PrimitiveType.PrimitiveTypeName
import org.apache.parquet.schema.{MessageType, Type}
import wvlet.airframe.codec.PrimitiveCodec.{BooleanCodec, DoubleCodec, FloatCodec, IntCodec, LongCodec, StringCodec}
import wvlet.airframe.codec.{MessageCodec, MessageCodecFactory}
import wvlet.airframe.msgpack.spi.MsgPack
import wvlet.airframe.parquet.AirframeParquetWriter.ParquetCodec
import wvlet.airframe.surface.{Parameter, Surface}
import wvlet.log.LogSupport

import scala.reflect.runtime.{universe => ru}
import scala.jdk.CollectionConverters._

/**
  */

private[parquet] object AirframeParquetWriter {
  //def newWriter[A: ru.TypeTag]: Builder[A] = Builder[A](surface = Surface.of[A])

  def builder[A: ru.TypeTag](path: String): Builder[A] = {
    val s      = Surface.of[A]
    val fsPath = new Path(path)
    val conf   = new Configuration()
    val file   = HadoopOutputFile.fromPath(fsPath, conf)
    val b      = new Builder[A](s, file)
    // Use snappy by default
    b.withCompressionCodec(CompressionCodecName.SNAPPY)
      .withWriteMode(ParquetFileWriter.Mode.OVERWRITE)
  }

  class Builder[A](surface: Surface, file: OutputFile) extends ParquetWriter.Builder[A, Builder[A]](file: OutputFile) {
    override def self(): Builder[A] = this
    override def getWriteSupport(conf: Configuration): WriteSupport[A] = {
      new AirframeParquetWriteSupport[A](surface)
    }
  }

  /**
    * Convert object --[MessageCodec]--> msgpack --[MessageCodec]--> Parquet type --> RecordConsumer
    * @param tpe
    * @param index
    * @param codec
    */
  abstract class ParquetCodec(tpe: Type, index: Int, codec: MessageCodec[_]) {
    protected def writeValue(recordConsumer: RecordConsumer, msgpack: MsgPack): Unit

    def write(recordConsumer: RecordConsumer, v: Any): Unit = {
      val msgpack = codec.asInstanceOf[MessageCodec[Any]].toMsgPack(v)
      recordConsumer.startField(tpe.getName, index)
      writeValue(recordConsumer, msgpack)
      recordConsumer.endField(tpe.getName, index)
    }
  }

  private[parquet] def parquetCodecOf(tpe: Type, index: Int, codec: MessageCodec[_]): ParquetCodec = {
    if (tpe.isPrimitive) {
      tpe.asPrimitiveType().getPrimitiveTypeName match {
        case PrimitiveTypeName.INT32 =>
          new ParquetCodec(tpe, index, codec) {
            override protected def writeValue(recordConsumer: RecordConsumer, msgpack: MsgPack): Unit = {
              recordConsumer.addInteger(IntCodec.fromMsgPack(msgpack))
            }
          }
        case PrimitiveTypeName.INT64 =>
          new ParquetCodec(tpe, index, codec) {
            override protected def writeValue(recordConsumer: RecordConsumer, msgpack: MsgPack): Unit = {
              recordConsumer.addLong(LongCodec.fromMsgPack(msgpack))
            }
          }
        case PrimitiveTypeName.BOOLEAN =>
          new ParquetCodec(tpe, index, codec) {
            override protected def writeValue(recordConsumer: RecordConsumer, msgpack: MsgPack): Unit = {
              recordConsumer.addBoolean(BooleanCodec.fromMsgPack(msgpack))
            }
          }
        case PrimitiveTypeName.FLOAT =>
          new ParquetCodec(tpe, index, codec) {
            override protected def writeValue(recordConsumer: RecordConsumer, msgpack: MsgPack): Unit = {
              recordConsumer.addFloat(FloatCodec.fromMsgPack(msgpack))
            }
          }
        case PrimitiveTypeName.DOUBLE =>
          new ParquetCodec(tpe, index, codec) {
            override protected def writeValue(recordConsumer: RecordConsumer, msgpack: MsgPack): Unit = {
              recordConsumer.addDouble(DoubleCodec.fromMsgPack(msgpack))
            }
          }
        case PrimitiveTypeName.BINARY if tpe.getLogicalTypeAnnotation == stringType =>
          new ParquetCodec(tpe, index, codec) {
            override protected def writeValue(recordConsumer: RecordConsumer, msgpack: MsgPack): Unit = {
              recordConsumer.addBinary(Binary.fromString(StringCodec.fromMsgPack(msgpack)))
            }
          }
        case _ =>
          new ParquetCodec(tpe, index, codec) {
            override protected def writeValue(recordConsumer: RecordConsumer, msgpack: MsgPack): Unit = {
              recordConsumer.addBinary(Binary.fromConstantByteArray(msgpack))
            }
          }
      }
    } else {
      new ParquetCodec(tpe, index, codec) {
        override protected def writeValue(recordConsumer: RecordConsumer, msgpack: MsgPack): Unit = {
          recordConsumer.addBinary(Binary.fromConstantByteArray(msgpack))
        }
      }
    }
  }

}

class AirframeParquetWriteSupport[A](surface: Surface) extends WriteSupport[A] with LogSupport {
  private lazy val schema: MessageType = Parquet.toParquetSchema(surface)
  private val parquetCodec: Seq[(Parameter, ParquetCodec)] =
    surface.params.zip(schema.getFields.asScala).map { case (param, tpe) =>
      val codec = MessageCodec.ofSurface(param.surface)
      (param, AirframeParquetWriter.parquetCodecOf(tpe, param.index, codec))
    }

  private var recordConsumer: RecordConsumer = null
  import scala.jdk.CollectionConverters._

  override def init(configuration: Configuration): WriteSupport.WriteContext = {
    trace(s"schema: ${schema}")
    val extraMetadata: Map[String, String] = Map.empty
    new WriteContext(schema, extraMetadata.asJava)
  }

  override def prepareForWrite(recordConsumer: RecordConsumer): Unit = {
    this.recordConsumer = recordConsumer
  }

  override def write(record: A): Unit = {
    require(recordConsumer != null)
    try {
      recordConsumer.startMessage()
      parquetCodec.foreach { case (param, pc) =>
        val v = param.get(record)
        v match {
          case None if param.surface.isOption =>
          // Skip writing Optional parameter
          case _ =>
            pc.write(recordConsumer, v)
        }
      }
    } finally {
      recordConsumer.endMessage()
    }
  }
}

private[parquet] object ParquetCodec {}
