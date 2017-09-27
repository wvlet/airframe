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

import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

case class TimeDuration(x: Long, unit: ChronoUnit, offset: Long = 0) {

  def timeWindowAt(context: ZonedDateTime): TimeWindow = {
    val grid        = TimeWindow.truncateTo(context, unit)
    val base        = grid.plus(offset, unit)
    val theOtherEnd = base.plus(x, unit)

    if (x <= 0) {
      if (grid.compareTo(context) == 0)
        TimeWindow(theOtherEnd, base)
      else
        TimeWindow(theOtherEnd, context)
    } else {
      if (grid.compareTo(context) == 0)
        TimeWindow(base, theOtherEnd)
      else
        TimeWindow(context, theOtherEnd)
    }
  }

}

object TimeDuration {
  private val durationPattern = "^([+-]|last|next)?([0-9]+)(s|m|d|h|w|M|y)".r("prefix", "num", "unit", "o")

  def apply(s: String): TimeDuration = {
    s match {
      // current
      // thisXXXX is a special time range and needs to be backward range to include the current time
      //
      //         now
      //   |------x------|
      //    <------------|  -1 x unit distance from the offset
      // base          offset
      case "thisHour"  => TimeDuration(-1, ChronoUnit.HOURS, 1)
      case "today"     => TimeDuration(-1, ChronoUnit.DAYS, 1)
      case "thisWeek"  => TimeDuration(-1, ChronoUnit.WEEKS, 1)
      case "thisMonth" => TimeDuration(-1, ChronoUnit.MONTHS, 1)
      case "thisYear"  => TimeDuration(-1, ChronoUnit.YEARS, 1)
      // past
      case "lastHour"  => TimeDuration(-1, ChronoUnit.HOURS)
      case "yesterday" => TimeDuration(-1, ChronoUnit.DAYS)
      case "lastWeek"  => TimeDuration(-1, ChronoUnit.WEEKS)
      case "lastMonth" => TimeDuration(-1, ChronoUnit.MONTHS)
      case "lastYear"  => TimeDuration(-1, ChronoUnit.YEARS)
      // future
      case "nextHour"  => TimeDuration(1, ChronoUnit.HOURS, 1)
      case "tomorrow"  => TimeDuration(1, ChronoUnit.DAYS, 1)
      case "nextWeek"  => TimeDuration(1, ChronoUnit.WEEKS, 1)
      case "nextMonth" => TimeDuration(1, ChronoUnit.MONTHS, 1)
      case "nextYear"  => TimeDuration(1, ChronoUnit.YEARS, 1)

      case other =>
        durationPattern.findFirstMatchIn(s) match {
          case None =>
            throw new IllegalArgumentException(s"Invalid duration: ${s}")
          case Some(m) =>
            val length = m.group("num").toInt
            val unit   = unitOf(m.group("unit"))
            m.group("prefix") match {
              case null | "-" | "last" =>
                TimeDuration(-length, unit)
              case "+" | "next" =>
                TimeDuration(length, unit)
              case other =>
                throw new IllegalArgumentException(s"Unknown duration prefix: ${other}")
            }
        }
    }
  }

  private val unitTable: Map[String, ChronoUnit] = Map(
    "s" -> ChronoUnit.SECONDS,
    "m" -> ChronoUnit.MINUTES,
    "d" -> ChronoUnit.DAYS,
    "h" -> ChronoUnit.HOURS,
    "w" -> ChronoUnit.WEEKS,
    "M" -> ChronoUnit.MONTHS,
    "y" -> ChronoUnit.YEARS
  )

  private[metrics] def unitOf(s: String): ChronoUnit = {
    unitTable.getOrElse(s, throw new IllegalArgumentException(s"Unknown unit type ${s}"))
  }

}
