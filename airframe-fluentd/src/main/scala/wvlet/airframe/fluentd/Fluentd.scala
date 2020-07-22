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
import org.komamitsu.fluency.FluencyBuilder
import org.komamitsu.fluency.fluentd.FluencyBuilderForFluentd
import org.komamitsu.fluency.ingester.sender.ErrorHandler
import org.komamitsu.fluency.treasuredata.FluencyBuilderForTreasureData
import wvlet.airframe.codec.MessageCodecFactory
import wvlet.log.LogLevel

case class FluentdClientConfig(
    tagPrefix: Option[String] = None,
    // Use the extended EventTime timestamps
    // https://github.com/fluent/fluentd/wiki/Forward-Protocol-Specification-v1#eventtime-ext-format
    useExtendedEventTime: Boolean = false,
    maxBufferSize: Long = 512 * 1024 * 1024,
    flushIntervalMillis: Int = 600,
    bufferChunkRetentionSize: Int = 8 * 1024 * 1024,
    bufferChunkRetentionTimeMillis: Int = 1000,
    jvmHeapBufferMode: Boolean = true,
    // fluentd-specific config. No effect for TDLogger
    ackResponseMode: Boolean = true,
    // fluentd-specific config. No effect for TDLogger (always use SSL)
    sslEnabled: Boolean = false,
    fileBackupDir: String = null,
    errorHandler: ErrorHandler = null,
    codecFactory: MessageCodecFactory = MessageCodecFactory.defaultFactory.withMapOutput
) {

  private def initBuilder(builder: FluencyBuilder): Unit = {
    builder.setMaxBufferSize(maxBufferSize)
    builder.setFlushAttemptIntervalMillis(flushIntervalMillis)
    builder.setBufferChunkRetentionSize(bufferChunkRetentionSize)
    builder.setBufferChunkRetentionTimeMillis(bufferChunkRetentionTimeMillis)
    builder.setJvmHeapBufferMode(jvmHeapBufferMode)
    builder.setFileBackupDir(fileBackupDir)
    builder.setErrorHandler(errorHandler) // Passing null is allowed in Fluency
  }

  def newFluentdLogger(host: String = "127.0.0.1", port: Int = 24224): FluentdLogger = {
    // We need to extract this code probably because of a bug of Scala compiler.
    val builder = new FluencyBuilderForFluentd()
    initBuilder(builder)
    // Add fluentd-specific config
    builder.setAckResponseMode(ackResponseMode)
    builder.setSslEnabled(sslEnabled)
    new FluentdLogger(tagPrefix, useExtendedEventTime, builder.build(host, port))
  }

  def newFluentdLoggerFactory(host: String = "127.0.0.1", port: Int = 24224): MetricLoggerFactory = {
    new MetricLoggerFactory(fluentdClient = newFluentdLogger(host = host, port = port), codecFactory = codecFactory)
  }

  def newTDLogger(apikey: String, host: String = "api.treasuredata.com"): FluentdLogger = {
    val builder = new FluencyBuilderForTreasureData()
    initBuilder(builder)
    new FluentdLogger(
      tagPrefix,
      useExtendedEventTime = useExtendedEventTime,
      builder.build(apikey, host)
    )
  }

  def newTDLoggerFactory(
      apikey: String,
      host: String = "api.treasuredata.com"
  ): MetricLoggerFactory = {
    new MetricLoggerFactory(fluentdClient = newTDLogger(apikey = apikey, host = host), codecFactory = codecFactory)
  }

  /**
    * Create a console logger for debugging
    * @param logLevel
    * @return
    */
  def newConsoleLogger(logLevel: LogLevel = LogLevel.INFO): MetricLogger = {
    new ConsoleLogger(tagPrefix, logLevel = logLevel)
  }

  def newConsoleLoggerFactory(logLevel: LogLevel = LogLevel.INFO): MetricLoggerFactory = {
    new MetricLoggerFactory(fluentdClient = newConsoleLogger(logLevel))
  }

  def withTagPrefix(tagPrefix: String): FluentdClientConfig    = this.copy(tagPrefix = Some(tagPrefix))
  def withExtendedEventTime: FluentdClientConfig               = this.copy(useExtendedEventTime = true)
  def noExtendedEventTime: FluentdClientConfig                 = this.copy(useExtendedEventTime = false)
  def withMaxBufferSize(bufferSize: Long): FluentdClientConfig = this.copy(maxBufferSize = bufferSize)
  def withFlushIntervalMillis(flushIntervalMillis: Int): FluentdClientConfig =
    this.copy(flushIntervalMillis = flushIntervalMillis)
  def withBufferChunkRetentionSize(bufferChunkRetentionSize: Int): FluentdClientConfig =
    this.copy(bufferChunkRetentionSize = bufferChunkRetentionSize)
  def withBufferChunkRetentionTimeMillis(bufferChunkRetentionTimeMillis: Int): FluentdClientConfig =
    this.copy(bufferChunkRetentionTimeMillis = bufferChunkRetentionTimeMillis)
  def withJVMHeapBuffer: FluentdClientConfig                            = this.copy(jvmHeapBufferMode = true)
  def noJVMHeapBuffer: FluentdClientConfig                              = this.copy(jvmHeapBufferMode = false)
  def withAckResponse: FluentdClientConfig                              = this.copy(ackResponseMode = true)
  def noAckResponse: FluentdClientConfig                                = this.copy(ackResponseMode = false)
  def withSSL: FluentdClientConfig                                      = this.copy(sslEnabled = true)
  def noSSL: FluentdClientConfig                                        = this.copy(sslEnabled = false)
  def withFileBackupDir(dir: String): FluentdClientConfig               = this.copy(fileBackupDir = dir)
  def withErrorHandler(errorHandler: ErrorHandler): FluentdClientConfig = this.copy(errorHandler = errorHandler)
  def withMessageCodecFactory(codecFactory: MessageCodecFactory): FluentdClientConfig =
    this.copy(codecFactory = codecFactory)
}

/**
  */
object Fluentd {

  def client: FluentdClientConfig = FluentdClientConfig()

  /**
    * Create a new Fluency-backed FluentdClient.
    * @deprecated Use Fluentd.client.newFluentdLogger instead
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
      bufferChunkRetentionSize: Int = 8 * 1024 * 1024,
      bufferChunkRetentionTimeMillis: Int = 1000,
      jvmHeapBufferMode: Boolean = true,
      ackResponseMode: Boolean = true,
      sslEnabled: Boolean = false,
      fileBackupDir: String = null,
      errorHandler: ErrorHandler = null
  ): FluentdLogger = {
    FluentdClientConfig(
      tagPrefix = if (tagPrefix.isEmpty) None else Some(tagPrefix),
      useExtendedEventTime = useExtendedEventTime,
      maxBufferSize = maxBufferSize,
      flushIntervalMillis = flushIntervalMillis,
      bufferChunkRetentionSize = bufferChunkRetentionSize,
      bufferChunkRetentionTimeMillis = bufferChunkRetentionTimeMillis,
      jvmHeapBufferMode = jvmHeapBufferMode,
      ackResponseMode = ackResponseMode,
      sslEnabled = sslEnabled,
      fileBackupDir = fileBackupDir,
      errorHandler = errorHandler
    ).newFluentdLogger(host, port)
  }

  /**
    * @deprecated Use Fluentd.client.newTDLogger instead
    */
  def newTDLogger(
      apikey: String,
      host: String = "api.treasuredata.com",
      tagPrefix: String = "",
      maxBufferSize: Long = 512 * 1024 * 1024,
      flushIntervalMillis: Int = 600,
      bufferChunkRetentionSize: Int = 8 * 1024 * 1024,
      bufferChunkRetentionTimeMillis: Int = 1000,
      jvmHeapBufferMode: Boolean = true,
      // Use the extended EventTime timestamps
      // https://github.com/fluent/fluentd/wiki/Forward-Protocol-Specification-v1#eventtime-ext-format
      useExtendedEventTime: Boolean = false,
      fileBackupDir: String = null,
      errorHandler: ErrorHandler = null
  ): FluentdLogger = {
    FluentdClientConfig(
      tagPrefix = if (tagPrefix.isEmpty) None else Some(tagPrefix),
      useExtendedEventTime = useExtendedEventTime,
      maxBufferSize = maxBufferSize,
      flushIntervalMillis = flushIntervalMillis,
      bufferChunkRetentionSize = bufferChunkRetentionSize,
      bufferChunkRetentionTimeMillis = bufferChunkRetentionTimeMillis,
      jvmHeapBufferMode = jvmHeapBufferMode,
      fileBackupDir = fileBackupDir,
      errorHandler = errorHandler
    ).newTDLogger(apikey, host)
  }
}
