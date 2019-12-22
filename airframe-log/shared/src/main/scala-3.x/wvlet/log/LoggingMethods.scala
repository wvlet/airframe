package wvlet.log

import scala.language.experimental.macros
import scala.quoted._
import scala.tasty._

trait LoggingMethods extends Serializable {

  protected def error(message: Any): Unit = {

  }

  protected def error(message: Any, cause: Throwable): Unit = {}

  protected def warn(message: Any): Unit                   = {}
  protected def warn(message: Any, cause: Throwable): Unit = {}

  protected def info(message: Any): Unit                   = {}
  protected def info(message: Any, cause: Throwable): Unit = {}

  protected def debug(message: Any): Unit                   = {}
  protected def debug(message: Any, cause: Throwable): Unit = {}

  protected def trace(message: Any): Unit                   = {}
  protected def trace(message: Any, cause: Throwable): Unit = {}

  protected def logAt(logLevel: LogLevel, message: Any): Unit = {}
}

object LogMacros {

  def logImpl(message:Expr[Any])(given qctx: QuoteContext): Expr[Unit] = {
    import qctx.tasty.{_, given}
    val pos = rootPosition
    val line = pos.startLine
    '{ () }
  }

}
