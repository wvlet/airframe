/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package wvlet.airframe.metrics

import java.time.format.{DateTimeFormatterBuilder, SignStyle}
import java.time.{Instant, ZoneOffset, ZonedDateTime}
import java.util.Locale

/**
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

  def formatTimestamp(timeMillis: Long, zone: ZoneOffset = systemTimeZone): String = {
    val timestamp = ZonedDateTime.ofInstant(Instant.ofEpochMilli(timeMillis), zone)
    humanReadableTimestampFormatter.format(timestamp)
  }

  def formatTimestampWithNoSpace(timeMillis: Long): String = {
    val timestamp = ZonedDateTime.ofInstant(Instant.ofEpochMilli(timeMillis), systemTimeZone)
    noSpaceTimestampFormat.format(timestamp)
  }
}
