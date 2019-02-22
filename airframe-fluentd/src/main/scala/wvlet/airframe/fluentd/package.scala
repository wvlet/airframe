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
import org.komamitsu.fluency.Fluency
import org.komamitsu.fluency.fluentd.FluencyBuilderForFluentd
import org.komamitsu.fluency.ingester.sender.ErrorHandler
import org.komamitsu.fluency.treasuredata.FluencyBuilderForTreasureData

/**
  *
  */
package object fluentd {

  /**
    * A design for using Fluency-backed FluentdClient
    *
    * @return
    */
  def withFluency(host: String = "127.0.0.1",
                  port: Int = 24224,
                  maxBufferSize: Long = 512 * 1024 * 1024,
                  flushIntervalMillis: Int = 600,
                  jvmHeapBufferMode: Boolean = true,
                  ackResponseMode: Boolean = true,
                  sslEnabled: Boolean = true,
                  fileBackupDir: String = null,
                  errorHandler: ErrorHandler = null) = {
    newDesign
      .bind[Fluency].toInstance {
        val builder = new FluencyBuilderForFluentd()
        builder.setMaxBufferSize(maxBufferSize)
        builder.setFlushIntervalMillis(flushIntervalMillis)
        builder.setJvmHeapBufferMode(jvmHeapBufferMode)
        builder.setAckResponseMode(ackResponseMode)
        builder.setSslEnabled(sslEnabled)
        builder.setFileBackupDir(fileBackupDir)
        builder.setErrorHandler(errorHandler) // Passing null is allowed in Fluency
        builder.build(host, port)
      }
      .bind[FluentdClient].to[FluencyClient]
  }

  def withFluencyForTD(apikey: String,
                       host: String = "api.treasuredata.com",
                       port: Int = 443,
                       maxBufferSize: Long = 512 * 1024 * 1024,
                       flushIntervalMillis: Int = 600,
                       jvmHeapBufferMode: Boolean = true,
                       fileBackupDir: String = null,
                       errorHandler: ErrorHandler = null): Design = {
    newDesign
      .bind[Fluency].toInstance {
        val builder = new FluencyBuilderForTreasureData()
        builder.setMaxBufferSize(maxBufferSize)
        builder.setFlushIntervalMillis(flushIntervalMillis)
        builder.setJvmHeapBufferMode(jvmHeapBufferMode)
        builder.setFileBackupDir(fileBackupDir)
        builder.setErrorHandler(errorHandler) // Passing null is allowed in Fluency
        builder.build(apikey, host)
      }
      .bind[FluentdClient].to[FluencyClient]

  }

  def withConsoleLogging =
    newDesign
      .bind[FluentdClient].to[ConsoleFluentdClient]
}
