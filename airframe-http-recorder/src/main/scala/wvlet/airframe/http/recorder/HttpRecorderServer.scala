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

import wvlet.airframe.{Design, Session}
import wvlet.airframe.http.HttpMessage.{Request, Response}
import wvlet.airframe.http.{RxEndpoint, RxFilter}
import wvlet.airframe.http.netty.{Netty, NettyServer, NettyServerConfig}
import wvlet.airframe.http.router.RxRouter
import wvlet.airframe.rx.{Rx, RxStream}
import wvlet.log.LogSupport
import wvlet.log.io.IOUtil

/**
  * A FinagleServer wrapper to close HttpRecordStore when the server terminates
  */
class HttpRecorderServer(recordStore: HttpRecordStore, endpoint: RxEndpoint) extends AutoCloseable {

  private val serverConfig: NettyServerConfig = Netty.server
    .withName(s"[http-recorder] ${recordStore.recorderConfig.recorderName}")
    .withPort(recordStore.recorderConfig.port.getOrElse(IOUtil.unusedPort))
    .withRouter(RxRouter.of(endpoint))

  private var diSession: Option[Session]  = None
  private var server: Option[NettyServer] = None

  def localAddress: String =
    server.map(_.localAddress).getOrElse(throw new IllegalStateException("Server is not yet started"))

  def clearSession: Unit = {
    recordStore.clearSession
  }

  def record(request: Request, response: Response): Unit = {
    recordStore.record(request, response)
  }

  def dumpSessionAsJson: String = {
    recordStore.dumpSessionAsJson
  }

  def dumpAllSessionsAsJson: String = {
    recordStore.dumpAllSessionsAsJson
  }

  def dumpSessionAsYaml: String = {
    recordStore.dumpSessionAsYaml
  }

  def dumpAllSessionsAsYaml: String = {
    recordStore.dumpAllSessionsAsYaml
  }

  def recordIfNotExists(request: Request, response: Response): Unit = {
    // Do not increment the request hit counter to replay the recorded responses
    recordStore
      .findNext(request, incrementHitCount = false)
      .getOrElse {
        record(request, response)
      }
  }

  def start: Unit = {
    diSession.foreach(_.close())
    diSession = Some(Design.empty.newSession)
    server = Some(serverConfig.newServer(diSession.get))
  }

  override def close(): Unit = {
    diSession.foreach(_.shutdown)
    endpoint.close()
    recordStore.close()
  }
}

object HttpRecorderServer {

  def newRecordingService(recordStore: HttpRecordStore, destClient: RxEndpoint): RxEndpoint = {
    new RecordingFilter(recordStore) andThen destClient
  }

  def newReplayService(recordStore: HttpRecordStore): RxEndpoint = {
    new ReplayFilter(recordStore) andThen recordStore.recorderConfig.fallBackHandler
  }

  /**
    * A request filter for replaying responses for known requests, and sending the requests to the destination server
    * for unknown requests.
    */
  def newRecordProxyService(recordStore: HttpRecordStore, destClient: RxEndpoint): RxEndpoint = {
    new ReplayFilter(recordStore) andThen new RecordingFilter(recordStore) andThen destClient
  }

  /**
    * An HTTP request filter for recording HTTP responses
    */
  class RecordingFilter(recordStore: HttpRecordStore) extends RxFilter with LogSupport {
    override def apply(request: Request, endpoint: RxEndpoint): RxStream[Response] = {
      // Rewrite the target host for proxying
      val newRequest = request.noHost
      endpoint(newRequest).map { response =>
        trace(s"Recording the response for ${request}")
        // Record the result
        recordStore.record(request, response)
        // Return the original response
        response
      }
    }
  }

  /**
    * An HTTP request filter for returning recorded HTTP responses
    */
  class ReplayFilter(recordStore: HttpRecordStore) extends RxFilter with LogSupport {
    override def apply(request: Request, service: RxEndpoint): RxStream[Response] = {
      // Rewrite the target host for proxying
      val newRequest = request.withHost(recordStore.recorderConfig.destAddress.hostAndPort)
      recordStore.findNext(newRequest) match {
        case Some(record) =>
          // Replay the recorded response
          trace(s"Found a recorded response: ${record.summary}")
          Rx.single(
            record.toResponse
              // Add an HTTP header to indicate that the response is a recorded one
              .withHeader("X-Airframe-Record-Time", record.createdAt.toString)
          )
        case None =>
          trace(s"No recording is found for ${request}")
          // Fallback to the default handler
          service(request)
      }
    }
  }
}
