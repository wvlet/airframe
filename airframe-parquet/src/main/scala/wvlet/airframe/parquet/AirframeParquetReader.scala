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
import org.apache.parquet.hadoop.ParquetReader
import org.apache.parquet.hadoop.api.ReadSupport.ReadContext
import org.apache.parquet.hadoop.api.{InitContext, ReadSupport}
import org.apache.parquet.hadoop.util.HadoopInputFile
import org.apache.parquet.io.InputFile
import org.apache.parquet.io.api._
import org.apache.parquet.schema.LogicalTypeAnnotation.stringType
import org.apache.parquet.schema.MessageType
import org.apache.parquet.schema.PrimitiveType.PrimitiveTypeName
import wvlet.airframe.codec.MessageCodec
import wvlet.airframe.codec.PrimitiveCodec.ValueCodec
import wvlet.airframe.surface.Surface
import wvlet.log.LogSupport

import java.util
import scala.collection.generic.Growable
import scala.jdk.CollectionConverters._
import scala.reflect.runtime.{universe => ru}

private[parquet] object AirframeParquetReader {

  def builder[A: ru.TypeTag](path: String): Builder[A] = {
    val conf   = new Configuration()
    val fsPath = new Path(path)
    val file   = HadoopInputFile.fromPath(fsPath, conf)
    new Builder[A](Surface.of[A], file)
  }

  class Builder[A](surface: Surface, inputFile: InputFile) extends ParquetReader.Builder[A](inputFile) {
    override def getReadSupport(): ReadSupport[A] = {
      new AirframeParquetReadSupport[A](surface)
    }
  }
}

class AirframeParquetReadSupport[A](surface: Surface) extends ReadSupport[A] {
  private val schema = Parquet.toParquetSchema(surface)

  override def init(context: InitContext): ReadSupport.ReadContext = {
    // do nothing
    new ReadContext(context.getFileSchema)
  }

  override def prepareForRead(
      configuration: Configuration,
      keyValueMetaData: util.Map[String, String],
      fileSchema: MessageType,
      readContext: ReadSupport.ReadContext
  ): RecordMaterializer[A] = {
    new AirframeParquetRecordMaterializer[A](surface, readContext.getRequestedSchema)
  }
}

class AirframeParquetRecordMaterializer[A](surface: Surface, parquetSchema: MessageType) extends RecordMaterializer[A] {
  private val recordConverter = new ParquetRecordConverter[A](surface, parquetSchema)

  override def getCurrentRecord: A = recordConverter.currentRecord

  override def getRootConverter: GroupConverter = recordConverter
}

object ParquetRecordConverter {

  type Holder = Growable[(String, Any)]

  private class IntConverter(fieldName: String, holder: Holder) extends PrimitiveConverter {
    override def addInt(value: Int): Unit = {
      holder += fieldName -> value
    }
  }
  private class LongConverter(fieldName: String, holder: Holder) extends PrimitiveConverter {
    override def addLong(value: Long): Unit = {
      holder += fieldName -> value
    }
  }
  private class BooleanConverter(fieldName: String, holder: Holder) extends PrimitiveConverter {
    override def addBoolean(value: Boolean): Unit = {
      holder += fieldName -> value
    }
  }
  private class StringConverter(fieldName: String, holder: Holder) extends PrimitiveConverter {
    override def addBinary(value: Binary): Unit = {
      holder += fieldName -> value.toStringUsingUTF8
    }
  }
  private class FloatConverter(fieldName: String, holder: Holder) extends PrimitiveConverter {
    override def addFloat(value: Float): Unit = {
      holder += fieldName -> value
    }
  }
  private class DoubleConverter(fieldName: String, holder: Holder) extends PrimitiveConverter {
    override def addDouble(value: Double): Unit = {
      holder += fieldName -> value
    }
  }
  private class MsgPackConverter(fieldName: String, holder: Holder) extends PrimitiveConverter {
    override def addBinary(value: Binary): Unit = {
      holder += fieldName -> ValueCodec.fromMsgPack(value.getBytes)
    }
  }

}

class ParquetRecordConverter[A](surface: Surface, parquetSchema: MessageType) extends GroupConverter with LogSupport {
  private val codec        = MessageCodec.ofSurface(surface)
  private val recordHolder = Map.newBuilder[String, Any]

  import ParquetRecordConverter._

  private val converters: Seq[Converter] = parquetSchema.getFields.asScala.zipWithIndex.map { case (f, i) =>
    f match {
      case p if p.isPrimitive =>
        p.asPrimitiveType().getPrimitiveTypeName match {
          case PrimitiveTypeName.INT32   => new IntConverter(f.getName, recordHolder)
          case PrimitiveTypeName.INT64   => new LongConverter(f.getName, recordHolder)
          case PrimitiveTypeName.BOOLEAN => new BooleanConverter(f.getName, recordHolder)
          case PrimitiveTypeName.FLOAT   => new FloatConverter(f.getName, recordHolder)
          case PrimitiveTypeName.DOUBLE  => new DoubleConverter(f.getName, recordHolder)
          case PrimitiveTypeName.BINARY if p.getLogicalTypeAnnotation == stringType =>
            new StringConverter(f.getName, recordHolder)
          case PrimitiveTypeName.BINARY =>
            new MsgPackConverter(f.getName, recordHolder)
          case _ => ???
        }
      case _ => ???
    }
  }

  def currentRecord: A = {
    val m = recordHolder.result()
    trace(m)
    codec.fromMap(m).asInstanceOf[A]
  }

  override def getConverter(fieldIndex: Int): Converter = converters(fieldIndex)

  override def start(): Unit = {
    recordHolder.clear()
  }

  override def end(): Unit = {}
}
