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
package wvlet.airframe.sql.parser

/**
  * Reading SQL texts in case-insensitive manner
  */
import org.antlr.v4.runtime.CharStream
import org.antlr.v4.runtime.IntStream
import org.antlr.v4.runtime.misc.Interval

class CaseInsensitiveStream(val stream: CharStream) extends CharStream {
  override def getText(interval: Interval): String = stream.getText(interval)
  override def consume(): Unit = {
    stream.consume()
  }
  override def LA(i: Int): Int = {
    val result = stream.LA(i)
    result match {
      case 0 =>
        result
      case IntStream.EOF =>
        result
      case _ =>
        Character.toUpperCase(result)
    }
  }
  override def mark: Int = stream.mark
  override def release(marker: Int): Unit = {
    stream.release(marker)
  }
  override def index: Int = stream.index
  override def seek(index: Int): Unit = {
    stream.seek(index)
  }
  override def size: Int             = stream.size
  override def getSourceName: String = stream.getSourceName
}
