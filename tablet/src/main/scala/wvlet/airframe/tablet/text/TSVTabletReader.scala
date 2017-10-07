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

import wvlet.airframe.tablet.{Record, StringArrayRecord}

import scala.io.Source

/**
  *
  */
class TSVTabletReader(file: String) extends TextTabletReader {
  private val source = Source.fromFile(file)
  private val lines  = source.getLines()

  def close {
    source.close()
  }

  def read: Option[Record] = {
    if (!lines.hasNext) {
      close
      None
    } else {
      val line = lines.next()
      val cols = line.split("\t")
      Some(StringArrayRecord(if (cols == null) Array.empty[String] else cols))
    }
  }
}
