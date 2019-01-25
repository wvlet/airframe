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
    maxBufferSize: Option[Long] = None,
    bufferChunkInitialSize: Option[Int] = None,
    bufferChunkRetentionSize: Option[Int] = None,
    bufferChunkRetentionTimeMillis: Option[Int] = None,
    flushIntervalMillis: Option[Int] = None,
    fileBackupDir: Option[String] = None,
    waitUntilBufferFlushed: Option[Int] = None,
    waitUntilFlusherTerminated: Option[Int] = None,
    jvmHeapBufferMode: Option[Boolean] = None,
    errorHandler: Option[ErrorHandler] = None,
    senderMaxRetryCount: Option[Int] = None,
    ackResponseMode: Option[Boolean] = None,
    sslEnabled: Option[Boolean] = None
)

object FluencyClient extends LogSupport {
  def newFluency(fluentdConfig: FluentdConfig, fluencyConfig: FluencyConfig): Fluency = {
    val builder = new FluencyBuilderForFluentd()

    fluencyConfig.maxBufferSize.foreach { x =>
      setConfiguration(builder, "maxBufferSize", _.setMaxBufferSize(x))
    }
    fluencyConfig.bufferChunkInitialSize.foreach { x =>
      setConfiguration(builder, "bufferChunkInitialSize", _.setBufferChunkInitialSize(x))
    }
    fluencyConfig.bufferChunkRetentionSize.foreach { x =>
      setConfiguration(builder, "bufferChunkRetentionSize", _.setBufferChunkRetentionSize(x))
    }
    fluencyConfig.bufferChunkRetentionTimeMillis.foreach { x =>
      setConfiguration(builder, "bufferChunkRetentionTimeMillis", _.setBufferChunkRetentionTimeMillis(x))
    }
    fluencyConfig.flushIntervalMillis.foreach { x =>
      setConfiguration(builder, "flushIntervalMillis", _.setFlushIntervalMillis(x))
    }
    fluencyConfig.fileBackupDir.foreach { x =>
      setConfiguration(builder, "fileBackupDir", _.setFileBackupDir(x))
    }
    fluencyConfig.waitUntilBufferFlushed.foreach { x =>
      setConfiguration(builder, "waitUntilBufferFlushed", _.setWaitUntilBufferFlushed(x))
    }
    fluencyConfig.waitUntilFlusherTerminated.foreach { x =>
      setConfiguration(builder, "waitUntilFlusherTerminated", _.setWaitUntilFlusherTerminated(x))
    }
    fluencyConfig.jvmHeapBufferMode.foreach { x =>
      setConfiguration(builder, "jvmHeapBufferMode", _.setJvmHeapBufferMode(x))
    }
    fluencyConfig.errorHandler.foreach { x =>
      setConfiguration(builder, "errorHandler", _.setErrorHandler(x))
    }
    fluencyConfig.senderMaxRetryCount.foreach { x =>
      setConfiguration(builder, "senderMaxRetryCount", _.setSenderMaxRetryCount(x))
    }
    fluencyConfig.ackResponseMode.foreach { x =>
      setConfiguration(builder, "ackResponseMode", _.setAckResponseMode(x))
    }
    fluencyConfig.sslEnabled.foreach { x =>
      setConfiguration(builder, "sslEnabled", _.setSslEnabled(x))
    }

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
