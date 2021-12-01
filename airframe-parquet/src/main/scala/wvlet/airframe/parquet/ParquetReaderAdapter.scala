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
import org.apache.parquet.schema.MessageType
import wvlet.airframe.surface.{CName, Surface}

import java.util
import scala.jdk.CollectionConverters._

object ParquetReaderAdapter {

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
      new ParquetReadSupportAdapter[A](surface, plan)
    }
  }
}

class ParquetReadSupportAdapter[A](surface: Surface, plan: Option[ParquetQueryPlan]) extends ReadSupport[A] {
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
    new ParquetRecordMaterializer[A](surface, readContext.getRequestedSchema)
  }
}
