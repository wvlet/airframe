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

case class TimeVector(x: Long, offset: Long, unit: TimeUnit) {

  def timeWindowFrom(context: ZonedDateTime): TimeWindow = {
    val grid = unit.truncate(context)

    val startOffset = unit.increment(grid, offset)
    val end         = unit.increment(startOffset, x)

    val onGrid = grid.compareTo(context) == 0
    val start  = if (onGrid) startOffset else context

    if (start.compareTo(end) <= 0) {
      TimeWindow(start, end)
    } else {
      TimeWindow(end, start)
    }
  }
}

object TimeVector {
  private val durationPattern = "^([+-]|last|next)?([0-9]+)(s|m|d|h|w|M|q|y)".r("prefix", "num", "unit")

  def apply(s: String): TimeVector = {
    s match {
      // current
      // thisXXXX is a special time range and needs to be backward range to include the current time
      //             now
      //   |----------x----------|
      //   <---------------------| x = -1, 1 unit distance from the offset
      //  grid (offset=0)  offset = 1
      case "thisHour"  => TimeVector(-1, 1, TimeUnit.Hour)
      case "today"     => TimeVector(-1, 1, TimeUnit.Day)
      case "thisWeek"  => TimeVector(-1, 1, TimeUnit.Week)
      case "thisMonth" => TimeVector(-1, 1, TimeUnit.Month)
      case "thisYear"  => TimeVector(-1, 1, TimeUnit.Year)
      // past
      case "lastHour"  => TimeVector(-1, 0, TimeUnit.Hour)
      case "yesterday" => TimeVector(-1, 0, TimeUnit.Day)
      case "lastWeek"  => TimeVector(-1, 0, TimeUnit.Week)
      case "lastMonth" => TimeVector(-1, 0, TimeUnit.Month)
      case "lastYear"  => TimeVector(-1, 0, TimeUnit.Year)
      // future
      case "nextHour"  => TimeVector(1, 1, TimeUnit.Hour)
      case "tomorrow"  => TimeVector(1, 1, TimeUnit.Day)
      case "nextWeek"  => TimeVector(1, 1, TimeUnit.Week)
      case "nextMonth" => TimeVector(1, 1, TimeUnit.Month)
      case "nextYear"  => TimeVector(1, 1, TimeUnit.Year)

      case other =>
        durationPattern.findFirstMatchIn(s) match {
          case None =>
            throw new IllegalArgumentException(s"Invalid duration: ${s}")
          case Some(m) =>
            val length = m.group("num").toInt
            val unit   = TimeUnit.of(m.group("unit"))
            m.group("prefix") match {
              case "-" | "last" =>
                TimeVector(-length, 0, unit)
              case null | "+" | "next" =>
                TimeVector(length, 0, unit)
              case other =>
                throw new IllegalArgumentException(s"Unknown duration prefix: ${other}")
            }
        }
    }
  }
}
