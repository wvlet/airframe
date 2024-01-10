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

import wvlet.airframe.http.HttpMessage.EmptyMessage
import wvlet.airframe.http.{Http, HttpHeader, HttpMessage, HttpStatus, RxHttpEndpoint, ServerAddress}
import wvlet.airframe.http.client.SyncClient
import wvlet.airframe.http.netty.NettyBackend
import wvlet.airframe.rx.Rx
import wvlet.log.LogSupport

import java.util.Locale

case class HttpRecorderConfig(
    recorderName: String = "http-recorder",
    destUri: String = "localhost",
    sessionName: String = "default",
    expirationTime: Option[String] = None, // Do not expire records by default
    // the folder to store response records
    dbFileName: String = "http-records",
    storageFolder: String = ".airframe/http",
    recordTableName: String = "record",
    // Explicitly specify the port to use
    port: Option[Int] = None,
    // Used for computing hash key for matching requests
    requestMatcher: HttpRequestMatcher = new HttpRequestMatcher.DefaultHttpRequestMatcher(),
    excludeHeaderFilterForRecording: (String, String) => Boolean = HttpRecorder.defaultExcludeHeaderFilterForRecording,
    fallBackHandler: RxHttpEndpoint = HttpRecorder.defaultFallBackHandler
) {
  private[http] def sqliteFilePath = s"${storageFolder}/${dbFileName}.sqlite"

  lazy val destAddress = ServerAddress(destUri)

  def withDestUri(destUri: String): HttpRecorderConfig = {
    this.copy(destUri = destUri)
  }
  def withRecorderName(name: String): HttpRecorderConfig = {
    this.copy(recorderName = name)
  }
  def withSessionName(sessionName: String): HttpRecorderConfig = {
    this.copy(sessionName = sessionName)
  }
  def withExpirationTime(expirationTime: String): HttpRecorderConfig = {
    this.copy(expirationTime = Some(expirationTime))
  }
  def noExpirationTime: HttpRecorderConfig = {
    this.copy(expirationTime = None)
  }
  def withDbFileName(dbFileName: String): HttpRecorderConfig = {
    this.copy(dbFileName = dbFileName)
  }
  def withRecordTableName(recordTableName: String): HttpRecorderConfig = {
    this.copy(recordTableName = recordTableName)
  }
  def withPort(port: Int): HttpRecorderConfig = {
    this.copy(port = Some(port))
  }
  def withRequestMatcher(requestMatcher: HttpRequestMatcher): HttpRecorderConfig = {
    this.copy(requestMatcher = requestMatcher)
  }
  def withExcludeHeaderFilterForRecording(
      excludeHeaderFilterForRecording: (String, String) => Boolean
  ): HttpRecorderConfig = {
    this.copy(excludeHeaderFilterForRecording = excludeHeaderFilterForRecording)
  }

}

/**
  * Creates a proxy server for recording and replaying HTTP responses. This is useful for simulate the behavior of Web
  * services, that are usually too heavy to use in an restricted environment (e.g., CI servers)
  */
object HttpRecorder extends LogSupport {
  def config: HttpRecorderConfig = HttpRecorderConfig()

  def defaultExcludeHeaderFilterForRecording: (String, String) => Boolean = { (key: String, value: String) =>
    key.toLowerCase(Locale.ENGLISH).contains("authorization")
  }

  private def newDestClient(recorderConfig: HttpRecorderConfig): SyncClient = {
    debug(s"dest: ${recorderConfig.destAddress.hostAndPort}")
    Http.client
      .withName("airframe-http-recorder-proxy")
      .newSyncClient(recorderConfig.destAddress.hostAndPort)
  }

  private def destProxyEndpoint(recorderConfig: HttpRecorderConfig): RxHttpEndpoint = {
    val client = newDestClient(recorderConfig)
    new RxHttpEndpoint {
      override def apply(request: HttpMessage.Request): Rx[HttpMessage.Response] = {
        // Remove Netty-specific headers added when relaying the request
        var newRequest = request
          // TODO Support HTTP2
          .removeHeader("HTTP2-Settings")
          .removeHeader(HttpHeader.Upgrade)
          .removeHeader(HttpHeader.Connection)
          .noHost

        if (newRequest.message.isEmpty) {
          newRequest = newRequest.removeHeader(HttpHeader.ContentLength)
        }
        val ret = Rx.single(client.send(newRequest))
        ret
      }
    }
  }

  private def newRecordStoreForRecording(recorderConfig: HttpRecorderConfig, dropSession: Boolean): HttpRecordStore = {
    val recorder = new HttpRecordStore(
      recorderConfig,
      // Delete the previous recordings for the same session name
      dropSession = dropSession
    )
    recorder
  }

  /**
    * Creates an HTTP proxy server that will return recorded responses. If no record is found, it will actually send the
    * request to the destination server and record the response.
    */
  def createRecorderProxy(
      recorderConfig: HttpRecorderConfig,
      dropExistingSession: Boolean = false
  ): HttpRecorderServer = {
    val recorder = newRecordStoreForRecording(recorderConfig, dropExistingSession)
    val server = new HttpRecorderServer(
      recorder,
      HttpRecorderServer.newRecordProxyService(recorder, destProxyEndpoint(recorderConfig))
    )
    server.start
    server
  }

  /**
    * Creates an HTTP server that will record HTTP responses.
    */
  def createRecordingServer(
      recorderConfig: HttpRecorderConfig,
      dropExistingSession: Boolean = true
  ): HttpRecorderServer = {
    val recorder = newRecordStoreForRecording(recorderConfig, dropExistingSession)
    val server =
      new HttpRecorderServer(
        recorder,
        HttpRecorderServer.newRecordingService(recorder, destProxyEndpoint(recorderConfig))
      )
    server.start
    server
  }

  /**
    * Creates an HTTP server that returns only recorded HTTP responses. If no matching record is found, use the given
    * fallback handler.
    */
  def createServer(recorderConfig: HttpRecorderConfig): HttpRecorderServer = {
    val recorder = new HttpRecordStore(recorderConfig)
    // Return the server instance as FinagleServer to avoid further recording
    val server = new HttpRecorderServer(recorder, HttpRecorderServer.newReplayService(recorder))
    server.start
    server
  }

  /**
    * Create an in-memory programmable server, whose recorded response will be discarded after closing the server. This
    * is useful for debugging HTTP clients
    */
  def createInMemoryServer(recorderConfig: HttpRecorderConfig): HttpRecorderServer = {
    val recorder = new HttpRecordStore(recorderConfig, inMemory = true)
    val server   = new HttpRecorderServer(recorder, HttpRecorderServer.newReplayService(recorder))
    server.start
    server
  }

  def defaultFallBackHandler: RxHttpEndpoint = new RxHttpEndpoint {
    override def apply(request: HttpMessage.Request): Rx[HttpMessage.Response] = {
      Rx.const(
        Http
          .response(HttpStatus.NotFound_404)
          .withContent(s"${request.uri} is not found")
      )
    }
  }
}
