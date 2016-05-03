package wvlet.log

import java.io.{ByteArrayOutputStream, PrintStream}
import java.util.logging.{Handler, Level}
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
}

sealed abstract class LogLevel(val order: Int, val jlLevel: Level, val name: String) extends Ordered[LogLevel] with Serializable {
  def compare(other: LogLevel) = this.order - other.order
  override def toString = name
}

trait LoggerHandler {

  def handler: Handler

  def addTo(logger:jl.Logger) {
    logger.addHandler(handler)
  }
}




object Logger {

  /**
    * Create a new {@link java.util.logging.Logger}
    * @param name
    * @param level
    * @param handlers
    * @param useParents
    * @return
    */
  def getLogger(name:String,
                level:Option[LogLevel],
                handlers:Seq[LoggerHandler] = Seq.empty,
                useParents: Boolean = true
               ) : jl.Logger = {
    val logger = jl.Logger.getLogger(name)
    logger.clearHandlers
    level.foreach(l => logger.setLevel(l.jlLevel))
    handlers.foreach(h => h.addTo(logger))
    logger.setUseParentHandlers(useParents)
    logger
  }

  implicit class RichLogger(logger:jl.Logger) {
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
  }
}


/**
  *
  */
trait LogSupport extends Serializable {

  import LogMacros._

  protected[this] def logger : jl.Logger = {
    //Logger.configuration // initialize
    java.util.logging.Logger.getLogger(this.getClass.getName)
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

