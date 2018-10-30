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

import scala.collection.JavaConverters._

case class FluentdConfig(
    host: String = "127.0.0.1",
    port: Int = 24224,
    // tag prefix pre-pended to each message
    tagPrefix: Option[String] = None,
    // Use the extended EventTime timestamps https://github.com/fluent/fluentd/wiki/Forward-Protocol-Specification-v1#eventtime-ext-format
    useExtendedEventTime: Boolean = false,
    fluencyConfig: Fluency.Config = new Fluency.Config()
)

object FluentdClient {
  def newFluency(fluentdConfig: FluentdConfig): Fluency = {
    Fluency.defaultFluency(fluentdConfig.host, fluentdConfig.port, fluentdConfig.fluencyConfig)
  }
}

trait FluentdClient {
  private val fluentdConfig = bind[FluentdConfig]
  private val fluency: Fluency = bind { config: FluentdConfig =>
    FluentdClient.newFluency(config)
  }.onShutdown { x =>
    close
  }

  def close: Unit = {
    fluency.flush()
    fluency.close()
  }

  def emit(tag: String, event: Map[String, AnyRef]) {
    val fullTag = fluentdConfig.tagPrefix match {
      case None         => tag
      case Some(prefix) => s"${prefix}.tag"
    }

    if (fluentdConfig.useExtendedEventTime) {
      val now       = Instant.now()
      val eventTime = EventTime.fromEpoch(now.getEpochSecond.toInt, now.getNano.toInt);
      fluency.emit(fullTag, eventTime, event.asJava)
    } else {
      fluency.emit(fullTag, event.asJava)
    }
  }
}

trait FluentdService {
  val fluentd = bind[FluentdClient]
}
