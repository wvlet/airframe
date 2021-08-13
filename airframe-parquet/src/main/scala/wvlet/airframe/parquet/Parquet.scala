package wvlet.airframe.parquet

import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.Path
import org.apache.parquet.filter2.compat.FilterCompat
import org.apache.parquet.hadoop.util.HadoopInputFile
import org.apache.parquet.hadoop.{ParquetFileReader, ParquetReader, ParquetWriter}
import org.apache.parquet.schema.LogicalTypeAnnotation.stringType
import org.apache.parquet.schema.PrimitiveType.PrimitiveTypeName
import org.apache.parquet.schema.{MessageType, Type, Types}
import wvlet.airframe.control.Control.withResource
import wvlet.airframe.json.Json
import wvlet.airframe.surface.{OptionSurface, Parameter, Primitive, Surface}
import wvlet.log.LogSupport

import scala.jdk.CollectionConverters._
import scala.reflect.runtime.{universe => ru}

object Parquet extends LogSupport {

  def newWriter[A: ru.TypeTag](
      path: String,
      // Hadoop filesystem specific configuration, e.g., fs.s3a.access.key
      hadoopConf: Configuration = new Configuration(),
      config: AirframeParquetWriter.Builder[A] => AirframeParquetWriter.Builder[A] =
        identity[AirframeParquetWriter.Builder[A]](_)
  ): ParquetWriter[A] = {
    val b       = AirframeParquetWriter.builder[A](path, hadoopConf)
    val builder = config(b)
    builder.build()
  }

  def newReader[A: ru.TypeTag](
      path: String,
      // Hadoop filesystem specific configuration, e.g., fs.s3a.access.key
      hadoopConf: Configuration = new Configuration(),
      config: ParquetReader.Builder[A] => ParquetReader.Builder[A] = identity[ParquetReader.Builder[A]](_)
  ): ParquetReader[A] = {
    val b: ParquetReader.Builder[A] = AirframeParquetReader.builder[A](path, hadoopConf)
    config(b).build()
  }

  def query[A: ru.TypeTag](
      path: String,
      sql: String,
      hadoopConf: Configuration = new Configuration(),
      config: ParquetReader.Builder[A] => ParquetReader.Builder[A] = identity[ParquetReader.Builder[A]](_)
  ): ParquetReader[A] = {
    // Read Parquet schema for resolving column types
    val schema                      = readSchema(path)
    val plan                        = ParquetQueryPlanner.parse(sql, schema)
    val b: ParquetReader.Builder[A] = AirframeParquetReader.builder[A](path, conf = hadoopConf, plan = Some(plan))

    val newConf = plan.predicate match {
      case Some(pred) =>
        // Set Parquet filter
        config(b).withFilter(FilterCompat.get(pred))
      case _ =>
        config(b)
    }
    newConf.build()
  }

  def readSchema(path: String, hadoopConf: Configuration = new Configuration()): MessageType = {
    val input = HadoopInputFile.fromPath(new Path(path), hadoopConf)
    withResource(ParquetFileReader.open(input)) { reader =>
      reader.getFooter.getFileMetaData.getSchema
    }
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
