package wvlet.airframe.metrics

import java.time.{Instant, ZoneId, ZoneOffset, ZonedDateTime}
import java.time.format.{DateTimeFormatterBuilder, SignStyle}
import java.util.Locale

/**
  *
  */
object TimeStampFormatter {
  import java.time.temporal.ChronoField._

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

  def formatTimestamp(time: ZonedDateTime): String = {
    humanReadableTimestampFormatter.format(time)
  }

  def formatTimestamp(timeMillis: Long, zone: ZoneOffset = TimeWindow.systemZone): String = {
    val timestamp = ZonedDateTime.ofInstant(Instant.ofEpochMilli(timeMillis), zone)
    humanReadableTimestampFormatter.format(timestamp)
  }

  def formatTimestampWithNoSpaace(timeMillis: Long): String = {
    val timestamp = ZonedDateTime.ofInstant(Instant.ofEpochMilli(timeMillis), TimeWindow.systemZone)
    noSpaceTimestampFormat.format(timestamp)
  }

}
