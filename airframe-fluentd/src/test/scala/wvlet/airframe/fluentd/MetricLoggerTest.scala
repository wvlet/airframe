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
package wvlet.airframe.fluentd
import java.lang.reflect.InvocationTargetException

import wvlet.airframe.{Design, fluentd}
import wvlet.airspec.AirSpec

case class SampleMetric(time: Long, value: String) extends TaggedMetric {
  def metricTag = "sample"
}
case class NestedMetric(message: String, data: Seq[Int], opt: Option[String], sample: SampleMetric)
    extends TaggedMetric {
  def metricTag = "nested"
}

case class ErrorMetric(errorType: String, ex: Exception) extends TaggedMetric {
  def metricTag = "error_log"
}

/**
  */
class MetricLoggerTest extends AirSpec {
  override protected def design: Design = {
    fluentd.withDebugConsoleLogging
  }

  def `generate MetricLogger for case classes`(f: MetricLoggerFactory): Unit = {
    val l = f.getTypedLogger[SampleMetric]
    l.emit(SampleMetric(100000, "hello"))
    l.emit(SampleMetric(100001, "fluentd"))

    val ll = f.getTypedLoggerWithTagPrefix[NestedMetric]("extended")
    ll.emit(NestedMetric("test nested logs", Seq(1, 2, 3), None, SampleMetric(100002, "I'm happy")))
  }

  def `support nested metrics`(f: MetricLoggerFactory): Unit = {
    val l = f.getTypedLogger[NestedMetric]
    l.emit(NestedMetric("test nested logs", Seq(1, 2, 3), None, SampleMetric(100002, "I'm happy")))
    l.emit(
      NestedMetric(
        "test options",
        Seq(10, 20),
        Some("optional value"),
        SampleMetric(100003, "option value is also supported")
      )
    )
  }

  def `support exception stack trace metrics`(f: MetricLoggerFactory): Unit = {
    val l = f.getTypedLogger[ErrorMetric]
    l.emit(ErrorMetric("illegal_argument", new IllegalArgumentException("invalid input")))
    l.emit(ErrorMetric("remote error", new InvocationTargetException(new IllegalStateException("unknown error"))))
  }
}
