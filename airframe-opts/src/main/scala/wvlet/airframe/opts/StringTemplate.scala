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

package wvlet.airframe.opts
import scala.collection.immutable.WrappedString
import scala.collection.{AbstractIterator, Iterator}

//--------------------------------------
//
// StringTemplate.scala
// Since: 2012/01/16 9:33
//
//--------------------------------------

object StringTemplate {

  def eval(template: String)(properties: Map[Any, String]) = new StringTemplate(template).eval(properties)

  private final val LF             = 0x0A
  private final val FF             = 0x0C
  private def isLineBreak(c: Char) = c == LF || c == FF

  def linesOf(str: String): Iterator[String] = {
    linesWithSeparators(str).map(line => new WrappedString(line).stripLineEnd)
  }

  // A copy of linesIterator implementation of StringLike.scala
  // TODO: Use String.linesIterator after Scala 2.13.0-RC1 is released.
  private def linesWithSeparators(str: String): Iterator[String] = new AbstractIterator[String] {
    private val len      = str.length
    private var index    = 0
    def hasNext: Boolean = index < len
    def next(): String = {
      if (index >= len) throw new NoSuchElementException("next on empty iterator")
      val start = index
      while (index < len && !isLineBreak(str.charAt(index))) index += 1
      index += 1
      str.substring(start, index min len)
    }
  }
}

/**
  * @author leo
  */
class StringTemplate(template: String) {

  def eval(property: Map[Any, String]): String = apply(property)

  private def convert(properties: Map[Any, String]): Map[Symbol, String] = {
    (for ((k, v) <- properties) yield {
      k match {
        case s: Symbol => s                  -> v.toString
        case _         => Symbol(k.toString) -> v.toString
      }
    }).toMap
  }

  def apply(property: Map[Any, String]): String = {
    val p = convert(property)

    val pattern = """(^\$|[^\\]\$)[^\$]+\$""".r
    val out     = new StringBuilder

    for ((line, lineCount) <- StringTemplate.linesOf(template).zipWithIndex) {
      if (lineCount > 0) {
        out.append("\n")
      }

      var cursor = 0
      for (m <- pattern.findAllIn(line).matchData) {
        val prefixSize = m.group(1).length() - 1
        val varStart   = m.start + prefixSize
        val varEnd     = m.end
        val key        = line.substring(varStart + 1, varEnd - 1)
        if (cursor < varStart) {
          out.append(line.substring(cursor, varStart))
          cursor = varEnd
        }

        val k = Symbol(key)
        if (p.contains(k)) {
          out.append(p(k))
        }
        cursor = varEnd;
      }

      if (cursor < line.length) {
        out.append(line.substring(cursor, line.length))
      }
    }
    out.toString
  }

}
