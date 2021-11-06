package wvlet.airframe.parquet

import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.Path
import org.apache.parquet.filter2.compat.FilterCompat
import org.apache.parquet.hadoop.util.HadoopInputFile
import org.apache.parquet.hadoop.{ParquetFileReader, ParquetReader, ParquetWriter}
import org.apache.parquet.schema.MessageType
import wvlet.airframe.control.Control.withResource
import wvlet.airframe.surface.Surface
import wvlet.log.LogSupport

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

  def toParquetSchema(s: Surface): MessageType = ParquetSchema.toParquetSchema(s)
}
