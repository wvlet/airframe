package wvlet.log

import java.io.{ByteArrayOutputStream, PrintStream}

import scala.language.experimental.macros

/**
  * Use this if you need a local logger instance in your class
  */
trait LazyLogger {
  protected[this] lazy val logger: Logger = Logger.getLogger(this.getClass.getName)
}

/**
  * This addds logging methods (error, warn, info, debug and trace) to your class
  */
trait LogSupport extends LoggingMethods with LazyLogger

/**
  * If you need to initialize the logger instance upon instance initialization, use this trait
  */
trait StrictLogSupport extends LoggingMethods {
  protected[this] val logger: Logger = Logger.getLogger(this.getClass.getName)
}

trait LoggingMethods extends Serializable {
  import LogMacros._

  protected[this] def logger : Logger

  protected def error(message: Any): Unit = macro errorLog
  protected def error(message: Any, cause: Throwable): Unit = macro errorLogWithCause

  protected def warn(message: Any): Unit = macro warnLog
  protected def warn(message: Any, cause: Throwable): Unit = macro warnLogWithCause

  protected def info(message: Any): Unit = macro infoLog
  protected def info(message: Any, cause: Throwable): Unit = macro infoLogWithCause

  protected def debug(message: Any): Unit = macro debugLog
  protected def debug(message: Any, cause: Throwable): Unit = macro debugLogWithCause

  protected def trace(message: Any): Unit = macro traceLog
  protected def trace(message: Any, cause: Throwable): Unit = macro traceLogWithCause
}

trait PublicLoggingMethods extends Serializable {
  import LogMacros._

  protected[this] def logger : Logger

  def error(message: Any): Unit = macro errorLog
  def error(message: Any, cause: Throwable): Unit = macro errorLogWithCause

  def warn(message: Any): Unit = macro warnLog
  def warn(message: Any, cause: Throwable): Unit = macro warnLogWithCause

  def info(message: Any): Unit = macro infoLog
  def info(message: Any, cause: Throwable): Unit = macro infoLogWithCause

  def debug(message: Any): Unit = macro debugLog
  def debug(message: Any, cause: Throwable): Unit = macro debugLogWithCause

  def trace(message: Any): Unit = macro traceLog
  def trace(message: Any, cause: Throwable): Unit = macro traceLogWithCause
}

