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

import com.twitter.finagle.http.{Request, Response}
import com.twitter.finagle.{Service, SimpleFilter}
import com.twitter.util.Future
import wvlet.airframe.http.finagle.FinagleServer.FinagleService
import wvlet.airframe.http.finagle.{Finagle, FinagleServer, FinagleServerConfig}
import wvlet.log.LogSupport
import wvlet.log.io.IOUtil

/**
  * A FinagleServer wrapper to close HttpRecordStore when the server terminates
  */
class HttpRecorderServer(recordStore: HttpRecordStore, finagleService: FinagleService)
    extends FinagleServer(
      Finagle.server
        .withName(s"[http-recorder] ${recordStore.recorderConfig.recorderName}")
        .withPort(recordStore.recorderConfig.port.getOrElse(IOUtil.unusedPort)),
      finagleService
    ) {
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

  override def close(): Unit = {
    super.close()
    recordStore.close()
  }
}

object HttpRecorderServer {
  def newRecordingService(recordStore: HttpRecordStore, destClient: Service[Request, Response]): FinagleService = {
    new RecordingFilter(recordStore) andThen destClient
  }

  def newReplayService(recordStore: HttpRecordStore): FinagleService = {
    new ReplayFilter(recordStore) andThen new FallbackService(recordStore)
  }

  /**
    * A request filter for replaying responses for known requests, and sending the requests to the destination server
    * for unknown requests.
    */
  def newRecordProxyService(recordStore: HttpRecordStore, destClient: Service[Request, Response]): FinagleService = {
    new ReplayFilter(recordStore) andThen new RecordingFilter(recordStore) andThen destClient
  }

  /**
    * An HTTP request filter for recording HTTP responses
    */
  class RecordingFilter(recordStore: HttpRecordStore) extends SimpleFilter[Request, Response] with LogSupport {
    override def apply(request: Request, service: Service[Request, Response]): Future[Response] = {
      // Rewrite the target host for proxying
      request.host = recordStore.recorderConfig.destAddress.hostAndPort
      service(request).map { response =>
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
  class ReplayFilter(recordStore: HttpRecordStore) extends SimpleFilter[Request, Response] with LogSupport {
    override def apply(request: Request, service: Service[Request, Response]): Future[Response] = {
      // Rewrite the target host for proxying
      request.host = recordStore.recorderConfig.destAddress.hostAndPort
      recordStore.findNext(request) match {
        case Some(record) =>
          // Replay the recorded response
          trace(s"Found a recorded response: ${record.summary}")
          val r = record.toResponse
          // Add an HTTP header to indicate that the response is a recorded one
          r.headerMap.put("X-Airframe-Record-Time", record.createdAt.toString)
          Future.value(r)
        case None =>
          trace(s"No recording is found for ${request}")
          // Fallback to the default handler
          service(request)
      }
    }
  }

  class FallbackService(recordStore: HttpRecordStore) extends Service[Request, Response] {
    override def apply(request: Request): Future[Response] = {
      recordStore.recorderConfig.fallBackHandler(request)
    }
  }
}
