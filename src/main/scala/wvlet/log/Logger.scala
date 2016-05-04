package wvlet.log

import java.io.{ByteArrayOutputStream, PrintStream}
import java.util.logging.{LogRecord, _}
import java.util.{Locale, logging => jl}

import scala.language.experimental.macros


/**
  * log level definitions
  */
object LogLevel {

  case object OFF extends LogLevel(0, Level.OFF, "off")
  case object ERROR extends LogLevel(1, Level.SEVERE, "error")
  case object WARN extends LogLevel(2, Level.WARNING ,"warn")
  case object INFO extends LogLevel(3, Level.INFO, "info")
  case object DEBUG extends LogLevel(4, Level.FINE, "debug")
  case object TRACE extends LogLevel(5, Level.FINER, "trace")
  case object ALL extends LogLevel(6, Level.ALL, "all")

  val values = IndexedSeq(OFF, ERROR, WARN, INFO, DEBUG, TRACE, ALL)
  private lazy val index = values.map { l => l.name.toLowerCase -> l } toMap

  def apply(name: String): LogLevel = {
    val n = name.toLowerCase(Locale.US)
    val lv = values.find(n == _.name)
    if (lv.isEmpty) {
      Console.err.println(s"Unknown log level [${name}] Use info log level instead.")
      INFO
    }
    else
      lv.get
  }

  def unapply(name:String) : Option[LogLevel] = index.get(name.toLowerCase(Locale.US))

  implicit object LogOrdering extends Ordering[LogLevel] {
    override def compare(x: LogLevel, y: LogLevel): Int = x.order - y.order
  }
}

sealed abstract class LogLevel(val order: Int, val jlLevel: Level, val name: String) extends Ordered[LogLevel] with Serializable {
  def compare(other: LogLevel) = this.order - other.order
  override def toString = name
}


object Logger {

  val root = getLogger("", handlers = Seq(new ConsoleLogHandler(new ConsoleLogFormatter)))

  /**
    * Create a new {@link java.util.logging.Logger}
    * @param name
    * @param level
    * @param handlers
    * @param useParents
    * @return
    */
  def getLogger(name:String,
                level:Option[LogLevel] = None,
                handlers:Seq[Handler] = Seq.empty,
                useParents: Boolean = true
               ) : jl.Logger = {
    val logger = jl.Logger.getLogger(name)
    logger.clearHandlers
    level.foreach(l => logger.setLevel(l.jlLevel))
    handlers.foreach(h => logger.addHandler(h))
    logger.setUseParentHandlers(useParents)
    logger
  }

  implicit class RichLogger(logger:jl.Logger) {

    def setLogLevel(l:LogLevel) {
      logger.setLevel(l.jlLevel)
    }

    def clear {
      clearHandlers
      resetLogLevel
    }

    def clearHandlers {
      for(lst <- Option(logger.getHandlers); h <- lst) {
        logger.removeHandler(h)
      }
    }

    def resetLogLevel {
      logger.setLevel(null)
    }

    def isEnabled(level:LogLevel) : Boolean = {
      logger.isLoggable(level.jlLevel)
    }

    def log(record:LogRecord) {
      logger.log(record)
    }
  }
}


case class LogRecord(level:LogLevel, source:String, line:Int, col:Int, message:String, cause:Option[Throwable] = None)
  extends jl.LogRecord(level.jlLevel, message)


trait LogFormatter extends Formatter {
  def formatLog(r:LogRecord) : String
}

class ConsoleLogFormatter extends LogFormatter {

  override def formatLog(r: LogRecord): String = {
    s"[${r.getLoggerName}] ${r.source} ${r.getMessage}"
  }
  override def format(record: jl.LogRecord): String = {
    record match {
      case lr:LogRecord => formatLog(lr)
      case _ => s"[${record.getLoggerName}] ${record.getMessage}"
    }
  }

}


class ConsoleLogHandler(formatter:LogFormatter) extends jl.Handler {
  override def publish(record: jl.LogRecord): Unit = {
    System.err.println(formatter.format(record))
  }
  override def flush(): Unit = Console.flush()
  override def close(): Unit = {}
}


/**
  *
  */
trait Logger extends Serializable {

  import LogMacros._

  import Logger._

  protected[this] def logger : RichLogger = {
    val l = Logger.getLogger(this.getClass.getName)
    l
  }

  protected[this] def formatLog(message: Any): String = {
    def errorString(e: Throwable) = {
      val buf = new ByteArrayOutputStream()
      try {
        val pout = new PrintStream(buf)
        try {
          e.printStackTrace(pout)
        }
        finally {
          pout.close()
        }
      }
      finally {
        buf.close()
      }
      buf.toString
    }

    message match {
      case null => ""
      case e: Error => errorString(e)
      case e: Exception => errorString(e)
      case _ => message.toString
    }
  }

  protected def error(message: Any): Unit = macro errorLog
  protected def error(message: Any, cause:Throwable): Unit = macro errorLogWithCause

  protected def warn(message: Any): Unit = macro warnLog
  protected def warn(message: Any, cause:Throwable): Unit = macro warnLogWithCause

  protected def info(message: Any): Unit = macro infoLog
  protected def info(message: Any, cause:Throwable): Unit = macro infoLogWithCause

  protected def debug(message: Any): Unit = macro debugLog
  protected def debug(message: Any, cause:Throwable): Unit = macro debugLogWithCause

  protected def trace(message: Any): Unit = macro traceLog
  protected def trace(message: Any, cause:Throwable): Unit = macro traceLogWithCause

}

