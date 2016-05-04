package wvlet.log

import java.io.{ByteArrayOutputStream, PrintStream}
import java.util.logging.{LogRecord, _}
import java.util.{Locale, logging => jl}

import scala.language.experimental.macros



object Logger {

  val rootLogger = getLogger("", handlers = Seq(new ConsoleLogHandler(new ConsoleLogFormatter)))

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
      record.setLoggerName(logger.getName)
      logger.log(record)
    }
  }
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

