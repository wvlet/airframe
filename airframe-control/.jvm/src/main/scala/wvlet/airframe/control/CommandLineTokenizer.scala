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
package wvlet.airframe.control
import wvlet.log.LogSupport

/**
  * Tokenize single string representations of command line arguments into Array[String]
  */
object CommandLineTokenizer extends LogSupport {
  private val DOUBLE_QUOTED_LITERAL = ("\"" + """([^"\p{Cntrl}\\]|\\[\\/\\"bfnrt]|\\u[a-fA-F0-9]{4})*""" + "\"").r
  private val SINGLE_QUOTED_LITERAL = ("'" + """([^'\p{Cntrl}\\]|\\[\\/\\"bfnrt]|\\u[a-fA-F0-9]{4})*""" + "'").r
  private val REGULAR_TOKEN         = """([^\"'\s]+)""".r

  private def unquote(s: String): String = s.substring(1, s.length() - 1)

  def tokenize(line: String): Array[String] = {
    parse(0, line).toArray
  }

  private def parse(pos: Int, line: String): List[String] = {
    var cursor = pos

    def skipWhiteSpaces: Unit = {
      var toContinue = true
      while (toContinue && cursor < line.length) {
        val ch = line.charAt(cursor)
        ch match {
          case ' ' | '\t' | '\n' | '\r' =>
            cursor += 1
          case _ =>
            toContinue = false
        }
      }
    }

    def parseToken: List[String] = {
      REGULAR_TOKEN.findPrefixMatchOf(line.substring(cursor)) match {
        case Some(m) =>
          val token = m.matched
          token :: parse(cursor + token.length, line)
        case None =>
          throw new IllegalArgumentException(s"Failed to parse token. pos:${pos}\n${line}")
      }
    }

    skipWhiteSpaces
    if (cursor >= line.length) {
      Nil
    } else {
      val ch = line.charAt(cursor)
      ch match {
        case '\'' =>
          // Parse single quoted token
          SINGLE_QUOTED_LITERAL.findPrefixMatchOf(line.substring(cursor)) match {
            case Some(m) =>
              val token = m.matched
              unquote(token) :: parse(cursor + token.length, line)
            case None =>
              parseToken
          }
        case '"' =>
          // Parse double quoted token
          DOUBLE_QUOTED_LITERAL.findPrefixMatchOf(line.substring(cursor)) match {
            case Some(m) =>
              val token = m.matched
              unquote(token) :: parse(cursor + token.length, line)
            case None =>
              parseToken
          }
        case _ =>
          parseToken
      }
    }
  }
}
