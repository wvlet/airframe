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
import org.komamitsu.fluency.fluentd.FluencyBuilderForFluentd
import org.komamitsu.fluency.ingester.sender.ErrorHandler
import org.komamitsu.fluency.treasuredata.FluencyBuilderForTreasureData

/**
  */
object Fluentd {

  /**
    * A design for using Fluency-backed FluentdClient
    *
    * @return
    */
  def newFluentdLogger(
      host: String = "127.0.0.1",
      port: Int = 24224,
      tagPrefix: String = "",
      // Use the extended EventTime timestamps
      // https://github.com/fluent/fluentd/wiki/Forward-Protocol-Specification-v1#eventtime-ext-format
      useExtendedEventTime: Boolean = false,
      maxBufferSize: Long = 512 * 1024 * 1024,
      flushIntervalMillis: Int = 600,
      bufferChunkRetentionSize: Int = 4 * 1024 * 1024,
      bufferChunkRetentionTimeMillis: Int = 1000,
      jvmHeapBufferMode: Boolean = true,
      ackResponseMode: Boolean = true,
      sslEnabled: Boolean = false,
      fileBackupDir: String = null,
      errorHandler: ErrorHandler = null
  ): FluentdLogger = {
    // We need to extract this code probably because of a bug of Scala compiler.
    val builder = new FluencyBuilderForFluentd()
    builder.setMaxBufferSize(maxBufferSize)
    builder.setFlushAttemptIntervalMillis(flushIntervalMillis)
    builder.setBufferChunkRetentionSize(bufferChunkRetentionSize)
    builder.setBufferChunkRetentionTimeMillis(bufferChunkRetentionTimeMillis)
    builder.setJvmHeapBufferMode(jvmHeapBufferMode)
    builder.setAckResponseMode(ackResponseMode)
    builder.setSslEnabled(sslEnabled)
    builder.setFileBackupDir(fileBackupDir)
    builder.setErrorHandler(errorHandler) // Passing null is allowed in Fluency
    new FluentdLogger(if (tagPrefix.isEmpty) None else Some(tagPrefix), useExtendedEventTime, builder.build(host, port))
  }

  def newTDLogger(
      apikey: String,
      host: String = "api.treasuredata.com",
      port: Int = 443,
      tagPrefix: String = "",
      maxBufferSize: Long = 512 * 1024 * 1024,
      flushIntervalMillis: Int = 600,
      bufferChunkRetentionSize: Int = 8 * 1024 * 1024,
      bufferChunkRetentionTimeMillis: Int = 1000,
      jvmHeapBufferMode: Boolean = true,
      // Use the extended EventTime timestamps
      // https://github.com/fluent/fluentd/wiki/Forward-Protocol-Specification-v1#eventtime-ext-format
      useExtededEventTime: Boolean = false,
      fileBackupDir: String = null,
      errorHandler: ErrorHandler = null
  ): FluentdLogger = {
    val builder = new FluencyBuilderForTreasureData()
    builder.setMaxBufferSize(maxBufferSize)
    builder.setFlushAttemptIntervalMillis(flushIntervalMillis)
    builder.setBufferChunkRetentionSize(bufferChunkRetentionSize)
    builder.setBufferChunkRetentionTimeMillis(bufferChunkRetentionTimeMillis)
    builder.setJvmHeapBufferMode(jvmHeapBufferMode)
    builder.setFileBackupDir(fileBackupDir)
    builder.setErrorHandler(errorHandler) // Passing null is allowed in Fluency
    builder.build(apikey, host)
    new FluentdLogger(
      if (tagPrefix.isEmpty) None else Some(tagPrefix),
      useExtededEventTime,
      builder.build(apikey, host)
    )
  }
}
