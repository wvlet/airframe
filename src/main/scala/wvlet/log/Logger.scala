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
import java.util.logging._
import java.util.{logging => jl}

import wvlet.log.LogFormatter.AppLogFormatter

import scala.annotation.tailrec
import scala.language.experimental.macros
import scala.reflect.ClassTag

/**
  * An wrapper of java.util.logging.Logger for supporting rich-format logging
  *
  * @param wrapped
  */
class Logger(wrapped: jl.Logger) extends Serializable {

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

  def getName = wrapped.getName

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

  def resetHandler(h: Handler) {
    clearHandlers
    wrapped.addHandler(h)
    setUseParentHandlers(false)
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
    log(LogRecord(level, source, formatLog(message), cause))
  }

  private def isMultiLine(str: String) = str.contains("\n")

  def formatLog(message: Any): String = {
    val formatted = message match {
      case null => ""
      case e: Error => LogFormatter.formatStacktrace(e)
      case e: Exception => LogFormatter.
                           formatStacktrace(e)
      case _ => message.toString
    }

    if (isMultiLine(formatted)) {
      s"\n${formatted}"
    }
    else {
      formatted
    }
  }

}

object Logger {

  import collection.JavaConverters._

  private val loggerCache = new ConcurrentHashMap[String, Logger].asScala

  val rootLogger = initLogger(
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
  def initLogger(name: String,
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

  def of[A](implicit tag:ClassTag[A]) : Logger = {
    apply(tag.runtimeClass.getName)
  }

  def apply(loggerName: String): Logger = {
    loggerCache.getOrElseUpdate(loggerName, new Logger(jl.Logger.getLogger(loggerName)))
  }

  def getDefaultLogLevel: LogLevel = rootLogger.getLogLevel

  def setDefaultLogLevel(level: LogLevel) {
    rootLogger.setLogLevel(level)
  }

  def setDefaultFormatter(formatter: LogFormatter) {
    rootLogger.resetHandler(new ConsoleLogHandler(formatter))
  }

  def resetDefaultLogLevel {
    rootLogger.resetLogLevel
  }
}

