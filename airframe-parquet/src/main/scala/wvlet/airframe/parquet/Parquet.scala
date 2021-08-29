package wvlet.airframe.parquet

import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.Path
import org.apache.parquet.filter2.compat.FilterCompat
import org.apache.parquet.hadoop.util.HadoopInputFile
import org.apache.parquet.hadoop.{ParquetFileReader, ParquetReader, ParquetWriter}
import org.apache.parquet.schema.LogicalTypeAnnotation.stringType
import org.apache.parquet.schema.PrimitiveType.PrimitiveTypeName
import org.apache.parquet.schema.Types.PrimitiveBuilder
import org.apache.parquet.schema.{MessageType, PrimitiveType, Type, Types}
import wvlet.airframe.control.Control.withResource
import wvlet.airframe.surface.Primitive.PrimitiveSurface
import wvlet.airframe.surface.{OptionSurface, Parameter, Primitive, Surface}
import wvlet.log.LogSupport

import scala.jdk.CollectionConverters._
import scala.reflect.runtime.{universe => ru}

object Parquet extends LogSupport {

  /**
    * Create a Parquet writer that accepts records represented in Map, Array, JSON, MsgPack, etc.
    * @param path
    * @param schema
    * @param hadoopConf
    * @param config
    * @return
    */
  def newRecordWriter(
      path: String,
      schema: MessageType,
      hadoopConf: Configuration = new Configuration(),
      config: AirframeParquetWriter.RecordWriterBuilder => AirframeParquetWriter.RecordWriterBuilder =
        identity[AirframeParquetWriter.RecordWriterBuilder](_)
  ): ParquetWriter[Any] = {
    val b       = AirframeParquetWriter.recordWriterBuilder(path, schema, hadoopConf)
    val builder = config(b)
    builder.build()
  }

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

  def readStatistics(path: String, hadoopConf: Configuration = new Configuration()): Map[String, ColumnStatistics] = {
    val input = HadoopInputFile.fromPath(new Path(path), hadoopConf)
    ParquetStatsReader.readStatistics(input)
  }

  def toParquetSchema(surface: Surface): MessageType = {

    def toParquetPrimitiveTypeName(s:PrimitiveSurface): PrimitiveTypeName = {
      s match {
        case Primitive.Int | Primitive.Short | Primitive.Byte =>
          PrimitiveTypeName.INT32
        case Primitive.Long =>
          PrimitiveTypeName.INT64
        case Primitive.Float =>
          PrimitiveTypeName.FLOAT
        case Primitive.Double =>
          PrimitiveTypeName.DOUBLE
        case Primitive.String | Primitive.Char =>
          PrimitiveTypeName.BINARY
        case Primitive.Boolean =>
          PrimitiveTypeName.BOOLEAN
        case _ =>
          ???
    }

    def toParquetPrimitive(
        s: PrimitiveSurface,
        rep: Option[Type.Repetition] = None
    ): PrimitiveBuilder[PrimitiveType] = {
      val repetition = rep.getOrElse(Type.Repetition.OPTIONAL)
      val typeName = toParquetPrimitiveTypeName(s)
      s match {
        case Primitive.String | Primitive.Char =>
          Types.primitive(typeName, repetition).as(stringType)
        case _ =>
          Types.primitive(typeName, repetition)
      }
    }

    def toParquetType(s: Surface, name: String, rep: Option[Type.Repetition] = None): Type = {
      val repetition = rep.getOrElse(Type.Repetition.OPTIONAL)
      s match {
        case p: PrimitiveSurface =>
          toParquetPrimitive(p, rep).named(name)
        case o: OptionSurface =>
          toParquetType(o.elementSurface, name, Some(Type.Repetition.OPTIONAL))
        case s: Surface if s.isSeq || s.isArray =>
          val elementSurface = s.typeArgs(0)
          toParquetType(elementSurface, name, Some(Type.Repetition.REPEATED))
        case m: Surface if s.isMap =>
          val keySurface   = m.typeArgs(0)
          val valueSurface = m.typeArgs(1)
          keySurface match {
            case p: PrimitiveSurface =>
              val keyType = toParquetPrimitive(p).named("key").getPrimitiveTypeName
              val mapType = Types.map(rep.getOrElse(Type.Repetition.REPEATED)).key(keyType)
              valueSurface match {
                case vp: PrimitiveSurface =>
                  val valueType = toParquetPrimitiveTypeName(vp)
                  mapType.optionalValue(valueType).named(name)
                case other =>
                  mapType.optionalValue()

              }

          }

        case _ =>
          // TODO Support Array/Seq/Map types. Just use MsgPack binary here
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
