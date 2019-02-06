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

import java.net.URI

import com.twitter.finagle.{Http, Service}
import com.twitter.finagle.builder.ClientBuilder
import com.twitter.finagle.http.{Request, Response, Status}
import com.twitter.finagle.service.RetryPolicy
import com.twitter.util.Future
import javax.net.ssl.SSLContext
import wvlet.airframe.http.finagle.{FinagleServer, FinagleServerConfig}
import wvlet.log.io.IOUtil

case class HttpRecorderConfig(destUri: String,
                              sessionName: String = "default",
                              folder: String = "fixtures",
                              recordTableName: String = "record",
                              private val port: Int = -1,
                              requestHeaderFilter: String => Boolean = HttpRecorder.defaultHeaderFilter,
                              fallBackHandler: Service[Request, Response] = HttpRecorder.defaultFallBackHandler) {

  lazy val serverPort = if (port == -1) IOUtil.unusedPort else port

  lazy val destHostAndPort: String = {
    if (destUri.startsWith("http:") || destUri.startsWith("https:")) {
      val uri = URI.create(destUri)
      val port = uri.getScheme match {
        case "https"                => 443
        case _ if uri.getPort == -1 => 80
        case _                      => uri.getPort
      }
      s"${uri.getHost}:${port}"
    } else {
      destUri
    }
  }
}

/**
  * Creates a proxy server for recording and replaying HTTP responses.
  * This is useful for simulate the behavior of Web services, that
  * are too heavy to use in an restricted environment (e.g., CI servers)
  */
object HttpRecorder {

  def defaultHeaderFilter(headerName: String): Boolean = {
    // Ignore Finagle's tracing IDs
    !headerName.startsWith("X-B3-") && !headerName.startsWith("Finagle-")
  }

  /**
    * Creates an HTTP server that will record HTTP responses.
    */
  def createRecordingServer(recorderConfig: HttpRecorderConfig): FinagleServer = {
    val finagleConfig = FinagleServerConfig(recorderConfig.serverPort)
    val recorder      = new HttpRecordStore(recorderConfig)

    val destClient =
      ClientBuilder()
        .stack(Http.client)
        .name(s"airframe-http-recorder-proxy")
        .dest(recorderConfig.destHostAndPort)
        .tls(SSLContext.getDefault)
        .keepAlive(true)
        .retryPolicy(RetryPolicy.tries(3, RetryPolicy.TimeoutAndWriteExceptionsOnly))
        .build()

    new FinagleServer(finagleConfig, new RecordingService(recorder, destClient))
  }

  /**
    * Creates an HTTP server that returns recorded HTTP responses.
    * If no matching record is found, use the given fallback handler.
    */
  def createReplayServer(recorderConfig: HttpRecorderConfig): FinagleServer = {
    val finagleConfig = FinagleServerConfig(recorderConfig.serverPort)
    val recorder      = new HttpRecordStore(recorderConfig)
    new FinagleServer(finagleConfig, new RecordReplayService(recorder))
  }

  def defaultFallBackHandler = {
    Service.mk { request: Request =>
      val r = Response(Status.NotFound)
      r.contentString = s"${request.uri} is not found"
      Future.value(r)
    }
  }

}
