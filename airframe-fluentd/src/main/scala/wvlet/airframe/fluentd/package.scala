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

/**
  *
  */
package object fluentd {

  /**
    * A design for using Fluency-backed FluentdClient
    * @return
    */
  def withFluency(host: String = "127.0.0.1",
                  port: Int = 24224,
                  useExtendedEventTime: Boolean = false,
                  maxBufferSize: Long = 512 * 1024 * 1024,
                  flushIntervalMillis: Int = 600,
                  jvmHeapBufferMode: Boolean = true,
                  ackResponseMode: Boolean = true,
                  sslEnabled: Boolean = true,
                  fileBackupDir: String = null,
                  errorHandler: ErrorHandler = null) =
    newDesign
      .bind[FluencyClientConfig].toInstance(
        FluencyClientConfig(
          host = host,
          port = port,
          useExtendedEventTime = useExtendedEventTime,
          maxBufferSize = maxBufferSize,
          flushIntervalMillis = flushIntervalMillis,
          jvmHeapBufferMode = jvmHeapBufferMode,
          ackResponseMode = ackResponseMode,
          sslEnabled = sslEnabled,
          fileBackupDir = fileBackupDir,
          errorHandler = errorHandler
        ))
      .bind[FluentdClient].to[FluencyClient]

  def withConsoleLogging =
    newDesign
      .bind[FluentdClient].to[ConsoleFluentdClient]
}
