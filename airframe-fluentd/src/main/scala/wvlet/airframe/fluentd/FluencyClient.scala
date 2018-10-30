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
import scala.collection.JavaConverters._
import wvlet.airframe._

/**
  *
  */
object FluencyClient {
  def newFluency(fluentdConfig: FluentdConfig, fluencyConfig: FluencyConfig): Fluency = {
    Fluency.defaultFluency(fluentdConfig.host, fluentdConfig.port, fluencyConfig.fluencyConfig)
  }
}

case class FluencyConfig(
    // Use the extended EventTime timestamps
    // https://github.com/fluent/fluentd/wiki/Forward-Protocol-Specification-v1#eventtime-ext-format
    useExtendedEventTime: Boolean = false,
    fluencyConfig: Fluency.Config = new Fluency.Config()
)

trait FluencyClient extends FluentdClient {
  private val fluentdConfig = bind[FluentdConfig]
  private val fluencyConfig = bind[FluencyConfig]
  private val fluency: Fluency = bind { (fluentdConfig: FluentdConfig, fluencyConfig: FluencyConfig) =>
    FluencyClient.newFluency(fluentdConfig, fluencyConfig)
  }.onShutdown { x =>
    close
  }

  def close: Unit = {
    fluency.flush()
    fluency.close()
  }

  def emit(tag: String, event: Map[String, AnyRef]): Unit = {
    val fullTag = if (fluentdConfig.tagPrefix.isEmpty) {
      tag
    } else {
      s"${fluentdConfig.tagPrefix}.${tag}"
    }

    if (fluencyConfig.useExtendedEventTime) {
      val now       = Instant.now()
      val eventTime = EventTime.fromEpoch(now.getEpochSecond.toInt, now.getNano.toInt);
      fluency.emit(fullTag, eventTime, event.asJava)
    } else {
      fluency.emit(fullTag, event.asJava)
    }
  }
}
