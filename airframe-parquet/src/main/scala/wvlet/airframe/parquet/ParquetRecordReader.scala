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

import org.apache.parquet.io.api.{Binary, Converter, GroupConverter, PrimitiveConverter, RecordMaterializer}
import org.apache.parquet.schema.{GroupType, MessageType}
import org.apache.parquet.schema.PrimitiveType.PrimitiveTypeName
import org.apache.parquet.schema.LogicalTypeAnnotation.stringType
import wvlet.airframe.codec.MessageCodec
import wvlet.airframe.codec.PrimitiveCodec.ValueCodec
import wvlet.airframe.surface.Surface
import wvlet.log.LogSupport
import scala.jdk.CollectionConverters._

object ParquetRecordReader extends LogSupport {
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

  case class ParentContext(paramName: String, recordBuilder: RecordBuilder)

}

import ParquetRecordReader._

class ParquetRecordReader[A](
    surface: Surface,
    projectedSchema: GroupType,
    parentContext: Option[ParentContext] = None
) extends GroupConverter
    with LogSupport {
  private val codec         = MessageCodec.ofSurface(surface)
  private val recordBuilder = RecordBuilder.newBuilder

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
      case _ if surface.isMap =>
        // Mapping Parquet columns to non-object types (e.g., Map[String, Any])
        if (f.isPrimitive) {
          new MsgPackConverter(f.getName, recordBuilder)
        } else {
          // Mapping Parquet group types to non-object types
          new ParquetRecordReader(
            Surface.of[Map[String, Any]],
            f.asGroupType(),
            parentContext = Some(ParentContext(f.getName, recordBuilder))
          )
        }
      case _ =>
        // GroupConverter for nested objects
        surface.params.find(_.name == f.getName) match {
          case Some(param) =>
            if (param.surface.isOption || param.surface.isSeq || param.surface.isArray) {
              // For Option[X], Seq[X] types, extract X
              val elementSurface = param.surface.typeArgs(0)
              new ParquetRecordReader(
                param.surface,
                ParquetSchema.toParquetSchema(elementSurface),
                parentContext = Some(ParentContext(f.getName, recordBuilder))
              )
            } else {
              new ParquetRecordReader(
                param.surface,
                ParquetSchema.toParquetSchema(param.surface),
                parentContext = Some(ParentContext(f.getName, recordBuilder))
              )
            }
          case None =>
            ???
        }
    }
    cv
  }.toIndexedSeq

  def currentRecord: A = {
    val m = recordBuilder.toMap
    codec.fromMap(m).asInstanceOf[A]
  }

  override def getConverter(fieldIndex: Int): Converter = converters(fieldIndex)

  override def start(): Unit = {
    recordBuilder.clear()
  }

  override def end(): Unit = {
    parentContext.foreach { ctx =>
      val m = recordBuilder.toMap
      ctx.recordBuilder.add(ctx.paramName, m)
    }
  }
}

/**
  * An adapter class for org.apache.parquet.RecordMaterializer
  */
class ParquetRecordMaterializer[A](surface: Surface, projectedSchema: MessageType) extends RecordMaterializer[A] {
  private val recordConverter = new ParquetRecordReader[A](surface, projectedSchema)

  override def getCurrentRecord: A = recordConverter.currentRecord

  override def getRootConverter: GroupConverter = recordConverter
}
