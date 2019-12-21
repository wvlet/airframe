package wvlet.log

trait LoggingMethods extends Serializable {

  protected def error(message: Any): Unit                   = {}
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
