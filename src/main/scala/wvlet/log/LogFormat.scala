package wvlet.log

import java.io.{PrintWriter, StringWriter}
import java.time.{Instant, ZoneId, ZonedDateTime}
import java.time.format.{DateTimeFormatterBuilder, SignStyle}
import java.util.{Locale, logging => jl}
import java.util.logging.{Formatter, LogRecord}

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

  def fileLoc = s"${fileName}:${line}"
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

object DefaultTSVLogFormatter extends LogFormatter {

  import java.time.temporal.ChronoField._

  private val SYSTEM_ZONE = ZoneId.systemDefault().normalized();
  private val TIMESTAMP_FORMATTER = new DateTimeFormatterBuilder()
                                    .parseCaseInsensitive()
                                    .appendValue(YEAR, 4, 10, SignStyle.EXCEEDS_PAD)
                                    .appendLiteral('-')
                                    .appendValue(MONTH_OF_YEAR, 2)
                                    .appendLiteral('-')
                                    .appendValue(DAY_OF_MONTH, 2)
                                    .appendLiteral('T')
                                    .appendValue(HOUR_OF_DAY, 2)
                                    .appendLiteral(':')
                                    .appendValue(MINUTE_OF_HOUR, 2)
                                    .appendLiteral(':')
                                    .appendValue(SECOND_OF_MINUTE, 2)
                                    .appendLiteral('.')
                                    .appendValue(MILLI_OF_SECOND, 3)
                                    .appendOffset("+HHMM", "Z")
                                    .toFormatter(Locale.US)

  override def formatLog(record: LogRecord): String = {
    val timestamp = ZonedDateTime.ofInstant(Instant.ofEpochMilli(record.getMillis()), SYSTEM_ZONE)
    val s = Seq.newBuilder[String]
    s += TIMESTAMP_FORMATTER.format(timestamp)
    s += record.level.toString
    s += Thread.currentThread().getName
    s += record.leafLoggerName
    s += record.getMessage

    val log = s.result().mkString("\t")
    Option(record.getThrown) match {
      case Some(ex) =>
        val trace = new StringWriter()
        ex.printStackTrace(new PrintWriter(trace))
        s"${log}\n${trace.toString}"
      case None =>
        log
    }
  }
}

object ColorLogFormatter extends LogFormatter {

  def colorLog(level:LogLevel, m:String) : String = {
    val prefix = level match {
      case ERROR => Console.RED
      case WARN => Console.YELLOW
      case INFO => Console.CYAN
      case DEBUG => Console.WHITE
      case TRACE => Console.MAGENTA
      case _ => ""
    }
    s"${prefix}${m}${Console.RESET}"
  }

  override def formatLog(r: LogRecord): String = {
    colorLog(r.level, s"[${r.leafLoggerName}] ${r.getMessage}")
  }
}

object DebugLogFormatter extends LogFormatter {
  override def formatLog(r: LogRecord): String = {
    s"${ColorLogFormatter.colorLog(r.level, s"[${r.leafLoggerName}] ${r.getMessage}")} - ${r.source.fileLoc}"
  }
}





class ConsoleLogHandler(formatter:LogFormatter) extends jl.Handler {
  override def publish(record: jl.LogRecord): Unit = {
    System.err.println(formatter.format(record))
  }
  override def flush(): Unit = Console.flush()
  override def close(): Unit = {}
}


