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
import wvlet.airframe.surface.{CName, Surface}
import wvlet.log.LogSupport

import java.util
import scala.collection.generic.Growable
import scala.jdk.CollectionConverters._

object AirframeParquetReader {

  def builder[A](
      surface: Surface,
      path: String,
      conf: Configuration,
      plan: Option[ParquetQueryPlan] = None
  ): Builder[A] = {
    val fsPath  = new Path(path)
    val file    = HadoopInputFile.fromPath(fsPath, conf)
    val builder = new Builder[A](surface, file, plan)
    builder.withConf(conf).asInstanceOf[Builder[A]]
  }

  class Builder[A](surface: Surface, inputFile: InputFile, plan: Option[ParquetQueryPlan])
      extends ParquetReader.Builder[A](inputFile) {
    override def getReadSupport(): ReadSupport[A] = {
      new AirframeParquetReadSupport[A](surface, plan)
    }
  }
}

class AirframeParquetReadSupport[A](surface: Surface, plan: Option[ParquetQueryPlan]) extends ReadSupport[A] {
  override def init(context: InitContext): ReadSupport.ReadContext = {
    val parquetFileSchema = context.getFileSchema
    val targetColumns = plan match {
      case Some(p) if p.projectedColumns.nonEmpty =>
        p.projectedColumns.map(c => CName(c).canonicalName).toSet
      case _ =>
        surface.params.map(p => CName(p.name).canonicalName).toSet
    }
    if (targetColumns.isEmpty) {
      // e.g., Json, Map where all parameters need to be extracted
      new ReadContext(parquetFileSchema)
    } else {
      // Pruning columns
      val projectedColumns =
        parquetFileSchema.getFields.asScala.filter(f => targetColumns.contains(CName(f.getName).canonicalName))
      val resultSchema = new MessageType(surface.fullName, projectedColumns.asJava)
      new ReadContext(resultSchema)
    }
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

class AirframeParquetRecordMaterializer[A](surface: Surface, projectedSchema: MessageType)
    extends RecordMaterializer[A] {
  private val recordConverter = new ParquetRecordConverter[A](surface, projectedSchema)

  override def getCurrentRecord: A = recordConverter.currentRecord

  override def getRootConverter: GroupConverter = recordConverter
}

object ParquetRecordConverter {
  private class IntConverter(fieldName: String, holder: RecordBuilder) extends PrimitiveConverter {
    override def addInt(value: Int): Unit = {
      holder.add(fieldName, value)
    }
  }
  private class LongConverter(fieldName: String, holder: RecordBuilder) extends PrimitiveConverter {
    override def addLong(value: Long): Unit = {
      holder.add(fieldName, value)
    }
  }
  private class BooleanConverter(fieldName: String, holder: RecordBuilder) extends PrimitiveConverter {
    override def addBoolean(value: Boolean): Unit = {
      holder.add(fieldName, value)
    }
  }
  private class StringConverter(fieldName: String, holder: RecordBuilder) extends PrimitiveConverter with LogSupport {
    override def addBinary(value: Binary): Unit = {
      holder.add(fieldName, value.toStringUsingUTF8)
    }
  }
  private class FloatConverter(fieldName: String, holder: RecordBuilder) extends PrimitiveConverter {
    override def addFloat(value: Float): Unit = {
      holder.add(fieldName, value)
    }
  }
  private class DoubleConverter(fieldName: String, holder: RecordBuilder) extends PrimitiveConverter {
    override def addDouble(value: Double): Unit = {
      holder.add(fieldName, value)
    }
  }
  private class MsgPackConverter(fieldName: String, holder: RecordBuilder) extends PrimitiveConverter {
    override def addBinary(value: Binary): Unit = {
      holder.add(fieldName, ValueCodec.fromMsgPack(value.getBytes))
    }
  }
}

class ParquetRecordConverter[A](surface: Surface, projectedSchema: MessageType) extends GroupConverter with LogSupport {
  private val codec         = MessageCodec.ofSurface(surface)
  private val recordBuilder = RecordBuilder.newBuilder

  import ParquetRecordConverter._

  private val converters: Seq[Converter] = projectedSchema.getFields.asScala.map { f =>
    val cv: Converter = f match {
      case p if p.isPrimitive =>
        p.asPrimitiveType().getPrimitiveTypeName match {
          case PrimitiveTypeName.INT32   => new IntConverter(f.getName, recordBuilder)
          case PrimitiveTypeName.INT64   => new LongConverter(f.getName, recordBuilder)
          case PrimitiveTypeName.BOOLEAN => new BooleanConverter(f.getName, recordBuilder)
          case PrimitiveTypeName.FLOAT   => new FloatConverter(f.getName, recordBuilder)
          case PrimitiveTypeName.DOUBLE  => new DoubleConverter(f.getName, recordBuilder)
          case PrimitiveTypeName.BINARY if p.getLogicalTypeAnnotation == stringType =>
            new StringConverter(f.getName, recordBuilder)
          case PrimitiveTypeName.BINARY =>
            new MsgPackConverter(f.getName, recordBuilder)
          case _ => ???
        }
      case _ =>
        // TODO Support nested types
        ???
    }
    cv
  }.toIndexedSeq

  def currentRecord: A = {
    val m = recordBuilder.toMap
    trace(m)
    codec.fromMap(m).asInstanceOf[A]
  }

  override def getConverter(fieldIndex: Int): Converter = converters(fieldIndex)

  override def start(): Unit = {
    recordBuilder.clear()
  }

  override def end(): Unit = {}
}
