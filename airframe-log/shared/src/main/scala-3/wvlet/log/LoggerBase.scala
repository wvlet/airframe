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
package wvlet.log

/**
  */
trait LoggerBase {

  def error(message: Any): Unit                   = ???
  def error(message: Any, cause: Throwable): Unit = ???
  def warn(message: Any): Unit                    = ???
  def warn(message: Any, cause: Throwable): Unit  = ???

  def info(message: Any): Unit                   = ???
  def info(message: Any, cause: Throwable): Unit = ???

  def debug(message: Any): Unit                   = ???
  def debug(message: Any, cause: Throwable): Unit = ???
  def trace(message: Any): Unit                   = ???
  def trace(message: Any, cause: Throwable): Unit = ???

  def isEnabled(level: LogLevel): Boolean
  def log(level: LogLevel, source: LogSource, message:Any): Unit

  import scala.quoted._
  private def logImpl(level: Expr[wvlet.log.LogLevel], message:Expr[Any])(using q: Quotes): Expr[Unit] = {
    import q.reflect._
    val pos = Position.ofMacroExpansion
    val line: Int = pos.startLine
    val column : Int = pos.endColumn
    val src = pos.sourceFile
    val path: java.nio.file.Path = src.jpath
    val l: Logger = this.asInstanceOf[Logger]

    //'{ if(${Expr(l)}.isEnabled(${level})) ${Expr(l)}.log(${level}, wvlet.log.LogSource(${Expr(path.toFile().getPath())}, ${Expr(path.getFileName().toString)}, ${Expr(line)}, ${Expr(column)}), $message) }
    '{ println(wvlet.log.LogSource(${Expr(path.toFile().getPath())}, ${Expr(path.getFileName().toString)}, ${Expr(line)}, ${Expr(column)})) }

  }

}

/**
  */
trait LoggingMethods extends Serializable {

  protected def error(message: Any): Unit                     = ???
  protected def error(message: Any, cause: Throwable): Unit   = ???
  protected def warn(message: Any): Unit                      = ???
  protected def warn(message: Any, cause: Throwable): Unit    = ???
  protected def info(message: Any): Unit                      = ???
  protected def info(message: Any, cause: Throwable): Unit    = ???
  protected def debug(message: Any): Unit                     = ???
  protected def debug(message: Any, cause: Throwable): Unit   = ???
  protected def trace(message: Any): Unit                     = ???
  protected def trace(message: Any, cause: Throwable): Unit   = ???
  protected def logAt(logLevel: LogLevel, message: Any): Unit = ???
}

object LogMacros {


}
