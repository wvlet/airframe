package java.util.logging


abstract class Handler

class Logger(name: String) {
  def getName(): String = name

  def log(level: Level, msg: String): Unit = {
    println(s"[$level] $name: $msg")
  }

  def log(record: LogRecord): Unit = {
    println(s"[${record.level}] $name: ${record.msg}")
  }

  def isLoggable(level: Level): Boolean = true

  def getParent(): Logger = {
    null
  }

  def getLevel(): Level = {
    Level.INFO
  }

  def setLevel(level: Level): Unit = {
    /// do nothing
  }

  def setUseParentHandlers(useParentHandlers: Boolean): Unit = {
    /// do nothing
  }

  def addHandler(h: Handler): Unit = {

  }

  def removeHandler(h: Handler): Unit = {
    /// do nothing
  }

  def getHandlers: Array[Handler] = Array.empty
}

object Logger:
  def getLogger(name: String): Logger = Logger(name)



abstract class Formatter:
  def format(record: LogRecord): String



case class LogRecord(level: Level, msg: String) extends Serializable:
  def setLoggerName(name: String): Unit = {
    /// do nothing
  }
  def setThrown(e: Throwable): Unit = {
    /// do nothing
  }
