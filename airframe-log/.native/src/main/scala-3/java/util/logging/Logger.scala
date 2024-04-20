package java.util.logging


abstract class Handler extends AutoCloseable:
  def publish(record: LogRecord): Unit
  def flush(): Unit

class Logger(name: String) {
  private var handlers = List.empty[Handler]
  private var parent: Option[Logger] = None
  private var useParentHandlers = true
  private var level: Option[Level] = None

  def getName(): String = name

  def log(level: Level, msg: String): Unit = {
    log(LogRecord(level, msg))
  }

  def log(record: LogRecord): Unit = {
    if(isLoggable(record.level)) {
      record.setLoggerName(name)
      if(useParentHandlers) then
        getParent().log(record)
      else
        handlers.foreach { h => h.publish(record) }
    }
  }

  def isLoggable(level: Level): Boolean = {
    val l = getLevel()
    if(level.intValue() < l.intValue()) then false else true
  }

  def getParent(): Logger = {
    parent.getOrElse(null)
  }

  def getLevel(): Level = {
    level.orElse(parent.map(_.getLevel())).getOrElse(Level.INFO)
  }

  def setLevel(newLevel: Level): Unit = {
    level = Some(newLevel)
  }

  def resetLogLevel(): Unit = {
    level = None
  }

  def setUseParentHandlers(useParentHandlers: Boolean): Unit = {
    this.useParentHandlers = useParentHandlers
  }

  def addHandler(h: Handler): Unit = {
    handlers = h :: handlers
  }

  def removeHandler(h: Handler): Unit = {
    handlers = handlers.filter(_ != h)
  }

  def getHandlers: Array[Handler] = handlers.toArray
}

object Logger:
  def getLogger(name: String): Logger = Logger(name)



object LogManager:
  private var loggers = Map.empty[String, Logger]
  def getLogger(name: String): Logger = {


    loggers.getOrElse(name, {
      val logger = Logger(name)
      loggers += (name -> logger)
      logger
    })
  }


abstract class Formatter:
  def format(record: LogRecord): String



case class LogRecord(level: Level, msg: String) extends Serializable:
  private val millis = System.currentTimeMillis()
  private var loggerName = ""
  private var thrown: Throwable = null

  def getMessage(): String = msg
  def getMillis(): Long = millis
  def getLoggerName(): String = loggerName
  def getLevel(): Level = level
  def getThrown(): Throwable = thrown

  def setLoggerName(name: String): Unit = {
    this.loggerName = name
  }
  def setThrown(e: Throwable): Unit = {
    thrown = e
  }
