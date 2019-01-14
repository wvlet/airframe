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

import org.antlr.v4.runtime.{DefaultErrorStrategy, RecognitionException, _}
import wvlet.log.LogSupport
import wvlet.msgframe.sql.model.LogicalPlan

/**
  * SQL -> Token -> ANTLR parzse tree -> LogicalPlan
  */
object SQLParser extends LogSupport {

  private def createLexerErrorListener = new BaseErrorListener {
    override def syntaxError(recognizer: Recognizer[_, _],
                             offendingSymbol: Any,
                             line: Int,
                             charPositionInLine: Int,
                             msg: String,
                             e: RecognitionException): Unit = {
      throw new SQLParseError(msg, line, charPositionInLine, e)
    }
  }

  def parse(sql: String): LogicalPlan = {
    trace(s"parse: ${sql}")
    val parser = new SqlBaseParser(tokenStream(sql))

    // Do not drop mismatched token
    parser.setErrorHandler(new DefaultErrorStrategy {
      override def recoverInline(recognizer: Parser): Token =
        if (nextTokensContext == null) {
          throw new InputMismatchException(recognizer)
        } else {
          throw new InputMismatchException(recognizer, nextTokensState, nextTokensContext)
        }
    })
    parser.removeErrorListeners()
    parser.addErrorListener(createLexerErrorListener)

    val ctx = parser.singleStatement()
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

class SQLParseError(message: String, val line: Int, val pos: Int, cause: RecognitionException)
    extends Exception(s"Parse error at line:${line}, pos:${pos}. ${message}", cause) {}
