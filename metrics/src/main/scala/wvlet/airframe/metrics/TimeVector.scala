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

case class TimeVector(x: Long, offset: Long, unit: ChronoUnit) {

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

object TimeVector {
  private val durationPattern = "^([+-]|last|next)?([0-9]+)(s|m|d|h|w|M|y)".r("prefix", "num", "unit")

  def apply(s: String): TimeVector = {
    s match {
      // current
      // thisXXXX is a special time range and needs to be backward range to include the current time
      //             now
      //   |----------x----------|
      //   <---------------------| x = -1, 1 unit distance from the offset
      //  grid (offset=0)  offset = 1
      case "thisHour"  => TimeVector(-1, 1, ChronoUnit.HOURS)
      case "today"     => TimeVector(-1, 1, ChronoUnit.DAYS)
      case "thisWeek"  => TimeVector(-1, 1, ChronoUnit.WEEKS)
      case "thisMonth" => TimeVector(-1, 1, ChronoUnit.MONTHS)
      case "thisYear"  => TimeVector(-1, 1, ChronoUnit.YEARS)
      // past
      case "lastHour"  => TimeVector(-1, 0, ChronoUnit.HOURS)
      case "yesterday" => TimeVector(-1, 0, ChronoUnit.DAYS)
      case "lastWeek"  => TimeVector(-1, 0, ChronoUnit.WEEKS)
      case "lastMonth" => TimeVector(-1, 0, ChronoUnit.MONTHS)
      case "lastYear"  => TimeVector(-1, 0, ChronoUnit.YEARS)
      // future
      case "nextHour"  => TimeVector(1, 1, ChronoUnit.HOURS)
      case "tomorrow"  => TimeVector(1, 1, ChronoUnit.DAYS)
      case "nextWeek"  => TimeVector(1, 1, ChronoUnit.WEEKS)
      case "nextMonth" => TimeVector(1, 1, ChronoUnit.MONTHS)
      case "nextYear"  => TimeVector(1, 1, ChronoUnit.YEARS)

      case other =>
        durationPattern.findFirstMatchIn(s) match {
          case None =>
            throw new IllegalArgumentException(s"Invalid duration: ${s}")
          case Some(m) =>
            val length = m.group("num").toInt
            val unit   = unitOf(m.group("unit"))
            m.group("prefix") match {
              case null | "-" | "last" =>
                TimeVector(-length, 0, unit)
              case "+" | "next" =>
                TimeVector(length, 0, unit)
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
