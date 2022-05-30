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

object Parquet extends ParquetCompat with LogSupport {

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
      config: ParquetWriterAdapter.RecordWriterBuilder => ParquetWriterAdapter.RecordWriterBuilder =
        identity[ParquetWriterAdapter.RecordWriterBuilder](_)
  ): ParquetWriter[Any] = {
    val b       = ParquetWriterAdapter.recordWriterBuilder(path, schema, hadoopConf)
    val builder = config(b)
    builder.build()
  }

  def newObjectWriter[A](
      objectSurface: Surface,
      path: String,
      // Hadoop filesystem specific configuration, e.g., fs.s3a.access.key
      hadoopConf: Configuration = new Configuration(),
      config: ParquetWriterAdapter.Builder[A] => ParquetWriterAdapter.Builder[A] =
        identity[ParquetWriterAdapter.Builder[A]](_)
  ): ParquetWriter[A] = {
    val b       = ParquetWriterAdapter.builder[A](objectSurface, path, hadoopConf)
    val builder = config(b)
    builder.build()
  }

  def newObjectReader[A](
      objectSurface: Surface,
      path: String,
      // Hadoop filesystem specific configuration, e.g., fs.s3a.access.key
      hadoopConf: Configuration = new Configuration(),
      config: ParquetReader.Builder[A] => ParquetReader.Builder[A] = identity[ParquetReader.Builder[A]](_)
  ): ParquetReader[A] = {
    val b: ParquetReader.Builder[A] = ParquetReaderAdapter.builder[A](objectSurface, path, hadoopConf)
    config(b).build()
  }

  def queryObject[A](
      objectSurface: Surface,
      path: String,
      sql: String,
      hadoopConf: Configuration = new Configuration(),
      config: ParquetReader.Builder[A] => ParquetReader.Builder[A] = identity[ParquetReader.Builder[A]](_)
  ): ParquetReader[A] = {
    // Read Parquet schema for resolving column types
    val schema = readSchema(path)
    val plan   = ParquetQueryPlanner.parse(sql, schema)
    val b: ParquetReader.Builder[A] =
      ParquetReaderAdapter.builder[A](objectSurface, path, conf = hadoopConf, plan = Some(plan))

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
