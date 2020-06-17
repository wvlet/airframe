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

import wvlet.airspec.AirSpec

/**
  */
class TimeVectorTest extends AirSpec {
  def `succinct time window unit`: Unit = {
    // 2020 have 366 days, so we need to fix the offset
    val t = TimeWindow.withUTC.withOffset("2019-01-02 01:23:45")

    def check(x: String, expected: String): Unit = {
      val w = t.parse(x)
      val s = TimeVector.succinctTimeVector(w.startUnixTime, w.endUnixTime)
      s.toDurationString shouldBe expected
    }

    val lst = Seq(
      "-1d"            -> "1d",
      "-7d/0w"         -> "1w",
      "-1w"            -> "1w",
      "-20d"           -> "20d",
      "30d/2019-01-01" -> "30d",
      "31d/2019-01-01" -> "1M",
      "+2M/2019-01-01" -> "2M",
      "-40d"           -> "40d",
      "-1M"            -> "1M",
      "-1M/0y"         -> "1M",
      "-2M"            -> "2M",
      "-356d"          -> "356d",
      "-366d"          -> "366d",
      "-365d"          -> "1y",
      "-1q"            -> "1q",
      "+5q"            -> "5q",
      "-1y"            -> "1y"
    )

    lst.foreach(x => check(x._1, x._2))
  }
}
