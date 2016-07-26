package wvlet.log

import java.io.{ByteArrayOutputStream, PrintStream, PrintWriter, StringWriter}
import java.time.format.{DateTimeFormatter, DateTimeFormatterBuilder, SignStyle}
import java.time.{Instant, ZoneId, ZonedDateTime}
import java.util.logging.Formatter
import java.util.{Locale, logging => jl}

import wvlet.log.LogLevel.{DEBUG, ERROR, INFO, TRACE, WARN}

/**
  * To implement your own log formatter, implement this formatLog(r: LogRecord) method
  */
trait LogFormatter extends Formatter {
  def formatLog(r: LogRecord): String

  override def format(record: jl.LogRecord): String = {
    record match {
      case lr: LogRecord => formatLog(lr)
      case _ => formatLog(LogRecord(record))
    }
  }
}

object LogFormatter {

  import java.time.temporal.ChronoField._

  val systemZone             = ZoneId.systemDefault().normalized()
  val noSpaceTimestampFormat = new DateTimeFormatterBuilder()
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

  val humanReadableTimestampFormatter = new DateTimeFormatterBuilder()
                                        .parseCaseInsensitive()
                                        .appendValue(YEAR, 4, 10, SignStyle.EXCEEDS_PAD)
                                        .appendLiteral('-')
                                        .appendValue(MONTH_OF_YEAR, 2)
                                        .appendLiteral('-')
                                        .appendValue(DAY_OF_MONTH, 2)
                                        .appendLiteral(' ')
                                        .appendValue(HOUR_OF_DAY, 2)
                                        .appendLiteral(':')
                                        .appendValue(MINUTE_OF_HOUR, 2)
                                        .appendLiteral(':')
                                        .appendValue(SECOND_OF_MINUTE, 2)
                                        .appendOffset("+HHMM", "Z")
                                        .toFormatter(Locale.US)

  def formatTimestamp(timeMillis: Long, dateTimeformatter:DateTimeFormatter = humanReadableTimestampFormatter): String = {
    val timestamp = ZonedDateTime.ofInstant(Instant.ofEpochMilli(timeMillis), systemZone)
    dateTimeformatter.format(timestamp)
  }

  def currentThreadName: String = Thread.currentThread().getName

  def formatStacktrace(e: Throwable) = {
    val trace = new StringWriter()
    e.printStackTrace(new PrintWriter(trace))
    trace.toString
  }

  def withColor(prefix:String, s:String) = {
    s"${prefix}${s}${Console.RESET}"
  }

  def highlightLog(level: LogLevel, message: String): String = {
    val color = level match {
      case ERROR => Console.RED
      case WARN => Console.YELLOW
      case INFO => Console.CYAN
      case DEBUG => Console.GREEN
      case TRACE => Console.MAGENTA
      case _ => Console.RESET
    }
    withColor(color, message)
  }

  def appendStackTrace(m:String, r:LogRecord) : String = {
    r.cause match {
      case Some(ex) =>
        s"${m}\n${formatStacktrace(ex)}"
      case None =>
        m
    }
  }

  object TSVLogFormatter extends LogFormatter {
    override def formatLog(record: LogRecord): String = {
      val s = Seq.newBuilder[String]
      s += formatTimestamp(record.getMillis, noSpaceTimestampFormat)
      s += record.level.toString
      s += currentThreadName
      s += record.leafLoggerName
      s += record.getMessage

      val log = s.result().mkString("\t")
      record.cause match {
        case Some(ex) =>
          s"${log}\n${formatStacktrace(ex)}"
        case None =>
          log
      }
    }
  }

  /**
    * Simple log formatter that shows only logger name and message
    */
  object SimpleLogFormatter extends LogFormatter {
    override def formatLog(r: LogRecord): String = {
      val log = s"[${highlightLog(r.level, r.leafLoggerName)}] ${highlightLog(r.level, r.getMessage)}"
      appendStackTrace(log, r)
    }
  }

  /**
    * log format for command-line user client
    */
  object AppLogFormatter extends LogFormatter {
    override def formatLog(r: LogRecord): String = {
      val logTag = highlightLog(r.level, r.level.name)
      val log = f"${withColor(Console.BLUE, formatTimestamp(r.getMillis))} ${logTag}%14s [${withColor(Console.WHITE, r.leafLoggerName)}] ${highlightLog(r.level, r.getMessage)}"
      appendStackTrace(log, r)
    }
  }

  /**
    * log format for debugging source code
    */
  object SourceCodeLogFormatter extends LogFormatter {
    override def formatLog(r: LogRecord): String = {
      val loc =
        r.source
        .map(source => s" ${withColor(Console.BLUE, s"- (${source.fileLoc})")}")
        .getOrElse("")

      val log = s"[${withColor(Console.BLUE, r.leafLoggerName)}] [${highlightLog(r.level, r.level.name)}] ${r.getMessage}${loc}"
      appendStackTrace(log, r)
    }
  }

  /**
    * Enable source code links in the run/debug console of IntelliJ
    */
  object IntelliJLogFormatter extends LogFormatter {
    override def formatLog(r: LogRecord): String = {
      val loc =
        r.source
        .map(source => s" ${withColor(Console.BLUE, s"- ${r.getLoggerName}(${source.fileLoc})")}")
        .getOrElse("")

      val log = s"[${highlightLog(r.level, r.level.name)}] ${highlightLog(r.level, r.getMessage)}$loc"
      appendStackTrace(log, r)
    }
  }

}
