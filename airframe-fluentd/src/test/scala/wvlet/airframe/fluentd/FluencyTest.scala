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
import wvlet.airframe.{AirframeSpec, bind}
import xerial.fluentd.FluentdStandalone
import wvlet.airframe._
import wvlet.log.io.IOUtil

trait FluentdStandaloneService {
  val fluentdServer = bind[FluentdStandalone]
    .onStart(_.startAndAwait)
    .onShutdown(_.stop)
}

trait MetricLoggingService extends FluentdStandaloneService {
  val client  = bind[FluentdClient]
  val factory = bind[MetricLoggerFactory]
}

case class FluencyMetric(id: Int, name: String)

/**
  *
  */
class FluencyTest extends AirframeSpec {
  val fluentdPort = IOUtil.randomPort
  val d = fluentd
    .withFluency(port = fluentdPort)
    .bind[FluentdStandalone].toInstance(new FluentdStandalone(fluentdPort))
    .noLifeCycleLogging

  "should send metrics to fluentd through Fluency" in {
    d.build[MetricLoggingService] { f =>
      // Use a regular emit method
      f.client.emit("mytag", Map("data" -> "hello"))

      // Use object metric logger
      val l = f.factory.newMetricLogger[FluencyMetric]("fluency_metric")
      l.emit(FluencyMetric(1, "leo"))
    }
  }
}
