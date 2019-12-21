package wvlet.log

import scala.language.experimental.macros

trait PublicLogMethods {
  import LogMacros._

  def error(message: Any): Unit = macro errorLogMethod
  def error(message: Any, cause: Throwable): Unit = macro errorLogMethodWithCause

  def warn(message: Any): Unit = macro warnLogMethod
  def warn(message: Any, cause: Throwable): Unit = macro warnLogMethodWithCause

  def info(message: Any): Unit = macro infoLogMethod
  def info(message: Any, cause: Throwable): Unit = macro infoLogMethodWithCause

  def debug(message: Any): Unit = macro debugLogMethod
  def debug(message: Any, cause: Throwable): Unit = macro debugLogMethodWithCause

  def trace(message: Any): Unit = macro traceLogMethod
  def trace(message: Any, cause: Throwable): Unit = macro traceLogMethodWithCause
}
