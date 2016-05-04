package wvlet.log

import java.io.{ByteArrayOutputStream, PrintStream}
import java.util.logging._
import java.util.{logging => jl}

import wvlet.log.LogFormatter.AppLogFormatter



object Logger {

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
               ): jl.Logger = {
    val logger = jl.Logger.getLogger(name)
    logger.clearHandlers
    level.foreach(l => logger.setLevel(l.jlLevel))
    handlers.foreach(h => logger.addHandler(h))
    logger.setUseParentHandlers(useParents)
    logger
  }

  def apply(loggerName:String) = {
    jl.Logger.getLogger(loggerName)
  }

  def getDefaultLogLevel : LogLevel = {
    rootLogger.getLevel match {
      case null => LogLevel.INFO
      case level => LogLevel(level)
    }
  }

  def setDefaultLogLevel(level:LogLevel) {
    rootLogger.setLevel(level.jlLevel)
  }

  def resetDefaultLogLevel {
    rootLogger.resetLogLevel
  }

  implicit class RichLogger(logger: jl.Logger) {

    def setLogLevel(l: LogLevel) {
      logger.setLevel(l.jlLevel)
    }

    def clear {
      clearHandlers
      resetLogLevel
    }

    def clearHandlers {
      for (lst <- Option(logger.getHandlers); h <- lst) {
        logger.removeHandler(h)
      }
    }

    def resetLogLevel {
      logger.setLevel(null)
    }

    def isEnabled(level: LogLevel): Boolean = {
      logger.isLoggable(level.jlLevel)
    }

    def log(record: LogRecord) {
      record.setLoggerName(logger.getName)
      logger.log(record)
    }
  }
}

