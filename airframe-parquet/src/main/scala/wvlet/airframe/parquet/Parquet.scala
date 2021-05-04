package wvlet.airframe.parquet

import org.apache.parquet.hadoop.ParquetWriter
import org.apache.parquet.schema.LogicalTypeAnnotation.stringType
import org.apache.parquet.schema.PrimitiveType.PrimitiveTypeName
import org.apache.parquet.schema.{ConversionPatterns, MessageType, PrimitiveType, Type, Types}
import wvlet.airframe.surface.{ArraySurface, OptionSurface, Parameter, Primitive, Surface}

import scala.jdk.CollectionConverters._
import scala.reflect.runtime.{universe => ru}

object Parquet {

  def writer[A: ru.TypeTag](
      path: String,
      config: AirframeParquetWriter.Builder[A] => AirframeParquetWriter.Builder[A] =
        identity[AirframeParquetWriter.Builder[A]](_)
  ): ParquetWriter[A] = {
    val b       = AirframeParquetWriter.builder[A](path)
    val builder = config(b)
    builder.build()
  }

  def toParquetSchema(surface: Surface): MessageType = {
    def toParquetType(s: Surface, name: String, rep: Option[Type.Repetition] = None): Type = {
      val repetition = rep.getOrElse(Type.Repetition.OPTIONAL)
      s match {
        case Primitive.Int | Primitive.Short =>
          Types.primitive(PrimitiveTypeName.INT32, repetition).named(name)
        case Primitive.Long =>
          Types.primitive(PrimitiveTypeName.INT64, repetition).named(name)
        case Primitive.Float =>
          Types.primitive(PrimitiveTypeName.FLOAT, repetition).named(name)
        case Primitive.Double =>
          Types.primitive(PrimitiveTypeName.DOUBLE, repetition).named(name)
        case Primitive.String =>
          Types.primitive(PrimitiveTypeName.BINARY, repetition).as(stringType).named(name)
        case Primitive.Boolean =>
          Types.primitive(PrimitiveTypeName.BOOLEAN, repetition).named(name)
        case o: OptionSurface =>
          toParquetType(o.elementSurface, name, Some(Type.Repetition.OPTIONAL))
        case _ =>
          // TODO Support Array/Seq/Map types. Just usg MsgPack binaary
          Types.primitive(PrimitiveTypeName.BINARY, repetition).named(name)
      }
    }

    def toType(p: Parameter): Type = {
      toParquetType(p.surface, p.name, if (p.isRequired) Some(Type.Repetition.REQUIRED) else None)
    }

    new MessageType(
      surface.fullName,
      surface.params.map(p => toType(p)).toList.asJava
    )
  }

}
