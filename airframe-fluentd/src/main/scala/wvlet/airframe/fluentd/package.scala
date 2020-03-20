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
package wvlet.airframe
import org.komamitsu.fluency.ingester.sender.ErrorHandler
import wvlet.log.LogLevel

/**
  *
  */
package object fluentd {
  type TDLogger = MetricLogger

  /**
    * A MetricLogger design for sending metrics to Fluentd
    *
    */
  def withFluentdLogger(
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
  ): Design = {
    newDesign
      .bind[MetricLogger].toInstance(
        Fluentd.newFluentdLogger(
          host,
          port,
          tagPrefix,
          useExtendedEventTime,
          maxBufferSize,
          flushIntervalMillis,
          bufferChunkRetentionSize,
          bufferChunkRetentionTimeMillis,
          jvmHeapBufferMode,
          ackResponseMode,
          sslEnabled,
          fileBackupDir,
          errorHandler
        )
      )
  }

  /**
    * A MetricLogger design for sending metrics to TD
    */
  def withTDLogger(
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
  ): Design = {
    newDesign
      .bind[MetricLogger].toInstance(
        Fluentd.newTDLogger(
          apikey,
          host,
          port,
          tagPrefix,
          maxBufferSize,
          flushIntervalMillis,
          bufferChunkRetentionSize,
          bufferChunkRetentionTimeMillis,
          jvmHeapBufferMode,
          useExtededEventTime,
          fileBackupDir,
          errorHandler
        )
      )
  }

  def withConsoleLogging: Design = {
    newDesign
      .bind[MetricLogger].to[ConsoleLogger]
  }

  def withDebugConsoleLogging: Design = {
    newDesign
      .bind[MetricLogger].toInstance(new ConsoleLogger(logLevel = LogLevel.DEBUG))
  }
}
