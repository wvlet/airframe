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

import java.time._
import java.time.temporal.ChronoUnit

import wvlet.log.LogSupport

import scala.annotation.tailrec
import scala.util.{Failure, Success, Try}

/**
  * TimeWindow of [statrt, end) range. The end-side is open. start <= time < end
  *
  * @param start
  * @param end
  */
case class TimeWindow(start: ZonedDateTime, end: ZonedDateTime) {
  require(
    start.compareTo(end) <= 0,
    s"invalid range: ${TimeStampFormatter.formatTimestamp(start)} > ${TimeStampFormatter.formatTimestamp(end)}"
  )

  private def instantOfStart = start.toInstant
  private def instantOfEnd   = end.toInstant

  def startUnixTime = start.toEpochSecond
  def endUnixTime   = end.toEpochSecond

  def startEpochMillis = instantOfStart.toEpochMilli
  def endEpochMillis   = instantOfEnd.toEpochMilli

  override def toString = {
    val s = TimeStampFormatter.formatTimestamp(start)
    val e = TimeStampFormatter.formatTimestamp(end)
    s"[${s},${e})"
  }

  def toStringAt(zone: ZoneOffset) = {
    val s = TimeStampFormatter.formatTimestamp(startEpochMillis, zone)
    val e = TimeStampFormatter.formatTimestamp(endEpochMillis, zone)
    s"[${s},${e})"
  }

  private def splitInto(unit: ChronoUnit): Seq[TimeWindow] = {
    val b      = Seq.newBuilder[TimeWindow]
    var cursor = start
    while (cursor.compareTo(end) < 0) {
      val e = unit match {
        case ChronoUnit.DAYS | ChronoUnit.HOURS | ChronoUnit.MINUTES =>
          cursor.plus(1, unit).truncatedTo(unit)
        case ChronoUnit.WEEKS =>
          cursor.plus(1, unit).`with`(DayOfWeek.MONDAY)
        case ChronoUnit.MONTHS =>
          cursor.plus(1, unit).withDayOfMonth(1)
        case ChronoUnit.YEARS =>
          cursor.plus(1, unit).withDayOfYear(1)
        case other =>
          throw new IllegalStateException(s"Invalid split unit ${unit} for range ${toString}")
      }
      if (e.compareTo(end) <= 0) {
        b += TimeWindow(cursor, e)
      } else {
        b += TimeWindow(cursor, end)
      }
      cursor = e
    }
    b.result()
  }

  def splitIntoHours: Seq[TimeWindow]  = splitInto(ChronoUnit.HOURS)
  def splitIntoDays: Seq[TimeWindow]   = splitInto(ChronoUnit.DAYS)
  def splitIntoMonths: Seq[TimeWindow] = splitInto(ChronoUnit.MONTHS)
  def splitIntoWeeks: Seq[TimeWindow]  = splitInto(ChronoUnit.WEEKS)

  def splitAt(date: ZonedDateTime): Seq[TimeWindow] = {
    if (date.compareTo(start) <= 0 || date.compareTo(end) > 0) {
      // date is out of range
      Seq(this)
    } else {
      Seq(TimeWindow(start, date), TimeWindow(date, end))
    }
  }

  def plus(n: Long, unit: ChronoUnit): TimeWindow  = TimeWindow(start.plus(n, unit), end.plus(n, unit))
  def minus(n: Long, unit: ChronoUnit): TimeWindow = plus(-n, unit)

  def howMany(unit: ChronoUnit): Long = unit.between(start, end)
  def howMany(unit: TimeWindowUnit): Long = {
    unit match {
      case TimeWindowUnit.Year =>
        howMany(ChronoUnit.YEARS)
      case TimeWindowUnit.Quarter =>
        val startTruncated = unit.truncate(start)
        val endTruncated   = unit.truncate(end)
        val yearDiff       = endTruncated.getYear - startTruncated.getYear
        (endTruncated.getMonthValue + (yearDiff * 12) - startTruncated.getMonthValue) / 3
      case TimeWindowUnit.Month =>
        monthDiff
      case TimeWindowUnit.Week =>
        weekDiff
      case TimeWindowUnit.Day =>
        dateDiff
      case TimeWindowUnit.Hour =>
        hourDiff
      case TimeWindowUnit.Minute =>
        minuteDiff
      case TimeWindowUnit.Second =>
        secondDiff
    }
  }

  def secondDiff: Long = howMany(ChronoUnit.SECONDS)
  def minuteDiff: Long = howMany(ChronoUnit.MINUTES)
  def hourDiff: Long   = howMany(ChronoUnit.HOURS)
  def dateDiff: Long   = howMany(ChronoUnit.DAYS)
  def weekDiff: Long   = howMany(ChronoUnit.WEEKS)
  def monthDiff: Long  = howMany(ChronoUnit.MONTHS)
  def yearDiff: Long   = howMany(ChronoUnit.YEARS)

  def intersectsWith(other: TimeWindow): Boolean = {
    start.compareTo(other.end) < 0 && end.compareTo(other.start) > 0
  }
}

object TimeWindow extends LogSupport {
  def withTimeZone(zoneName: String): TimeWindowBuilder = {
    import scala.jdk.CollectionConverters._
    // Add commonly used daylight saving times
    val idMap = ZoneId.SHORT_IDS.asScala ++
      Map("PDT" -> "-07:00", "EDT" -> "-04:00", "CDT" -> "-05:00", "MDT" -> "-06:00")
    val zoneId = idMap
      .get(zoneName)
      .map(ZoneId.of(_))
      .getOrElse { ZoneId.of(zoneName) }

    withTimeZone(zoneId)
  }

  def withTimeZone(zoneId: ZoneId): TimeWindowBuilder = {
    val offset = ZonedDateTime.now(zoneId).getOffset
    new TimeWindowBuilder(offset)
  }
  def withUTC: TimeWindowBuilder            = withTimeZone(UTC)
  def withSystemTimeZone: TimeWindowBuilder = withTimeZone(systemTimeZone)
}

class TimeWindowBuilder(val zone: ZoneOffset, currentTime: Option[ZonedDateTime] = None) extends LogSupport {
  def withOffset(t: ZonedDateTime): TimeWindowBuilder = new TimeWindowBuilder(zone, Some(t))
  def withOffset(dateTimeStr: String): TimeWindowBuilder = {
    TimeParser
      .parse(dateTimeStr, zone)
      .map(d => withOffset(d))
      .getOrElse {
        throw new IllegalArgumentException(s"Invalid datetime: ${dateTimeStr}")
      }
  }
  def withUnixTimeOffset(unixTime: Long): TimeWindowBuilder = {
    withOffset(ZonedDateTime.ofInstant(Instant.ofEpochSecond(unixTime), UTC))
  }

  def now                 = currentTime.getOrElse(ZonedDateTime.now(zone))
  def beginningOfTheHour  = now.truncatedTo(ChronoUnit.HOURS)
  def endOfTheHour        = beginningOfTheHour.plusHours(1)
  def beginningOfTheDay   = now.truncatedTo(ChronoUnit.DAYS)
  def endOfTheDay         = beginningOfTheDay.plusDays(1)
  def beginningOfTheWeek  = now.truncatedTo(ChronoUnit.DAYS).`with`(DayOfWeek.MONDAY)
  def endOfTheWeek        = beginningOfTheWeek.plusWeeks(1)
  def beginningOfTheMonth = now.withDayOfMonth(1).truncatedTo(ChronoUnit.DAYS)
  def endOfTheMonth       = beginningOfTheMonth.plusMonths(1)
  def beginningOfTheYear  = now.withDayOfYear(1).truncatedTo(ChronoUnit.DAYS)
  def endOfTheYear        = beginningOfTheYear.plusYears(1)

  def today     = TimeWindow(beginningOfTheDay, endOfTheDay)
  def thisHour  = TimeWindow(beginningOfTheHour, endOfTheHour)
  def thisWeek  = TimeWindow(beginningOfTheWeek, endOfTheWeek)
  def thisMonth = TimeWindow(beginningOfTheMonth, endOfTheMonth)
  def thisYear  = TimeWindow(beginningOfTheYear, endOfTheYear)

  def yesterday = today.minus(1, ChronoUnit.DAYS)

  @tailrec
  private def parseOffset(o: String, windowUnit: TimeWindowUnit, adjustments: Seq[TimeVector] = Nil): ZonedDateTime = {
    val pattern = s"^(?<duration>[^/]+)(?<sep>/(?<offset>.+))".r
    pattern.findFirstMatchIn(o) match {
      case Some(m) =>
        // When a nested offset is found
        val d        = m.group("duration")
        val duration = TimeVector(d)
        parseOffset(m.group("offset"), windowUnit, duration +: adjustments)
      case None =>
        // no more nested offsets
        o match {
          case "now" => adjustOffset(now, adjustments)
          case other =>
            Try(TimeVector(o)) match {
              case Success(x) =>
                // When the offset string is time duration patterns (e.g., 0M, 0d, etc.)
                if (x.x <= 0) {
                  x.timeWindowFrom(adjustOffset(now, adjustments)).start
                } else {
                  x.timeWindowFrom(adjustOffset(now, adjustments)).end
                }
              case Failure(e) =>
                // When the offset string is the exact date
                val (timeString, truncate) = if (o.endsWith(")")) {
                  (o.substring(0, o.length - 1), false)
                } else {
                  (o, true)
                }
                TimeParser
                  .parse(timeString, zone)
                  .map(offset => adjustOffset(offset, adjustments))
                  // Truncate the exact date time to the time window unit
                  .map(offset => if (truncate) windowUnit.truncate(offset) else offset)
                  .getOrElse {
                    throw new IllegalArgumentException(s"Invalid offset string: ${o}")
                  }
            }
        }
    }
  }

  private def adjustOffset(offset: ZonedDateTime, adjustments: Seq[TimeVector]): ZonedDateTime = {
    adjustments.foldLeft(offset) { case (offset, duration) =>
      duration.unit.increment(offset, duration.x)
    }
  }

  def parse(str: String): TimeWindow = {
    val pattern = s"^(?<duration>[^/]+)(?<sep>/(?<offset>.*))?".r
    pattern.findFirstMatchIn(str) match {
      case Some(m) =>
        val d = m.group("duration")

        // Check the exact start date first
        TimeParser.parseTimeAndUnit(d, zone) match {
          case Some(exactStartTime) =>
            // When exact start datetime is specified
            val offset = m.group("offset") match {
              case null =>
                // When the offset is not given, use the start date + time unit as the end point
                val thisUnit = TimeVector(-1, 1, exactStartTime.unit)
                thisUnit.timeWindowFrom(exactStartTime.dateTime).end
              case offsetStr =>
                // [start date, offset time)
                parseOffset(offsetStr, TimeWindowUnit.Second)
            }
            TimeWindow(exactStartTime.dateTime, offset)
          case None =>
            // Check relative time ranges
            val duration = TimeVector(d)
            m.group("offset") match {
              case null =>
                // When offset is not given, use the truncated time
                val context = duration.unit.truncate(now)
                duration.timeWindowFrom(context)
              case offsetStr =>
                val offset = parseOffset(offsetStr, duration.unit)
                duration.timeWindowFrom(offset)
            }
        }
      case None =>
        throw new IllegalArgumentException(s"TimeRange.of(${str})")
    }
  }

  def fromRange(startUnixTime: Long, endUnixTime: Long): TimeWindow = {
    TimeWindow(Instant.ofEpochSecond(startUnixTime).atZone(zone), Instant.ofEpochSecond(endUnixTime).atZone(zone))
  }
}
