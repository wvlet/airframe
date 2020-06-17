package wvlet.log

import java.time.format.{DateTimeFormatterBuilder, SignStyle}
import java.time.{Instant, ZoneId, ZonedDateTime}
import java.util.Locale

/**
  */
object LogTimestampFormatter {
  import java.time.temporal.ChronoField._

  val systemZone = ZoneId.systemDefault().normalized()
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
    .appendLiteral('.')
    .appendValue(MILLI_OF_SECOND, 3)
    .appendOffset("+HHMM", "Z")
    .toFormatter(Locale.US)

  def formatTimestamp(timeMillis: Long): String = {
    val timestamp = ZonedDateTime.ofInstant(Instant.ofEpochMilli(timeMillis), systemZone)
    humanReadableTimestampFormatter.format(timestamp)
  }

  def formatTimestampWithNoSpaace(timeMillis: Long): String = {
    val timestamp = ZonedDateTime.ofInstant(Instant.ofEpochMilli(timeMillis), systemZone)
    noSpaceTimestampFormat.format(timestamp)
  }
}
