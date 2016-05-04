package wvlet.log

import java.util.{logging => jl}
import java.util.logging.Formatter

import wvlet.log.LogLevel.{ALL, DEBUG, ERROR, INFO, OFF, TRACE, WARN}

/**
  * Source code location where the log is
  *
  * @param path
  * @param fileName
  * @param line
  * @param col
  */
case class LogSource(path:String, fileName:String, line:Int, col:Int) {
  def fileBaseName = {
    val pos = fileName.lastIndexOf('.')
    if(pos == -1) {
      fileName
    }
    else {
      fileName.substring(0, pos)
    }
  }

  override def toString = s"${fileName}:${line}"
}

case class LogRecord(level:LogLevel, source:LogSource, message:String, cause:Option[Throwable] = None)
  extends jl.LogRecord(level.jlLevel, message) {

  def leafLoggerName : String = {
    getLoggerName match {
      case null => ""
      case name =>
        val pos = name.lastIndexOf('.')
        if(pos == -1) {
          name
        }
        else {
          name.substring(pos + 1)
        }
    }
  }

  def date = getMillis
}

trait LogFormatter extends Formatter {
  def formatLog(r:LogRecord) : String

  override def format(record: jl.LogRecord): String = {
    record match {
      case lr:LogRecord => formatLog(lr)
      case _ => s"[${record.getLoggerName}] ${record.getMessage}"
    }
  }
}

object ANSIColorLogFormatter extends LogFormatter {

  override def formatLog(r: LogRecord): String = {
    val prefix = r.level match {
      case ERROR => Console.RED
      case WARN => Console.YELLOW
      case INFO => Console.CYAN
      case DEBUG => Console.WHITE
      case TRACE => Console.MAGENTA
      case _ => ""
    }

    s"${prefix}[${r.leafLoggerName}] ${r.getMessage} - ${r.getLoggerName}(${r.source})${Console.RESET}"
  }

}


class ConsoleLogHandler(formatter:LogFormatter) extends jl.Handler {
  override def publish(record: jl.LogRecord): Unit = {
    System.err.println(formatter.format(record))
  }
  override def flush(): Unit = Console.flush()
  override def close(): Unit = {}
}
