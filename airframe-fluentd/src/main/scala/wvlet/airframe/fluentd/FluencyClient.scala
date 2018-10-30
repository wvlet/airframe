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
import java.time.Instant

import org.komamitsu.fluency.{EventTime, Fluency}
import wvlet.airframe._
import wvlet.log.LogSupport

case class FluencyConfig(
    // Use the extended EventTime timestamps
    // https://github.com/fluent/fluentd/wiki/Forward-Protocol-Specification-v1#eventtime-ext-format
    useExtendedEventTime: Boolean = false,
    fluencyConfig: Fluency.Config = new Fluency.Config()
)

object FluencyClient {
  def newFluency(fluentdConfig: FluentdConfig, fluencyConfig: FluencyConfig): Fluency = {
    Fluency.defaultFluency(fluentdConfig.host, fluentdConfig.port, fluencyConfig.fluencyConfig)
  }
}

trait FluencyClient extends FluentdClient with LogSupport {
  private val fluentdConfig = bind[FluentdConfig]
  private val fluencyConfig = bind[FluencyConfig]
  private val fluency: Fluency = bind { (fluentdConfig: FluentdConfig, fluencyConfig: FluencyConfig) =>
    FluencyClient.newFluency(fluentdConfig, fluencyConfig)
  }.onStart { x =>
      info(s"Starting Fluency")
    }
    .onShutdown { x =>
      info(s"Stopping Fluency")
      close
    }

  def close: Unit = {
    fluency.flush()
    fluency.close()
  }

  protected override def emitRaw(fullTag: String, event: Map[String, Any]): Unit = {
    if (fluencyConfig.useExtendedEventTime) {
      val now       = Instant.now()
      val eventTime = EventTime.fromEpoch(now.getEpochSecond.toInt, now.getNano.toInt);
      fluency.emit(fullTag, eventTime, toJavaMap(event))
    } else {
      fluency.emit(fullTag, toJavaMap(event))
    }
  }
}
