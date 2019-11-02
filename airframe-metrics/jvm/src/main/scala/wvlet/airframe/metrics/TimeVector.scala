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

import wvlet.airframe.metrics.TimeWindow.withUTC

import scala.annotation.tailrec

case class TimeVector(x: Long, offset: Long, unit: TimeWindowUnit) {
  override def toString: String = toDurationString

  def toDurationString = {
    if (offset == 0) {
      s"${x}${unit.symbol}"
    } else {
      s"${x}${unit.symbol}/${offset}${unit.symbol}"
    }
  }

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
      // thisXXXX is a special time range and needs to be a backward range to include the current time
      // even after truncating with `now` offset.
      //             now
      //   |----------x----------|
      //   <---------------------| x = -1, 1 unit distance from the offset
      //  grid (offset=0)  offset = 1
      case "thisHour"  => TimeVector(-1, 1, TimeWindowUnit.Hour)
      case "today"     => TimeVector(-1, 1, TimeWindowUnit.Day)
      case "thisWeek"  => TimeVector(-1, 1, TimeWindowUnit.Week)
      case "thisMonth" => TimeVector(-1, 1, TimeWindowUnit.Month)
      case "thisYear"  => TimeVector(-1, 1, TimeWindowUnit.Year)
      // past
      case "lastHour"  => TimeVector(-1, 0, TimeWindowUnit.Hour)
      case "yesterday" => TimeVector(-1, 0, TimeWindowUnit.Day)
      case "lastWeek"  => TimeVector(-1, 0, TimeWindowUnit.Week)
      case "lastMonth" => TimeVector(-1, 0, TimeWindowUnit.Month)
      case "lastYear"  => TimeVector(-1, 0, TimeWindowUnit.Year)
      // future
      case "nextHour"  => TimeVector(1, 1, TimeWindowUnit.Hour)
      case "tomorrow"  => TimeVector(1, 1, TimeWindowUnit.Day)
      case "nextWeek"  => TimeVector(1, 1, TimeWindowUnit.Week)
      case "nextMonth" => TimeVector(1, 1, TimeWindowUnit.Month)
      case "nextYear"  => TimeVector(1, 1, TimeWindowUnit.Year)

      case other =>
        durationPattern.findFirstMatchIn(s) match {
          case None =>
            throw new IllegalArgumentException(s"Invalid duration: ${s}")
          case Some(m) =>
            val length = m.group("num").toInt
            val unit   = TimeWindowUnit.of(m.group("unit"))
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

  /**
    * Compute the most succinct TimeVector to represent a time range [start unixtime, end unixtime)
    */
  def succinctTimeVector(startUnixTime: Long, endUnixTime: Long): TimeVector = {
    val r          = withUTC.fromRange(startUnixTime, endUnixTime)
    val secondDiff = (endUnixTime - startUnixTime).toDouble

    @tailrec
    def loop(unitsToUse: List[TimeWindowUnit]): TimeVector = {
      if (unitsToUse.isEmpty) {
        TimeVector(endUnixTime - startUnixTime, 0, TimeWindowUnit.Second)
      } else {
        val unit     = unitsToUse.head
        val numUnits = r.howMany(unit)

        val startTruncated      = unit.truncate(r.start)
        val endTruncated        = unit.truncate(r.end)
        val truncated           = TimeWindow(startTruncated, endTruncated)
        val truncatedSecondDiff = truncated.secondDiff

        if (numUnits > 0 && ((secondDiff - truncatedSecondDiff) / (numUnits * unit.secondsInUnit)).abs <= 0.001) {
          TimeVector(numUnits, 0, unit)
        } else {
          loop(unitsToUse.tail)
        }
      }
    }

    // Find the largest unit first from Year to Second
    loop(TimeWindowUnit.units.reverse)
  }
}
