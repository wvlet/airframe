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
import wvlet.airframe.{AirframeSpec, fluentd}

case class SampleMetric(time: Long, value: String)
case class NestedMetric(message: String, data: Seq[Int], opt: Option[String], sample: SampleMetric)

/**
  *
  */
class MetricLoggerTest extends AirframeSpec {
  val d = fluentd.withConsoleLogging.noLifeCycleLogging

  "generate MetricLogger for case classes" in {
    d.build[MetricLoggerFactory] { f =>
      val l = f.newMetricLogger[SampleMetric](tag = "sample")
      l.emit(SampleMetric(100000, "hello"))
      l.emit(SampleMetric(100001, "fluentd"))
    }
  }

  "support nested metrics" in {
    d.build[MetricLoggerFactory] { f =>
      val l = f.newMetricLogger[NestedMetric](tag = "nested")
      l.emit(NestedMetric("test nested logs", Seq(1, 2, 3), None, SampleMetric(100002, "I'm happy")))
      l.emit(
        NestedMetric("test options",
                     Seq(10, 20),
                     Some("optional value"),
                     SampleMetric(100003, "option value is also supported")))
    }
  }
}
