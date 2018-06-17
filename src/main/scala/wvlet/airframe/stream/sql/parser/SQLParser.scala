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
package wvlet.airframe.stream.sql.parser

import org.antlr.v4.runtime.{CharStreams, CommonTokenStream}
import wvlet.log.LogSupport

/**
  *
  */
object SQLParser extends LogSupport {

  def parse(sql: String): Unit = {
    debug(s"parse: ${sql}")
    val lexer       = new SqlBaseLexer(new CaseInsensitiveStream(CharStreams.fromString(sql)))
    val tokenStream = new CommonTokenStream(lexer)
    val parser      = new SqlBaseParser(tokenStream)
    val ctx         = parser.singleStatement()
    debug(ctx.toStringTree(parser))
    val interpreter = new SQLInterpreter
    interpreter.interpret(ctx)
  }

}
