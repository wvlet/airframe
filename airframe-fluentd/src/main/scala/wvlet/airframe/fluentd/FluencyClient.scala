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

case class FluencyConfig(
    // Use the extended EventTime timestamps
    // https://github.com/fluent/fluentd/wiki/Forward-Protocol-Specification-v1#eventtime-ext-format
    useExtendedEventTime: java.lang.Boolean = false,
    maxBufferSize: java.lang.Long = null,
    bufferChunkInitialSize: java.lang.Integer = null,
    bufferChunkRetentionSize: java.lang.Integer = null,
    bufferChunkRetentionTimeMillis: java.lang.Integer = null,
    flushIntervalMillis: java.lang.Integer = null,
    fileBackupDir: String = null,
    waitUntilBufferFlushed: java.lang.Integer = null,
    waitUntilFlusherTerminated: java.lang.Integer = null,
    jvmHeapBufferMode: java.lang.Boolean = null,
    errorHandler: ErrorHandler = null,
    senderMaxRetryCount: java.lang.Integer = null,
    ackResponseMode: Boolean = false,
    sslEnabled: Boolean = false
)

object FluencyClient {
  def newFluency(fluentdConfig: FluentdConfig, fluencyConfig: FluencyConfig): Fluency = {
    val builder = new FluencyBuilderForFluentd()

    builder.setMaxBufferSize(fluencyConfig.maxBufferSize)
    builder.setBufferChunkInitialSize(fluencyConfig.bufferChunkInitialSize)
    builder.setBufferChunkRetentionSize(fluencyConfig.bufferChunkRetentionSize)
    builder.setBufferChunkRetentionTimeMillis(fluencyConfig.bufferChunkRetentionTimeMillis)
    builder.setFlushIntervalMillis(fluencyConfig.flushIntervalMillis)
    builder.setFileBackupDir(fluencyConfig.fileBackupDir)
    builder.setWaitUntilBufferFlushed(fluencyConfig.waitUntilBufferFlushed)
    builder.setWaitUntilFlusherTerminated(fluencyConfig.waitUntilFlusherTerminated)
    builder.setJvmHeapBufferMode(fluencyConfig.jvmHeapBufferMode)
    builder.setErrorHandler(fluencyConfig.errorHandler)
    builder.setSenderMaxRetryCount(fluencyConfig.senderMaxRetryCount)
    builder.setAckResponseMode(fluencyConfig.ackResponseMode)
    builder.setSslEnabled(fluencyConfig.sslEnabled)

    builder.build(fluentdConfig.host, fluentdConfig.port)
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

  private def getEventTime: EventTime = {
    val now       = Instant.now()
    val eventTime = EventTime.fromEpoch(now.getEpochSecond.toInt, now.getNano.toInt);
    eventTime
  }

  override protected def emitRaw(fullTag: String, event: Map[String, Any]): Unit = {
    if (fluencyConfig.useExtendedEventTime) {
      fluency.emit(fullTag, getEventTime, toJavaMap(event))
    } else {
      fluency.emit(fullTag, toJavaMap(event))
    }
  }
  override protected def emitRawMsgPack(tag: String, event: Array[Byte]): Unit = {
    if (fluencyConfig.useExtendedEventTime) {
      fluency.emit(tag, getEventTime, event, 0, event.length)
    } else {
      fluency.emit(tag, event, 0, event.length)
    }
  }
}
