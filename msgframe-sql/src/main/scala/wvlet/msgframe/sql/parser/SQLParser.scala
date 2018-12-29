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
package wvlet.msgframe.sql.parser

import org.antlr.v4.runtime.{CharStreams, CommonTokenStream, Token}
import wvlet.log.LogSupport
import wvlet.msgframe.sql.model.SQLModel

/**
  *
  */
object SQLParser extends LogSupport {

  def parse(sql: String): SQLModel = {
    trace(s"parse: ${sql}")
    val parser = new SqlBaseParser(tokenStream(sql))
    val ctx    = parser.singleStatement()
    trace(ctx.toStringTree(parser))
    val interpreter = new SQLInterpreter
    interpreter.interpret(ctx)
  }

  def tokenStream(sql: String): CommonTokenStream = {
    val lexer       = new SqlBaseLexer(new CaseInsensitiveStream(CharStreams.fromString(sql)))
    val tokenStream = new CommonTokenStream(lexer)
    tokenStream
  }

  def tokenName(t: Token): String = {
    SqlBaseParser.VOCABULARY.getDisplayName(t.getType)
  }
}
