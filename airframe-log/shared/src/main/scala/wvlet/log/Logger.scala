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

import java.util.concurrent.ConcurrentHashMap
import java.util.{Properties, logging => jl}

import scala.annotation.tailrec
import scala.language.experimental.macros
import scala.reflect.ClassTag

/**
  * An wrapper of java.util.logging.Logger for supporting rich-format logging
  *
  * @param wrapped
  */
class Logger(
    private val name: String,
    /**
      * Since java.util.logging.Logger is non-serializable, we need to find the logger instance after deserialization.
      * If wrapped is null, _log method will find or create the logger instance.
      */
    @transient private[log] var wrapped: jl.Logger
) extends LoggerBase
    with Serializable {

  private def _log = {
    if (wrapped == null) {
      wrapped = jl.Logger.getLogger(name)
    }
    wrapped
  }

  def getName = name

  def getLogLevel: LogLevel = {
    @tailrec
    def getLogLevelOf(l: jl.Logger): LogLevel = {
      if (l == null) {
        LogLevel.INFO
      } else {
        val jlLevel = l.getLevel
        if (jlLevel != null) {
          LogLevel(jlLevel)
        } else {
          getLogLevelOf(l.getParent)
        }
      }
    }
    getLogLevelOf(_log)
  }

  def setLogLevel(l: LogLevel): Unit = {
    _log.setLevel(l.jlLevel)
  }

  def setFormatter(formatter: LogFormatter): Unit = {
    resetHandler(new ConsoleLogHandler(formatter))
  }

  def resetHandler(h: jl.Handler): Unit = {
    clearHandlers
    _log.addHandler(h)
    setUseParentHandlers(false)
  }

  def getParent: Option[Logger] = {
    Option(wrapped.getParent).map(x => Logger(x.getName))
  }

  def addHandler(h: jl.Handler): Unit = {
    _log.addHandler(h)
  }

  def setUseParentHandlers(use: Boolean): Unit = {
    _log.setUseParentHandlers(use)
  }

  def clear: Unit = {
    clearHandlers
    resetLogLevel
  }

  def clearHandlers: Unit = {
    synchronized {
      for (lst <- Option(_log.getHandlers); h <- lst) {
        _log.removeHandler(h)
      }
    }
  }

  /**
    * Clean up all handlers including this and parent, ancestor loggers
    */
  def clearAllHandlers: Unit = {
    var l: Option[Logger] = Some(this)
    synchronized {
      while (l.isDefined) {
        l.map { x =>
          x.clearHandlers
          l = x.getParent
        }
      }
    }
  }

  def getHandlers: Seq[jl.Handler] = {
    wrapped.getHandlers.toSeq
  }

  def resetLogLevel: Unit = {
    _log.setLevel(null)
  }

  def isEnabled(level: LogLevel): Boolean = {
    _log.isLoggable(level.jlLevel)
  }

  def log(record: wvlet.log.LogRecord): Unit = {
    record.setLoggerName(name)
    _log.log(record)
  }

  def log(level: LogLevel, source: LogSource, message: Any): Unit = {
    log(wvlet.log.LogRecord(level, source, formatLog(message)))
  }

  def logWithCause(level: LogLevel, source: LogSource, message: Any, cause: Throwable): Unit = {
    log(wvlet.log.LogRecord(level, source, formatLog(message), cause))
  }

  protected def isMultiLine(str: String) = str.contains("\n")

  protected def formatLog(message: Any): String = {
    val formatted = message match {
      case null         => ""
      case e: Error     => LogFormatter.formatStacktrace(e)
      case e: Exception => LogFormatter.formatStacktrace(e)
      case _            => message.toString
    }

    if (isMultiLine(formatted)) {
      s"\n${formatted}"
    } else {
      formatted
    }
  }

  /**
    * Suppress warning messages (i.e., setting ERROR log level during the code block).
    * Useful for exception testing
    */
  def suppressWarnings[U](f: => U): U = {
    val prev = getLogLevel
    try {
      setLogLevel(LogLevel.ERROR)
      f
    } finally {
      setLogLevel(prev)
    }
  }
}

object Logger {
  // Set a custom LogManager to show log messages even in shutdown hooks
  sys.props.put("java.util.logging.manager", "wvlet.log.AirframeLogManager")

  import scala.jdk.CollectionConverters._

  private lazy val loggerCache = new ConcurrentHashMap[String, Logger].asScala

  lazy val rootLogger = {
    val l = initLogger(name = "", handlers = Seq(LogEnv.defaultHandler))
    if (LogEnv.isScalaJS) {
      l.setLogLevel(LogLevel.INFO)
    }
    l
  }

  /**
    * Create a new java.util.logging.Logger
    *
    * @param name
    * @param level
    * @param handlers
    * @param useParents
    * @return
    */
  private[log] def initLogger(
      name: String,
      level: Option[LogLevel] = None,
      handlers: Seq[jl.Handler] = Seq.empty,
      useParents: Boolean = true
  ): Logger = {
    val logger = Logger.apply(name)
    logger.clearHandlers
    level.foreach(l => logger.setLogLevel(l))
    handlers.foreach(h => logger.addHandler(h))
    logger.setUseParentHandlers(useParents)
    logger
  }

  /**
    * Create a logger corresponding to a class
    *
    * @tparam A
    * @return
    */
  def of[A: ClassTag]: Logger = {
    apply(implicitly[ClassTag[A]].runtimeClass.getName)
  }

  def apply(loggerName: String): Logger = {
    loggerCache.getOrElseUpdate(loggerName, new Logger(loggerName, jl.Logger.getLogger(loggerName)))
  }

  def getDefaultLogLevel: LogLevel = rootLogger.getLogLevel

  def setDefaultLogLevel(level: LogLevel): Unit = {
    rootLogger.setLogLevel(level)
  }

  def setDefaultFormatter(formatter: LogFormatter): Unit = {
    synchronized {
      rootLogger.resetHandler(new ConsoleLogHandler(formatter))
    }
  }

  def setDefaultHandler(handler: jl.Handler): Unit = {
    rootLogger.resetHandler(handler)
  }

  def resetDefaultLogLevel: Unit = {
    rootLogger.resetLogLevel
  }

  def clearAllHandlers: Unit = {
    rootLogger.clearAllHandlers
  }

  def init: Unit = {
    clearAllHandlers
    resetDefaultLogLevel
    rootLogger.resetHandler(LogEnv.defaultHandler)
  }

  /**
    * Set log levels using Properties (key: logger name, value: log level)
    *
    * @param logLevels
    */
  def setLogLevels(logLevels: Properties): Unit = {
    for ((loggerName, level) <- logLevels.asScala) {
      LogLevel.unapply(level) match {
        case Some(lv) =>
          Logger(loggerName).setLogLevel(lv)
        case None =>
          Console.err.println(s"Unknown loglevel ${level} is specified for ${loggerName}")
      }
    }
  }

  def scheduleLogLevelScan: Unit = { LogEnv.scheduleLogLevelScan }
  def stopScheduledLogLevelScan: Unit = { LogEnv.stopScheduledLogLevelScan }

  /**
    * Scan the default log level file only once. To periodically scan, use scheduleLogLevelScan
    */
  def scanLogLevels: Unit = { LogEnv.scanLogLevels }

  /**
    * Scan the specified log level file
    *
    * @param loglevelFileCandidates
    */
  def scanLogLevels(loglevelFileCandidates: Seq[String]): Unit = {
    LogEnv.scanLogLevels(loglevelFileCandidates)
  }
}
