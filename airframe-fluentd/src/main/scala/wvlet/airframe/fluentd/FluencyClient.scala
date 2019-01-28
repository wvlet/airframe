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

import org.komamitsu.fluency.fluentd.FluencyBuilderForFluentd
import org.komamitsu.fluency.ingester.sender.ErrorHandler
import org.komamitsu.fluency.{EventTime, Fluency}
import wvlet.log.LogSupport
import wvlet.airframe._

case class FluencyClientConfig(
    host: String = "127.0.0.1",
    port: Int = 24224,
    // Use the extended EventTime timestamps
    // https://github.com/fluent/fluentd/wiki/Forward-Protocol-Specification-v1#eventtime-ext-format
    useExtendedEventTime: Boolean = false,
    maxBufferSize: Long = 512 * 1024 * 1024,
    flushIntervalMillis: Int = 600,
    jvmHeapBufferMode: Boolean = true,
    ackResponseMode: Boolean = true,
    sslEnabled: Boolean = true,
    fileBackupDir: String = null,
    errorHandler: ErrorHandler = null
)

trait FluencyClient extends FluentdClient with LogSupport {
  private val fluencyClientConfig = bind[FluencyClientConfig]
  private val fluency: Fluency = bind { (fluencyClientConfig: FluencyClientConfig) =>
    val builder = new FluencyBuilderForFluentd()
    builder.setMaxBufferSize(fluencyClientConfig.maxBufferSize)
    builder.setFlushIntervalMillis(fluencyClientConfig.flushIntervalMillis)
    builder.setJvmHeapBufferMode(fluencyClientConfig.jvmHeapBufferMode)
    builder.setAckResponseMode(fluencyClientConfig.ackResponseMode)
    builder.setSslEnabled(fluencyClientConfig.sslEnabled)
    builder.setFileBackupDir(fluencyClientConfig.fileBackupDir)
    builder.setErrorHandler(fluencyClientConfig.errorHandler)
    builder.build(fluencyClientConfig.host, fluencyClientConfig.port)
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

  private def getEventTime: EventTime = {
    val now       = Instant.now()
    val eventTime = EventTime.fromEpoch(now.getEpochSecond.toInt, now.getNano.toInt)
    eventTime
  }

  override protected def emitRaw(fullTag: String, event: Map[String, Any]): Unit = {
    if (fluencyClientConfig.useExtendedEventTime) {
      fluency.emit(fullTag, getEventTime, toJavaMap(event))
    } else {
      fluency.emit(fullTag, toJavaMap(event))
    }
  }
  override protected def emitRawMsgPack(tag: String, event: Array[Byte]): Unit = {
    if (fluencyClientConfig.useExtendedEventTime) {
      fluency.emit(tag, getEventTime, event, 0, event.length)
    } else {
      fluency.emit(tag, event, 0, event.length)
    }
  }
}
