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
package wvlet.airframe.parquet.io

import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.Path
import org.apache.parquet.column.ParquetProperties
import org.apache.parquet.hadoop.{ParquetFileWriter, ParquetWriter}
import org.apache.parquet.hadoop.api.WriteSupport
import org.apache.parquet.hadoop.metadata.CompressionCodecName
import org.apache.parquet.hadoop.util.HadoopOutputFile
import org.apache.parquet.io.OutputFile
import org.apache.parquet.io.api.RecordConsumer
import wvlet.airframe.surface.Surface

import scala.reflect.runtime.{universe => ru}

/**
  */

object AirframeParquetWriter {
  //def newWriter[A: ru.TypeTag]: Builder[A] = Builder[A](surface = Surface.of[A])

  def builder[A: ru.TypeTag](path: String): Builder[A] = {
    val s      = Surface.of[A]
    val fsPath = new Path(path)
    val conf   = new Configuration()
    val file   = HadoopOutputFile.fromPath(fsPath, conf)
    new Builder[A](s, file)
  }

  class Builder[A](val surface: Surface, file: OutputFile)
      extends ParquetWriter.Builder[A, Builder[A]](file: OutputFile) {
    override def self(): Builder[A] = this
    override def getWriteSupport(conf: Configuration): WriteSupport[A] = {
      new AirframeParquetWriteSupport[A](surface)
    }
  }
}

class AirframeParquetWriteSupport[A](surface: Surface) extends WriteSupport[A] {
  private var recordConsumer: RecordConsumer = null

  override def init(configuration: Configuration): WriteSupport.WriteContext = {
    ???
  }

  override def prepareForWrite(recordConsumer: RecordConsumer): Unit = {
    this.recordConsumer = recordConsumer
  }

  override def write(record: A): Unit = {
    require(recordConsumer != null)

  }
}
