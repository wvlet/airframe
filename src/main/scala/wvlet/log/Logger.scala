package wvlet.log

import java.util.concurrent.ConcurrentHashMap
import java.util.logging._
import java.util.{logging => jl}

import wvlet.log.LogFormatter.AppLogFormatter

import scala.annotation.tailrec

/**
  * An wrapper of java.util.logging.Logger for supporting rich-format logging
  *
  * @param wrapped
  */
class Logger(wrapped: jl.Logger) extends PublicLoggingMethods {

  protected[this] def logger = this

  def getLogLevel: LogLevel = {
    @tailrec
    def getLogLevelOf(l: jl.Logger): LogLevel = {
      if (l == null) {
        LogLevel.INFO
      }
      else {
        val jlLevel = l.getLevel
        if (jlLevel != null) {
          LogLevel(jlLevel)
        }
        else {
          getLogLevelOf(l.getParent)
        }
      }
    }
    getLogLevelOf(wrapped)
  }

  def setLogLevel(l: LogLevel) {
    wrapped.setLevel(l.jlLevel)
  }

  def addHandler(h: Handler) {
    wrapped.addHandler(h)
  }

  def setUseParentHandlers(use: Boolean) {
    wrapped.setUseParentHandlers(use)
  }

  def clear {
    clearHandlers
    resetLogLevel
  }

  def clearHandlers {
    for (lst <- Option(wrapped.getHandlers); h <- lst) {
      wrapped.removeHandler(h)
    }
  }

  def resetLogLevel {
    wrapped.setLevel(null)
  }

  def isEnabled(level: LogLevel): Boolean = {
    wrapped.isLoggable(level.jlLevel)
  }

  def log(record: LogRecord) {
    record.setLoggerName(wrapped.getName)
    wrapped.log(record)
  }

  def log(level: LogLevel, source: LogSource, message: Any) {
    log(LogRecord(level, source, formatLog(message)))
  }

  def logWithCause(level: LogLevel, source: LogSource, message: Any, cause: Throwable) {
    log(LogRecord(level, source, formatLog(message), Some(cause)))
  }

  def formatLog(message: Any): String = {
    message match {
      case null => ""
      case e: Error => LogFormatter.formatStacktrace(e)
      case e: Exception => LogFormatter.formatStacktrace(e)
      case _ => message.toString
    }
  }

}

object Logger {

  import collection.JavaConverters._

  private val loggerCache = new ConcurrentHashMap[String, Logger].asScala

  val rootLogger = getLogger(
    name = "",
    handlers = Seq(new ConsoleLogHandler(AppLogFormatter)))

  /**
    * Create a new {@link java.util.logging.Logger}
    *
    * @param name
    * @param level
    * @param handlers
    * @param useParents
    * @return
    */
  def getLogger(name: String,
                level: Option[LogLevel] = None,
                handlers: Seq[Handler] = Seq.empty,
                useParents: Boolean = true
               ): Logger = {

    val logger = Logger.apply(name)
    logger.clearHandlers
    level.foreach(l => logger.setLogLevel(l))
    handlers.foreach(h => logger.addHandler(h))
    logger.setUseParentHandlers(useParents)
    logger
  }

  def apply(loggerName: String): Logger = {
    loggerCache.getOrElseUpdate(loggerName, new Logger(jl.Logger.getLogger(loggerName)))
  }

  def getDefaultLogLevel: LogLevel = rootLogger.getLogLevel

  def setDefaultLogLevel(level: LogLevel) {
    rootLogger.setLogLevel(level)
  }

  def resetDefaultLogLevel {
    rootLogger.resetLogLevel
  }
}

