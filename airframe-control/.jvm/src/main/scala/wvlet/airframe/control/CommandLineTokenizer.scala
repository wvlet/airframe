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

import scala.util.parsing.combinator.RegexParsers

/**
  * Tokenize single string representations of command line arguments into Array[String]
  */
object CommandLineTokenizer extends RegexParsers with LogSupport {
  private def unquote(s: String): String = s.substring(1, s.length() - 1)

  def stringLiteral: Parser[String] =
    ("\"" + """([^"\p{Cntrl}\\]|\\[\\/\\"bfnrt]|\\u[a-fA-F0-9]{4})*""" + "\"").r ^^ { unquote(_) }
  def quotation: Parser[String] =
    ("'" + """([^'\p{Cntrl}\\]|\\[\\/\\"bfnrt]|\\u[a-fA-F0-9]{4})*""" + "'").r ^^ { unquote(_) }
  def other: Parser[String]        = """([^\"'\s]+)""".r
  def token: Parser[String]        = stringLiteral | quotation | other
  def tokens: Parser[List[String]] = rep(token)

  def tokenize(line: String): Array[String] = {
    val p = parseAll(tokens, line)
    val r = p match {
      case Success(result, next) => result
      case Error(msg, next) => {
        warn(msg)
        List.empty
      }
      case Failure(msg, next) => {
        warn(msg)
        List.empty
      }
    }
    r.toArray
  }
}
