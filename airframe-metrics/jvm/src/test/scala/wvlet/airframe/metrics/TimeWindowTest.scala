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

import java.time.ZoneOffset
import java.util.TimeZone

import org.scalatest.exceptions.TestFailedException
import wvlet.airframe.AirframeSpec

/**
  *
  */
class TimeWindowTest extends AirframeSpec {

  val t    = TimeWindow.withTimeZone("PDT").withOffset("2016-06-26 01:23:45-0700")
  val zone = t.zone
  debug(s"now: ${t.now}")

  val defaultTimeZone = TimeZone.getDefault

  override protected def beforeAll(): Unit = {
    TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
  }

  override protected def afterAll(): Unit = {
    TimeZone.setDefault(defaultTimeZone)
  }

  def parse(s: String, expected: String): TimeWindow = {
    val w  = t.parse(s)
    val ws = w.toString // Check toString
    debug(s"str:${s}, window:${ws}")
    try {
      w.toStringAt(zone) shouldBe expected
    } catch {
      case e: TestFailedException =>
        warn(s"Failed parsing: string:${s}\nwindow:   ${ws}\nexpected: ${expected}")
        throw e
    }

    w
  }

  "TimeWindow" should {

    "parse string repl" in {
      // duration/offset

      // DURATION := (+ | -)?(INTEGER)(UNIT)
      // UNIT     := s | m | h | d | w | M | y
      //
      // OFFSET   := DURATION | DATE_TIME
      // RANGE    := (DURATION) (/ (OFFSET))?
      // DATE_TIME := yyyy-MM-dd( HH:mm:ss(.ZZZ| ' ' z)?)?
      //

      // The following tests are the results if the current time is 2016-06-26 01:23:45-0700

      // The default offset is 0(UNIT) (the beginning of the given time unit)

      // 0 means no duration from the beginning of specified time unit
      parse("0h", "[2016-06-26 01:00:00-0700,2016-06-26 01:00:00-0700)")
      parse("0d", "[2016-06-26 00:00:00-0700,2016-06-26 00:00:00-0700)")
      parse("0M", "[2016-06-01 00:00:00-0700,2016-06-01 00:00:00-0700)")

      // 1 hour from the beginning of today
      parse("1h", "[2016-06-26 01:00:00-0700,2016-06-26 02:00:00-0700)")
      // today
      parse("1d", "[2016-06-26 00:00:00-0700,2016-06-27 00:00:00-0700)")
      // this month
      parse("1M", "[2016-06-01 00:00:00-0700,2016-07-01 00:00:00-0700)")

      // 7 days ago until at the beginning of today.
      // 0d := the beginning of the day
      // [-7d, 0d)
      // |-------------|
      // -7d -- ... -- 0d ---- now  ------
      parse("-7d", "[2016-06-19 00:00:00-0700,2016-06-26 00:00:00-0700)")

      // Since 7 days ago + time fragment from [-7d, now)
      //  |-------------------|
      // -7d - ... - 0d ---- now  ------
      parse("-7d/now", "[2016-06-19 00:00:00-0700,2016-06-26 01:23:45-0700)")

      // '+' indicates forward time range
      // +7d = [0d, +7d)
      //      |------------------------------|
      // ---  0d --- now --- 1d ---  ... --- 7da
      parse("+7d", "[2016-06-26 00:00:00-0700,2016-07-03 00:00:00-0700)")
      // We can omit '+' sign
      parse("7d", "[2016-06-26 00:00:00-0700,2016-07-03 00:00:00-0700)")

      // [now, +7d)
      //         |---------------------|
      // 0d --- now --- 1d ---  ... --- 7d
      parse("+7d/now", "[2016-06-26 01:23:45-0700,2016-07-03 00:00:00-0700)")

      // [-1h, 0h)
      parse("-1h", "[2016-06-26 00:00:00-0700,2016-06-26 01:00:00-0700)")
      // [-1h, now)
      parse("-1h/now", "[2016-06-26 00:00:00-0700,2016-06-26 01:23:45-0700)")
      parse("-1h/0m", "[2016-06-26 00:00:00-0700,2016-06-26 01:23:00-0700)")

      // -12h/now  (last 12 hours + fraction until now)

      parse("-12h/now", "[2016-06-25 13:00:00-0700,2016-06-26 01:23:45-0700)")
      parse("-12h", "[2016-06-25 13:00:00-0700,2016-06-26 01:00:00-0700)")
      parse("-12h/now", "[2016-06-25 13:00:00-0700,2016-06-26 01:23:45-0700)")
      parse("+12h/now", "[2016-06-26 01:23:45-0700,2016-06-26 13:00:00-0700)")

      // Absolute offset
      // 3d:2017-04-07 [2017-04-04,2017-04-07)
      parse("-3d/2017-04-07", "[2017-04-04 00:00:00-0700,2017-04-07 00:00:00-0700)")

      // The offset can be specified using a duration
      // -1M:-1M  [2017-04-01, 2017-05-01) if today is 2017-05-20
      parse("-1M/0M", "[2016-05-01 00:00:00-0700,2016-06-01 00:00:00-0700)")
      // -1M:-1M  [2017-03-01, 2017-04-01) if today is 2017-05-20
      parse("-1M/-1M", "[2016-04-01 00:00:00-0700,2016-05-01 00:00:00-0700)")
      parse("-1M/lastMonth", "[2016-04-01 00:00:00-0700,2016-05-01 00:00:00-0700)")

      // Offset dates can be arbitrary time units
      parse("-1M/2018-09-02", "[2018-08-01 00:00:00-0700,2018-09-01 00:00:00-0700)")
      parse("-1M/2018-09-02 01:12:13", "[2018-08-01 00:00:00-0700,2018-09-01 00:00:00-0700)")
      parse("-1h/2017-01-23 01:00:00", "[2017-01-23 00:00:00-0700,2017-01-23 01:00:00-0700)")
      parse("-1h/2017-01-23 01:23:45", "[2017-01-23 00:00:00-0700,2017-01-23 01:00:00-0700)")
      parse("-60m/2017-01-23 01:23:45", "[2017-01-23 00:23:00-0700,2017-01-23 01:23:00-0700)")

      // Untruncate offset if it ends with ")"
      parse("0M/2018-09-02)", "[2018-09-01 00:00:00-0700,2018-09-02 00:00:00-0700)")
      parse("-1M/2018-09-02 12:34:56)", "[2018-08-01 00:00:00-0700,2018-09-02 12:34:56-0700)")
      parse("+1M/2018-09-02)", "[2018-09-02 00:00:00-0700,2018-10-01 00:00:00-0700)")

      // If different units are used for duration and offset, try to extend to the range to the offset unit
      parse("-1M/0d", "[2016-05-01 00:00:00-0700,2016-06-26 00:00:00-0700)")
      parse("-1M/0h", "[2016-05-01 00:00:00-0700,2016-06-26 01:00:00-0700)")
      parse("-1M/0m", "[2016-05-01 00:00:00-0700,2016-06-26 01:23:00-0700)")
      parse("-1M/0s", "[2016-05-01 00:00:00-0700,2016-06-26 01:23:45-0700)")

      // quarter
      parse("-1q", "[2016-01-01 00:00:00-0700,2016-04-01 00:00:00-0700)")
      parse("1q", "[2016-04-01 00:00:00-0700,2016-07-01 00:00:00-0700)")
      parse("-2q", "[2015-10-01 00:00:00-0700,2016-04-01 00:00:00-0700)")
      parse("-1q/0y", "[2015-10-01 00:00:00-0700,2016-01-01 00:00:00-0700)")
    }

    "support human-friendly range" in {
      parse("today", "[2016-06-26 00:00:00-0700,2016-06-27 00:00:00-0700)")
      parse("today/now", "[2016-06-26 00:00:00-0700,2016-06-26 01:23:45-0700)")
      parse("thisHour", "[2016-06-26 01:00:00-0700,2016-06-26 02:00:00-0700)")
      parse("thisWeek", "[2016-06-20 00:00:00-0700,2016-06-27 00:00:00-0700)")
      parse("thisMonth", "[2016-06-01 00:00:00-0700,2016-07-01 00:00:00-0700)")
      parse("thisMonth/now", "[2016-06-01 00:00:00-0700,2016-06-26 01:23:45-0700)")
      parse("thisYear", "[2016-01-01 00:00:00-0700,2017-01-01 00:00:00-0700)")

      parse("yesterday", "[2016-06-25 00:00:00-0700,2016-06-26 00:00:00-0700)")
      parse("yesterday/now", "[2016-06-25 00:00:00-0700,2016-06-26 01:23:45-0700)")
      parse("lastHour", "[2016-06-26 00:00:00-0700,2016-06-26 01:00:00-0700)")
      parse("lastWeek", "[2016-06-13 00:00:00-0700,2016-06-20 00:00:00-0700)")
      parse("lastMonth", "[2016-05-01 00:00:00-0700,2016-06-01 00:00:00-0700)")
      parse("lastYear", "[2015-01-01 00:00:00-0700,2016-01-01 00:00:00-0700)")

      parse("tomorrow", "[2016-06-27 00:00:00-0700,2016-06-28 00:00:00-0700)")
      parse("tomorrow/now", "[2016-06-26 01:23:45-0700,2016-06-28 00:00:00-0700)")
      parse("nextHour", "[2016-06-26 02:00:00-0700,2016-06-26 03:00:00-0700)")
      parse("nextWeek", "[2016-06-27 00:00:00-0700,2016-07-04 00:00:00-0700)")
      parse("nextMonth", "[2016-07-01 00:00:00-0700,2016-08-01 00:00:00-0700)")
      parse("nextYear", "[2017-01-01 00:00:00-0700,2018-01-01 00:00:00-0700)")
    }

    "split time windows" in {
      val weeks = t.parse("5w").splitIntoWeeks
      debug(weeks.mkString("\n"))

      val weeks2 = t.parse("-5w/2017-06-01").splitIntoWeeks
      debug(weeks2.mkString("\n"))

      val months = t.parse("thisYear/thisMonth").splitIntoMonths
      debug(months.mkString("\n"))
      val months2 = t.parse("thisYear/0M").splitIntoMonths
      debug(months2.mkString("\n"))

      val days = t.parse("thisMonth").splitIntoWeeks
      debug(days.mkString("\n"))
    }

    "parse timezone" in {
      // Sanity tests
      TimeWindow.withTimeZone("UTC")
      TimeWindow.withTimeZone("PST")
      TimeWindow.withTimeZone("PDT")
      TimeWindow.withTimeZone("JST")
      TimeWindow.withTimeZone("EDT")
      TimeWindow.withTimeZone("BST")
      TimeWindow.withTimeZone("CDT")
      TimeWindow.withTimeZone("MDT")
    }

    "use proper time zone" in {
      val default = TimeZone.getDefault
      try {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
        val t = TimeParser.parse("2017-04-04", ZoneOffset.of("-07:00"))
        debug(t)
        val w = TimeWindow.withTimeZone("PDT")
        val d = w.parse("-3d/2017-04-07")
        debug(d)
      } finally {
        TimeZone.setDefault(default)
      }
    }

  }

}
