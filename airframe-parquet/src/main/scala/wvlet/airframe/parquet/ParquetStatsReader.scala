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

import org.apache.parquet.column.statistics.Statistics
import org.apache.parquet.hadoop.ParquetFileReader
import org.apache.parquet.hadoop.util.HadoopInputFile
import org.apache.parquet.io.api.Binary
import org.apache.parquet.schema.{LogicalTypeAnnotation, Type}
import wvlet.log.LogSupport

import java.util.concurrent.ConcurrentHashMap
import scala.jdk.CollectionConverters._

case class ColumnStatistics(
    numNulls: Option[Long] = None,
    compressedSize: Option[Long] = None,
    uncompressedSize: Option[Long] = None,
    minValue: Option[Any] = None,
    maxValue: Option[Any] = None
)

/**
  */
object ParquetStatsReader extends LogSupport {

  class ColumnMetric {
    private val table = new ConcurrentHashMap[String, Long]().asScala

    def get(columnName: String): Option[Long] = {
      table.get(columnName)
    }

    def add(columnName: String, value: Long): Unit = {
      // TODO: Use updateWith when deprecating Scala 2.12 support
//      table.updateWith(columnName){ prev: Option[Long] =>
//        prev.getOrElse(0) + value
//      }
      val newValue = table.getOrElse(columnName, 0L) + value
      table.put(columnName, newValue)
    }
  }

  def readStatistics(inputFile: HadoopInputFile): Map[String, ColumnStatistics] = {
    val reader   = ParquetFileReader.open(inputFile)
    val metadata = reader.getFooter

    var missingStatsColumn = Set.empty[String]
    var rowCount           = 0L
    val schema             = metadata.getFileMetaData.getSchema
    // Prepare statistics holder
    val columnStats = schema.getColumns.asScala
      .map { col =>
        val colName = col.getPath.mkString(".")
        val tpe     = col.getPrimitiveType
        colName -> Statistics.createStats(tpe)
      // We need to specify explicit map element types for Scala 2.12
      }.toMap[String, Statistics[_]]

    val uncompressedSize = new ColumnMetric()
    val compressedSize   = new ColumnMetric()

    // Read each Parquet block
    for (block <- metadata.getBlocks.asScala) {
      rowCount += block.getRowCount
      for (columnMetaData <- block.getColumns.asScala) {
        val columnName = columnMetaData.getPath.toDotString
        uncompressedSize.add(columnName, columnMetaData.getTotalUncompressedSize)
        compressedSize.add(columnName, columnMetaData.getTotalSize)

        trace(s"Reading stats of ${columnName}: ${columnMetaData.getPrimitiveType}")
        // TODO: We may need to use our own types
        Option(columnMetaData.getStatistics) match {
          case None =>
            warn(s"Missing statistics in ${columnName}")
            missingStatsColumn += columnName
          case Some(stats) =>
            columnStats.get(columnName).foreach { currentStats =>
              // Update stats (Statistics object is mutable)
              currentStats.mergeStatistics(stats)
            }
        }
      }
    }

    // Report column stats unless the stats is missing
    val columnStatistics = for ((columnName, stats) <- columnStats if !missingStatsColumn.contains(columnName)) yield {
      columnName -> ColumnStatistics(
        numNulls = Some(stats.getNumNulls),
        compressedSize = compressedSize.get(columnName),
        uncompressedSize = uncompressedSize.get(columnName),
        minValue = convert(stats.`type`(), stats.genericGetMin()),
        maxValue = convert(stats.`type`(), stats.genericGetMax())
      )
    }
    columnStatistics.toMap
  }

  private def convert(parquetType: Type, minMax: Any): Option[Any] = {
    minMax match {
      case b: Binary if parquetType.getLogicalTypeAnnotation == LogicalTypeAnnotation.stringType() =>
        Some(b.toStringUsingUTF8)
      case b: Binary =>
        Some(b.getBytes)
      case other =>
        Some(other)
    }
  }
}
