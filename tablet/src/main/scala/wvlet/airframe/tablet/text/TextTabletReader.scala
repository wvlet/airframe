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
package wvlet.airframe.tablet.text

import java.io.{File, InputStream}

import com.github.tototoshi.csv.CSVReader
import wvlet.airframe.tablet.{Record, StringArrayRecord, TabletReader}

import scala.io.Source

trait TextTabletReader extends TabletReader {
  def close
}

/**
  *
  */
class TSVTabletReader(source: Source) extends TextTabletReader {
  private val lines = source.getLines()

  override def close {
    source.close()
  }

  def read: Option[Record] = {
    if (!lines.hasNext) {
      close
      None
    } else {
      val line = lines.next()
      val cols = line.split("\t")
      Some(StringArrayRecord(if (cols == null) Seq.empty[String] else cols))
    }
  }
}

object TSVTabletReader {
  def apply(in: InputStream): TSVTabletReader = new TSVTabletReader(Source.fromInputStream(in))
  def apply(file: File): TSVTabletReader      = new TSVTabletReader(Source.fromFile(file))
}

class CSVTabletReader(source: Source) extends TextTabletReader {
  private val reader                       = CSVReader.open(source)
  private val lines: Iterator[Seq[String]] = reader.iterator

  override def close {
    reader.close()
  }

  def read: Option[Record] = {
    if (!lines.hasNext) {
      close
      None
    } else {
      val cols: Seq[String] = lines.next()
      Some(StringArrayRecord(cols))
    }
  }
}

object CSVTabletReader {
  def apply(in: InputStream): CSVTabletReader = new CSVTabletReader(Source.fromInputStream(in))
  def apply(file: File): CSVTabletReader      = new CSVTabletReader(Source.fromFile(file))
}
