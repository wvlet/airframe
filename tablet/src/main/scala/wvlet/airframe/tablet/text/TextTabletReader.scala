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

import wvlet.airframe.tablet.text.TextTabletWriter.{CSVRecordFormatter, TSVRecordFormatter}
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
      val cols = line.split("\t").map(x => TSVRecordFormatter.unescape(x))
      Some(StringArrayRecord(if (cols == null) Seq.empty[String] else cols))
    }
  }
}

object TSVTabletReader {
  def apply(source: Source): TSVTabletReader  = new TSVTabletReader(source)
  def apply(in: InputStream): TSVTabletReader = apply(Source.fromInputStream(in))
  def apply(file: File): TSVTabletReader      = apply(Source.fromFile(file))
}

class CSVTabletReader(source: Source) extends TextTabletReader {
  // TODO handle CSV properly
  private val lines: Iterator[Seq[String]] = source.getLines().map(line => line.split(",").toSeq)

  override def close {
    source.close()
  }

  def read: Option[Record] = {
    if (!lines.hasNext) {
      close
      None
    } else {
      val cols: Seq[String] = lines.next().map(x => CSVRecordFormatter.unescape(x))
      Some(StringArrayRecord(cols))
    }
  }
}

object CSVTabletReader {
  def apply(source: Source)                   = new CSVTabletReader(source)
  def apply(in: InputStream): CSVTabletReader = apply(Source.fromInputStream(in))
  def apply(file: File): CSVTabletReader      = apply(Source.fromFile(file))
}
