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

import java.time.temporal.ChronoUnit
import java.time.{DayOfWeek, ZonedDateTime}

/**
  *
  */
sealed abstract class TimeUnit(symbol: String) {

  /**
    * Truncate the given time to this unit
    */
  def truncate(t: ZonedDateTime): ZonedDateTime
  def increment(a: ZonedDateTime, v: Long): ZonedDateTime
}

object TimeUnit {

  private val unitTable: Map[String, TimeUnit] = Map(
    "s" -> TimeUnit.Second,
    "m" -> TimeUnit.Minute,
    "d" -> TimeUnit.Day,
    "h" -> TimeUnit.Hour,
    "w" -> TimeUnit.Week,
    "M" -> TimeUnit.Month,
    "q" -> TimeUnit.Quarter,
    "y" -> TimeUnit.Year
  )

  def of(s: String): TimeUnit = {
    unitTable.getOrElse(s, throw new IllegalArgumentException(s"Unknown unit type ${s}"))
  }

  case object Second extends TimeUnit("s") {
    override def truncate(t: ZonedDateTime): ZonedDateTime = {
      t.truncatedTo(ChronoUnit.SECONDS)
    }
    override def increment(a: ZonedDateTime, v: Long): ZonedDateTime = {
      a.plus(v, ChronoUnit.SECONDS)
    }
  }
  case object Minute extends TimeUnit("m") {
    override def truncate(t: ZonedDateTime): ZonedDateTime = {
      t.truncatedTo(ChronoUnit.MINUTES)
    }
    override def increment(a: ZonedDateTime, v: Long): ZonedDateTime = {
      a.plus(v, ChronoUnit.MINUTES)
    }
  }
  case object Hour extends TimeUnit("h") {
    override def truncate(t: ZonedDateTime): ZonedDateTime = {
      t.truncatedTo(ChronoUnit.HOURS)
    }
    override def increment(a: ZonedDateTime, v: Long): ZonedDateTime = {
      a.plus(v, ChronoUnit.HOURS)
    }
  }
  case object Day extends TimeUnit("d") {
    override def truncate(t: ZonedDateTime): ZonedDateTime = {
      t.truncatedTo(ChronoUnit.DAYS)
    }
    override def increment(a: ZonedDateTime, v: Long): ZonedDateTime = {
      a.plus(v, ChronoUnit.DAYS)
    }
  }
  case object Week extends TimeUnit("w") {
    override def truncate(t: ZonedDateTime): ZonedDateTime = {
      t.truncatedTo(ChronoUnit.DAYS) // Truncate to the beginning of the day
        .`with`(DayOfWeek.MONDAY) // Monday-origin week is the ISO standard
    }
    override def increment(a: ZonedDateTime, v: Long): ZonedDateTime = {
      a.plus(v, ChronoUnit.WEEKS)
    }
  }
  case object Month extends TimeUnit("M") {
    override def truncate(t: ZonedDateTime): ZonedDateTime = {
      t.withDayOfMonth(1) // Jump to the beginning of the day
        .truncatedTo(ChronoUnit.DAYS)
    }
    override def increment(a: ZonedDateTime, v: Long): ZonedDateTime = {
      a.plus(v, ChronoUnit.MONTHS)
    }
  }
  case object Quarter extends TimeUnit("q") {
    override def truncate(t: ZonedDateTime): ZonedDateTime = {
      // The first quarter of the year is January
      val quarter = ((t.getMonthValue - 1) / 3)
      val month   = 3 * quarter
      t.withDayOfYear(1)
        .plusMonths(month)
        .truncatedTo(ChronoUnit.DAYS)
    }
    override def increment(a: ZonedDateTime, v: Long): ZonedDateTime = {
      val currentMonth = a.getMonthValue
      val quarter      = ((a.getMonthValue - 1) / 3) + v
      val targetMonth  = (3 * quarter) + 1
      a.plus(targetMonth - currentMonth, ChronoUnit.MONTHS)
    }
  }

  case object Year extends TimeUnit("y") {
    override def truncate(t: ZonedDateTime): ZonedDateTime = {
      t.withDayOfYear(1)
        .truncatedTo(ChronoUnit.DAYS)
    }
    override def increment(a: ZonedDateTime, v: Long): ZonedDateTime = {
      a.plus(v, ChronoUnit.YEARS)
    }
  }
}
