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

import java.util.concurrent.TimeUnit

import org.komamitsu.fluency.{EventTime, Fluency}
import wvlet.airframe._

import scala.collection.JavaConverters._

case class FluentdConfig(
  host: String = "127.0.0.1",
  port: Int = 24224,
  // tag prefix pre-pended to each message
  tagPrefix: String = "",
  useEventTime: Boolean = false,
  fluencyConfig: Fluency.Config = new Fluency.Config()
)

trait FluentdClient {
  private lazy val fluentdConfig = bind[FluentdConfig]
  private      val fluency       = bind[Fluency] {Fluency.defaultFluency(fluentdConfig.host, fluentdConfig.port, fluentdConfig.fluencyConfig)}

  def emit(tag: String, event: Map[String, AnyRef]) {
    val fullTag = if (fluentdConfig.tagPrefix.isEmpty) {
      tag
    }
    else {
      s"${fluentdConfig.tagPrefix}.${tag}"
    }
    if (fluentdConfig.useEventTime) {
      val eventTime = EventTime.fromEpochMilli(TimeUnit.NANOSECONDS.toMillis(System.nanoTime()))
      fluency.emit(fullTag, eventTime, event.asJava)
    }
    else {
      fluency.emit(fullTag, event.asJava)
    }
  }
}
