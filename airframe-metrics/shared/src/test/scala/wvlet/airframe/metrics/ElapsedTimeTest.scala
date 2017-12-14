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

import wvlet.airframe.AirframeSpec

import scala.concurrent.duration.TimeUnit
import scala.concurrent.duration._

/**
  *
  */
class ElapsedTimeTest extends AirframeSpec {

  case class Example(str: String, value: Double, unit: TimeUnit)

  val examples = Seq(
    Example("1234 ns", 1234, NANOSECONDS),
    Example("1234 ms", 1234, MILLISECONDS),
    Example("1234 s", 1234, SECONDS),
    Example("1234 m", 1234, MINUTES),
    Example("1234 h", 1234, HOURS),
    Example("1234 d", 1234, DAYS),
    Example("1234.567 ns", 1234.567, NANOSECONDS),
    Example("1234.567 ms", 1234.567, MILLISECONDS),
    Example("1234.567 s", 1234.567, SECONDS),
    Example("1234.567 m", 1234.567, MINUTES),
    Example("1234.567 h", 1234.567, HOURS),
    Example("1234.567 d", 1234.567, DAYS),
    Example("1234ns", 1234, NANOSECONDS),
    Example("1234ms", 1234, MILLISECONDS),
    Example("1234s", 1234, SECONDS),
    Example("1234m", 1234, MINUTES),
    Example("1234h", 1234, HOURS),
    Example("1234d", 1234, DAYS),
    Example("1234.567ns", 1234.567, NANOSECONDS),
    Example("1234.567ms", 1234.567, MILLISECONDS),
    Example("1234.567s", 1234.567, SECONDS),
    Example("1234.567m", 1234.567, MINUTES),
    Example("1234.567h", 1234.567, HOURS),
    Example("1234.567d", 1234.567, DAYS)
  )

  "ElapsedTime" should {
    "parse time" in {
      examples.foreach { x =>
        ElapsedTime.parse(x.str) shouldBe ElapsedTime(x.value, x.unit)
      }
    }

  }
}
