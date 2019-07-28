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

import com.twitter.finagle.builder.ClientBuilder
import com.twitter.finagle.http.{Request, Response, Status}
import com.twitter.finagle.service.RetryPolicy
import com.twitter.finagle.{Http, Service}
import com.twitter.util.Future
import javax.net.ssl.SSLContext
import wvlet.airframe.http.ServerAddress
import wvlet.airframe.http.finagle.FinagleServer
import wvlet.log.LogSupport
import wvlet.log.io.IOUtil

case class HttpRecorderConfig(destUri: String,
                              sessionName: String = "default",
                              expirationTime: String = "1w",
                              // the folder to store response records
                              storageFolder: String = "fixtures",
                              // Drop the session records for recording mode
                              dropSessionIfExists: Boolean = true,
                              recordTableName: String = "record",
                              // Specify the port to use. The default is finding an available port
                              private val port: Int = -1,
                              // A filter for customizing HTTP request headers to use for generating database keys.
                              // For example, we should remove headers that depends on the current time, etc.
                              headerExcludes: String => Boolean = HttpRecorder.defaultHeaderExclude,
                              fallBackHandler: Service[Request, Response] = HttpRecorder.defaultFallBackHandler) {

  def sqliteFilePath   = s"${storageFolder}/${sessionName}.sqlite"
  lazy val serverPort  = if (port == -1) IOUtil.unusedPort else port
  lazy val destAddress = ServerAddress(destUri)
}

/**
  * Creates a proxy server for recording and replaying HTTP responses.
  * This is useful for simulate the behavior of Web services, that
  * are usually too heavy to use in an restricted environment (e.g., CI servers)
  */
object HttpRecorder extends LogSupport {

  def defaultHeaderExclude: String => Boolean = { headerName =>
    // Ignore Finagle's tracing IDs
    headerName.startsWith("X-B3-") || headerName.startsWith("Finagle-")
  }

  private def newDestClient(recorderConfig: HttpRecorderConfig): Service[Request, Response] = {
    debug(s"dest: ${recorderConfig.destAddress}")
    val clientBuilder =
      ClientBuilder()
        .stack(Http.client)
        .name("airframe-http-recorder-proxy")
        .dest(recorderConfig.destAddress.hostAndPort)
        .noFailureAccrual
        .keepAlive(true)
        .retryPolicy(RetryPolicy.tries(3, RetryPolicy.TimeoutAndWriteExceptionsOnly))

    (if (recorderConfig.destAddress.port == 443) {
       clientBuilder.tls(SSLContext.getDefault)
     } else {
       clientBuilder
     }).build()
  }

  private def newRecordStoreForRecording(recorderConfig: HttpRecorderConfig): HttpRecordStore = {
    val recorder = new HttpRecordStore(
      recorderConfig,
      // Delete the previous recordings for the same session name
      dropSession = recorderConfig.dropSessionIfExists
    )
    recorder
  }

  /**
    * Creates an HTTP server that will record HTTP responses.
    */
  def createRecordingServer(recorderConfig: HttpRecorderConfig): FinagleServer = {
    val recorder = newRecordStoreForRecording(recorderConfig)
    new HttpRecorderServer(recorder, HttpRecorderServer.newRecordingService(recorder, newDestClient(recorderConfig)))
  }

  /**
    * Creates an HTTP service that will return recorded responses. If no record is found, it will
    * actually send the request to the destination server and record the response.
    */
  def createPathThroughServer(recorderConfig: HttpRecorderConfig): FinagleServer = {
    val recorder = newRecordStoreForRecording(recorderConfig)
    new HttpRecorderServer(recorder, HttpRecorderServer.newPathThroughService(recorder, newDestClient(recorderConfig)))
  }

  /**
    * Creates an HTTP server that returns recorded HTTP responses.
    * If no matching record is found, use the given fallback handler.
    */
  def createReplayServer(recorderConfig: HttpRecorderConfig): FinagleServer = {
    val recorder = new HttpRecordStore(recorderConfig)
    new HttpRecorderServer(recorder, HttpRecorderServer.newReplayService(recorder))
  }

  /**
    * Creates an HTTP server that returns programmed HTTP responses.
    * If no matching record is found, use the given fallback handler.
    */
  def createProgrammableServer(programmer: HttpRecordStore => Unit): FinagleServer = {
    val recorderConfig = HttpRecorderConfig("localhost")
    val recorder = new HttpRecordStore(recorderConfig, true) {
      override def requestHash(request: Request): Int = {
        computeRequestHash(request, recorderConfig)
      }
    }
    programmer(recorder)
    new HttpRecorderServer(recorder, HttpRecorderServer.newReplayService(recorder))
  }

  def defaultFallBackHandler = {
    Service.mk { request: Request =>
      val r = Response(Status.NotFound)
      r.contentString = s"${request.uri} is not found"
      Future.value(r)
    }
  }

  private[recorder] def computeRequestHash(request: Request, recorderConfig: HttpRecorderConfig): Int = {
    s"${request.method.toString()}:${recorderConfig.destAddress.hostAndPort}${request.uri}:${request.contentString.hashCode}".hashCode
  }

}
