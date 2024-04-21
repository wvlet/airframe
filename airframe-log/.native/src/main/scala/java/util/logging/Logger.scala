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
package java.util.logging

abstract class Handler extends AutoCloseable {
  def publish(record: LogRecord): Unit

  def flush(): Unit
}

/**
  * Implements java.util.logging.Logger interface, which is not avaialble in Scala Native
  * @param name
  */
class Logger(parent: Option[Logger], name: String) {
  private var handlers             = List.empty[Handler]
  private var useParentHandlers    = true
  private var level: Option[Level] = None

  def getName(): String = name

  def log(level: Level, msg: String): Unit =
    log(LogRecord(level, msg))

  def log(record: LogRecord): Unit = {
    if (isLoggable(record.getLevel())) {
      if (record.getLoggerName() == null) record.setLoggerName(name)
      if (parent.nonEmpty && useParentHandlers) {
        getParent().log(record)
      } else {
        handlers.foreach { h => h.publish(record) }
      }
    }
  }

  def isLoggable(level: Level): Boolean = {
    val l = getLevel()
    if (level.intValue() < l.intValue()) false else true
  }

  def getParent(): Logger =
    parent.getOrElse(null)

  def getLevel(): Level =
    level.orElse(parent.map(_.getLevel())).getOrElse(Level.INFO)

  def setLevel(newLevel: Level): Unit =
    level = Some(newLevel)

  def resetLogLevel(): Unit =
    level = None

  def setUseParentHandlers(useParentHandlers: Boolean): Unit =
    this.useParentHandlers = useParentHandlers

  def addHandler(h: Handler): Unit =
    handlers = h :: handlers

  def removeHandler(h: Handler): Unit =
    handlers = handlers.filter(_ != h)

  def getHandlers: Array[Handler] = handlers.toArray
}

object Logger {

  import scala.jdk.CollectionConverters.*

  private val loggerTable = new java.util.concurrent.ConcurrentHashMap[String, Logger]().asScala
  private val rootLogger  = Logger(None, "")

  def getLogger(name: String): Logger = {
    loggerTable.get(name) match {
      case Some(logger) => logger
      case None =>
        val logger = newLogger(name)
        synchronized {
          loggerTable.put(name, logger)
        }
        logger
    }
  }

  private def newLogger(name: String): Logger = {
    name match {
      case null | "" => rootLogger
      case other =>
        val parentName   = name.substring(0, name.lastIndexOf('.').max(0))
        val parentLogger = getLogger(parentName)
        Logger(Some(parentLogger), name)
    }
  }
}

abstract class Formatter {
  def format(record: LogRecord): String
}

class LogRecord(_level: Level, msg: String) extends Serializable {
  private val millis            = System.currentTimeMillis()
  private var loggerName        = ""
  private var thrown: Throwable = null

  def getMessage(): String = msg

  def getMillis(): Long = millis

  def getLoggerName(): String = loggerName

  def getLevel(): Level = _level

  def getThrown(): Throwable = thrown

  def setLoggerName(name: String): Unit =
    this.loggerName = name

  def setThrown(e: Throwable): Unit =
    thrown = e
}
