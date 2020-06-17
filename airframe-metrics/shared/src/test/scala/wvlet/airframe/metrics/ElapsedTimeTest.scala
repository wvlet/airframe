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

import scala.concurrent.duration.TimeUnit
import scala.concurrent.duration._

/**
  */
class ElapsedTimeTest extends AirSpec {
  scalaJsSupport

  case class Example(str: String, value: Double, unit: TimeUnit)

  val examples = Seq(
    Example("1234 ns", 1234, NANOSECONDS),
    Example("1234 us", 1234, MICROSECONDS),
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

  case class ConversionExample(inputUnit: TimeUnit, targetUnit: TimeUnit, factor: Double)

  val conversionExamples = Seq(
    ConversionExample(NANOSECONDS, NANOSECONDS, 1.0),
    ConversionExample(NANOSECONDS, MILLISECONDS, 1.0 / 1000000),
    ConversionExample(NANOSECONDS, SECONDS, 1.0 / 1000000 / 1000),
    ConversionExample(NANOSECONDS, MINUTES, 1.0 / 1000000 / 1000 / 60),
    ConversionExample(NANOSECONDS, HOURS, 1.0 / 1000000 / 1000 / 60 / 60),
    ConversionExample(NANOSECONDS, DAYS, 1.0 / 1000000 / 1000 / 60 / 60 / 24),
    ConversionExample(MILLISECONDS, NANOSECONDS, 1000000.0),
    ConversionExample(MILLISECONDS, MILLISECONDS, 1.0),
    ConversionExample(MILLISECONDS, SECONDS, 1.0 / 1000),
    ConversionExample(MILLISECONDS, MINUTES, 1.0 / 1000 / 60),
    ConversionExample(MILLISECONDS, HOURS, 1.0 / 1000 / 60 / 60),
    ConversionExample(MILLISECONDS, DAYS, 1.0 / 1000 / 60 / 60 / 24),
    ConversionExample(SECONDS, NANOSECONDS, 1000000.0 * 1000),
    ConversionExample(SECONDS, MILLISECONDS, 1000),
    ConversionExample(SECONDS, SECONDS, 1),
    ConversionExample(SECONDS, MINUTES, 1.0 / 60),
    ConversionExample(SECONDS, HOURS, 1.0 / 60 / 60),
    ConversionExample(SECONDS, DAYS, 1.0 / 60 / 60 / 24),
    ConversionExample(MINUTES, NANOSECONDS, 1000000.0 * 1000 * 60),
    ConversionExample(MINUTES, MILLISECONDS, 1000 * 60),
    ConversionExample(MINUTES, SECONDS, 60),
    ConversionExample(MINUTES, MINUTES, 1),
    ConversionExample(MINUTES, HOURS, 1.0 / 60),
    ConversionExample(MINUTES, DAYS, 1.0 / 60 / 24),
    ConversionExample(HOURS, NANOSECONDS, 1000000.0 * 1000 * 60 * 60),
    ConversionExample(HOURS, MILLISECONDS, 1000 * 60 * 60),
    ConversionExample(HOURS, SECONDS, 60 * 60),
    ConversionExample(HOURS, MINUTES, 60),
    ConversionExample(HOURS, HOURS, 1),
    ConversionExample(HOURS, DAYS, 1.0 / 24),
    ConversionExample(DAYS, NANOSECONDS, 1000000.0 * 1000 * 60 * 60 * 24),
    ConversionExample(DAYS, MILLISECONDS, 1000 * 60 * 60 * 24),
    ConversionExample(DAYS, SECONDS, 60 * 60 * 24),
    ConversionExample(DAYS, MINUTES, 60 * 24),
    ConversionExample(DAYS, HOURS, 24),
    ConversionExample(DAYS, DAYS, 1)
  )
  import ElapsedTime._

  def `parse time`: Unit = {
    examples.foreach { x => ElapsedTime(x.str) shouldBe ElapsedTime(x.value, x.unit) }
  }

  def `print string`: Unit = {
    examples.foreach { x => ElapsedTime(x.str).toString shouldBe f"${x.value}%.2f${timeUnitToString(x.unit)}" }
  }

  def `throw exception for invalid inputs`: Unit = {
    val list = Seq("-1", "1.23g", "1x4")

    list.foreach { x =>
      intercept[IllegalArgumentException] {
        ElapsedTime(x)
      }
    }
  }

  def `validate invalid input`: Unit = {
    val in = Seq(
      (-1d, SECONDS),
      (Double.PositiveInfinity, SECONDS),
      (Double.NegativeInfinity, SECONDS),
      (Double.NaN, SECONDS)
    )

    in.foreach { x =>
      intercept[IllegalArgumentException] {
        ElapsedTime(x._1, x._2)
      }
    }
  }

  def `convert from nanos`: Unit = {
    succinctNanos(123).toString shouldBe ElapsedTime(123, NANOSECONDS).toString
    succinctNanos(123456).toString shouldBe ElapsedTime(123.456, MICROSECONDS).toString
    succinctNanos(SECONDS.toNanos(300)).toString shouldBe ElapsedTime(5, MINUTES).toString

    succinctDuration(123, NANOSECONDS).toString shouldBe ElapsedTime(123, NANOSECONDS).toString
    succinctDuration(123456, NANOSECONDS).toString shouldBe ElapsedTime(123.456, MICROSECONDS).toString
    succinctDuration(300, SECONDS).toString shouldBe ElapsedTime(5, MINUTES).toString
  }

  def `extract values in given time units`: Unit = {
    val millis   = 12346789.0d
    var duration = ElapsedTime(millis, MILLISECONDS)
    duration.valueIn(MILLISECONDS) shouldBe millis
    assertEquals(duration.valueIn(SECONDS), millis / 1000, 0.001)
    assertEquals(duration.valueIn(MINUTES), millis / 1000 / 60, 0.001)
    assertEquals(duration.valueIn(HOURS), millis / 1000 / 60 / 60, 0.001)
    assertEquals(duration.valueIn(DAYS), millis / 1000 / 60 / 60 / 24, 0.001)

    val days = 3.0
    duration = ElapsedTime(days, DAYS)
    duration.valueIn(DAYS) shouldBe days
    assertEquals(duration.valueIn(HOURS), days * 24, 0.001)
    assertEquals(duration.valueIn(MINUTES), days * 24 * 60, 0.001)
    assertEquals(duration.valueIn(SECONDS), days * 24 * 60 * 60, 0.001)
    assertEquals(duration.valueIn(MILLISECONDS), days * 24 * 60 * 60 * 1000, 0.001)
  }

  def `support toMillis`: Unit = {
    examples.map(x => parse(x.str)).foreach { x => assertEquals(x.toMillis, x.roundTo(MILLISECONDS), 0.001) }
  }

  def `convert units`: Unit = {
    for (c <- conversionExamples) {
      val duration = ElapsedTime(1, c.inputUnit).convertTo(c.targetUnit)
      duration.unit shouldBe c.targetUnit
      assertEquals(duration.value, c.factor, (c.factor * 0.001))
      assertEquals(duration.valueIn(c.targetUnit), c.factor, (c.factor * 0.001))
    }
  }

  def `convert to succinct units`: Unit = {
    for (c <- conversionExamples) {
      val duration = ElapsedTime(c.factor, c.targetUnit)
      val actual   = duration.convertToMostSuccinctTimeUnit
      assertEquals(actual.valueIn(c.targetUnit), c.factor, c.factor * 0.001)
      assertEquals(actual.valueIn(c.inputUnit), 1.0, 0.001)
      if (actual.unit != c.inputUnit) {
        actual.unit shouldBe c.inputUnit
      }
    }
  }

  def `be comparable`: Unit = {
    assert(parse("1d").compareTo(parse("1.1d")) <= 0)
    assert(parse("1h").compareTo(parse("1d")) <= 0)
  }

  def `support rounding`: Unit = {}
}
