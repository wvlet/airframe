package wvlet.log

import java.io.{ByteArrayOutputStream, PrintStream}
import java.util.logging.Level

import scala.language.experimental.macros


object Logger {

//  lazy val configuration : Unit = {
//    val context = LoggerFactory.getILoggerFactory().asInstanceOf[LoggerContext]
//    val configurator = new JoranConfigurator
//    configurator.setContext(context)
//    context.reset()
//
//    if(System.console() != null) {
//      configurator.doConfigure(getClass.getResource("/wvlet/log/logback-console.xml"))
//    }
//  }
}

/**
  *
  */
trait Logger extends Serializable {

  import LogMacros._

  protected[this] def logger = {
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

