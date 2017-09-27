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

import java.time.{ZoneOffset, ZonedDateTime}
import java.util.TimeZone

import wvlet.airframe.AirframeSpec

/**
  *
  */
class TimeParserTest extends AirframeSpec {

  def parse(str: String, expected: String) {
    val z   = TimeParser.parse(str, TimeWindow.UTC)
    val ans = ZonedDateTime.parse(expected)

    if (z.isEmpty) {
      warn(s"failed to parse ${str}")
    }
    z shouldBe defined

    TimeStampFormatter.formatTimestamp(z.get) shouldBe TimeStampFormatter.formatTimestamp(ans)
  }

  "TimeParser" should {

    "parse date time" in {

      // Time with time zone
      parse("2017-01-01 23:01:23-0700", "2017-01-01T23:01:23-07:00")
      parse("2017-01-01 23:01:23-07:00", "2017-01-01T23:01:23-07:00")
      parse("2017-01-01 00:00:00 UTC", "2017-01-01T00:00:00Z")
      parse("2017-01-01 01:23:45Z", "2017-01-01T01:23:45Z")
      parse("2017-01-01 01:23:45+0900", "2017-01-01T01:23:45+09:00")

      // PDT
      parse("2017-01-01 00:00:00 America/Los_Angeles", "2017-01-01T00:00:00-08:00")

      // PST
      parse("2017-05-01 00:00:00 America/Los_Angeles", "2017-05-01T00:00:00-07:00")

      // Date only strings
      // UTC
      parse("2017-01-01", "2017-01-01T00:00:00Z")
      parse("2016-12-01", "2016-12-01T00:00:00Z")

      // Datetime without time zone
      parse("2016-12-01 08:00:01", "2016-12-01T08:00:01Z")
      parse("2016-12-01 08:00:01", "2016-12-01T08:00:01Z")
    }

    "use proper time zone" in {
      val default = TimeZone.getDefault
      try {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
        val t = TimeParser.parse("2017-04-04", ZoneOffset.of("-07:00"))
        info(t)
        val w = TimeWindow.withZone("PDT")
        val d = w.parse("3d/2017-04-07")
        info(d)
      } finally {
        TimeZone.setDefault(default)
      }
    }

  }
}
