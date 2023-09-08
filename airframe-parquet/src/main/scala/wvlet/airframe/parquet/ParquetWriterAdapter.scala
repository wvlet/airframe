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
import org.apache.parquet.hadoop.api.WriteSupport
import org.apache.parquet.hadoop.api.WriteSupport.WriteContext
import org.apache.parquet.hadoop.metadata.CompressionCodecName
import org.apache.parquet.hadoop.util.HadoopOutputFile
import org.apache.parquet.hadoop.{ParquetFileWriter, ParquetWriter}
import org.apache.parquet.io.OutputFile
import org.apache.parquet.io.api.RecordConsumer
import org.apache.parquet.schema.MessageType
import wvlet.airframe.surface.Surface
import wvlet.log.LogSupport

import scala.jdk.CollectionConverters.*

/**
  */
object ParquetWriterAdapter extends LogSupport {
  def builder[A](surface: Surface, path: String, conf: Configuration): Builder[A] = {
    val fsPath = new Path(path)
    val file   = HadoopOutputFile.fromPath(fsPath, conf)
    val b      = new Builder[A](surface, file).withConf(conf)
    // Use snappy by default
    b.withCompressionCodec(CompressionCodecName.SNAPPY)
      .withWriteMode(ParquetFileWriter.Mode.OVERWRITE)
  }

  class Builder[A](surface: Surface, file: OutputFile) extends ParquetWriter.Builder[A, Builder[A]](file: OutputFile) {
    override def self(): Builder[A] = this
    override def getWriteSupport(conf: Configuration): WriteSupport[A] = {
      new ParquetWriteSupportAdapter[A](surface)
    }
  }

  class RecordWriterBuilder(schema: MessageType, file: OutputFile, knownSurfaces: Seq[Surface])
      extends ParquetWriter.Builder[Any, RecordWriterBuilder](file: OutputFile) {
    override def self(): RecordWriterBuilder = this
    override def getWriteSupport(conf: Configuration): WriteSupport[Any] = {
      new ParquetRecordWriterSupportAdapter(schema, knownSurfaces)
    }
  }

  def recordWriterBuilder(
      path: String,
      schema: MessageType,
      knownSurfaces: Seq[Surface],
      conf: Configuration
  ): RecordWriterBuilder = {
    val fsPath = new Path(path)
    val file   = HadoopOutputFile.fromPath(fsPath, conf)
    val b      = new RecordWriterBuilder(schema, file, knownSurfaces).withConf(conf)
    // Use snappy by default
    b.withCompressionCodec(CompressionCodecName.SNAPPY)
      .withWriteMode(ParquetFileWriter.Mode.OVERWRITE)
  }

}

class ParquetWriteSupportAdapter[A](surface: Surface) extends WriteSupport[A] with LogSupport {
  private lazy val schema = Parquet.toParquetSchema(surface)
  private val objectCodec: ParquetObjectWriter = {
    ParquetObjectWriter.buildFromSurface(surface, schema).asRoot
  }

  private var recordConsumer: RecordConsumer = null
  import scala.jdk.CollectionConverters.*

  override def init(configuration: Configuration): WriteSupport.WriteContext = {
    val extraMetadata: Map[String, String] = Map.empty
    new WriteContext(schema, extraMetadata.asJava)
  }

  override def prepareForWrite(recordConsumer: RecordConsumer): Unit = {
    this.recordConsumer = recordConsumer
  }

  override def write(record: A): Unit = {
    require(recordConsumer != null)
    objectCodec.write(recordConsumer, record)
  }
}

class ParquetRecordWriterSupportAdapter(schema: MessageType, knownSurfaces: Seq[Surface])
    extends WriteSupport[Any]
    with LogSupport {
  private var recordConsumer: RecordConsumer = null

  override def init(configuration: Configuration): WriteContext = {
    trace(s"schema: ${schema}")
    new WriteContext(schema, Map.empty[String, String].asJava)
  }

  override def prepareForWrite(recordConsumer: RecordConsumer): Unit = {
    this.recordConsumer = recordConsumer
  }

  private val codec = new ParquetRecordWriter(schema, knownSurfaces)

  override def write(record: Any): Unit = {
    require(recordConsumer != null)

    codec.pack(record, recordConsumer)
  }
}
