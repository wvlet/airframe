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
    useExtendedEventTime: Boolean = false,
    maxBufferSize: Long = 512 * 1024 * 1024,
    bufferChunkInitialSize: Int = 1024 * 1024,
    bufferChunkRetentionSize: Int = 1024 * 1024 * 4,
    bufferChunkRetentionTimeMillis: Int = 1000,
    flushIntervalMillis: Int = 600,
    fileBackupDir: String = null,
    waitUntilBufferFlushed: Int = 60,
    waitUntilFlusherTerminated: Int = 60,
    jvmHeapBufferMode: Boolean = false,
    errorHandler: ErrorHandler = null,
    senderMaxRetryCount: Int = 7,
    ackResponseMode: Boolean = false,
    sslEnabled: Boolean = false
)

object FluencyClient extends LogSupport {
  def newFluency(fluentdConfig: FluentdConfig, fluencyConfig: FluencyConfig): Fluency = {
    val builder = new FluencyBuilderForFluentd()

    setConfiguration(builder, "maxBufferSize", _.setMaxBufferSize(fluencyConfig.maxBufferSize))
    setConfiguration(builder,
                     "bufferChunkInitialSize",
                     _.setBufferChunkInitialSize(fluencyConfig.bufferChunkInitialSize))
    setConfiguration(builder,
                     "bufferChunkRetentionSize",
                     _.setBufferChunkRetentionSize(fluencyConfig.bufferChunkRetentionSize))
    setConfiguration(builder,
                     "bufferChunkRetentionTimeMillis",
                     _.setBufferChunkRetentionTimeMillis(fluencyConfig.bufferChunkRetentionTimeMillis))
    setConfiguration(builder, "flushIntervalMillis", _.setFlushIntervalMillis(fluencyConfig.flushIntervalMillis))
    setConfiguration(builder, "fileBackupDir", _.setFileBackupDir(fluencyConfig.fileBackupDir))
    setConfiguration(builder,
                     "waitUntilBufferFlushed",
                     _.setWaitUntilBufferFlushed(fluencyConfig.waitUntilBufferFlushed))
    setConfiguration(builder,
                     "waitUntilFlusherTerminated",
                     _.setWaitUntilFlusherTerminated(fluencyConfig.waitUntilFlusherTerminated))
    setConfiguration(builder, "jvmHeapBufferMode", _.setJvmHeapBufferMode(fluencyConfig.jvmHeapBufferMode))
    setConfiguration(builder, "errorHandler", _.setErrorHandler(fluencyConfig.errorHandler))
    setConfiguration(builder, "senderMaxRetryCount", _.setSenderMaxRetryCount(fluencyConfig.senderMaxRetryCount))
    setConfiguration(builder, "ackResponseMode", _.setAckResponseMode(fluencyConfig.ackResponseMode))
    setConfiguration(builder, "sslEnabled", _.setSslEnabled(fluencyConfig.sslEnabled))

    builder.build(fluentdConfig.host, fluentdConfig.port)
  }

  // Wrap a function to warn missing configuration items
  private def setConfiguration(builder: FluencyBuilderForFluentd,
                               name: String,
                               f: FluencyBuilderForFluentd => Unit): Unit = {
    try {
      f(builder)
    } catch {
      case _: NoSuchMethodError =>
        warn(s"$name is no longer supported in Fluency.")
    }
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
