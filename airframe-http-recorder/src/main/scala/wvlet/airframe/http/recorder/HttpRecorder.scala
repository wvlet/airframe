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
package wvlet.airframe.http.recorder

import com.twitter.finagle.{Http, Service}
import com.twitter.finagle.builder.ClientBuilder
import com.twitter.finagle.http.{Request, Response, Status}
import com.twitter.finagle.service.RetryPolicy
import com.twitter.util.Future
import wvlet.airframe.http.finagle.{FinagleServer, FinagleServerConfig}
import wvlet.log.io.IOUtil

/**
  * Creates a proxy server for recording and replaying HTTP responses.
  * This is useful for simulate the behavior of Web services, that
  * are too heavy to use in an restricted environment (e.g., CI servers)
  */
object HttpRecorder {

  /**
    * Creates an HTTP server that will record HTTP responses.
    */
  def createRecordingServer(destUri: String,
                            sessionName: String = "default",
                            recorderConfig: HttpRecordStoreConfig = HttpRecordStoreConfig()): FinagleServer = {
    val port          = IOUtil.unusedPort
    val finagleConfig = FinagleServerConfig(port)
    val recorder      = new HttpRecordStore(sessionName, recorderConfig)

    val destClient =
      ClientBuilder()
        .stack(Http.client)
        .name(s"vcr-proxy")
        .dest(destUri)
        .keepAlive(true)
        .retryPolicy(RetryPolicy.tries(3, RetryPolicy.TimeoutAndWriteExceptionsOnly))
        .build()

    new FinagleServer(finagleConfig, new RecordingService(recorder, destClient))
  }

  /**
    * Creates an HTTP server that returns recorded HTTP responses.
    * If no matching record is found, use the given fallback handler.
    */
  def createReplayServer(sessionName: String = "default",
                         recorderConfig: HttpRecordStoreConfig = HttpRecordStoreConfig(),
                         fallBackHandler: Service[Request, Response] = defaultFallBackHandler,
  ): FinagleServer = {
    val port          = IOUtil.unusedPort
    val finagleConfig = FinagleServerConfig(port)
    val recorder      = new HttpRecordStore(sessionName, recorderConfig)
    new FinagleServer(finagleConfig, new RecordReplayService(recorder, fallBackHandler))
  }

  def defaultFallBackHandler = {
    Service.mk { request: Request =>
      val r = Response(Status.NotFound)
      r.contentString = s"${request.uri} is not found"
      Future.value(r)
    }
  }

}
