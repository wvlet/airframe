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
package wvlet.airframe.http.internal

import wvlet.airframe.http.{
  Http,
  HttpHeader,
  HttpLogger,
  HttpMessage,
  HttpMultiMap,
  HttpServerException,
  HttpStatus,
  RPCContext,
  RPCException,
  RPCStatus,
  RxHttpEndpoint,
  RxHttpFilter
}
import wvlet.airframe.rx.Rx
import wvlet.log.LogSupport

import scala.util.{Failure, Success}

/**
  * A filter for managing RPC status header, logs, and errors. Exception messages will be embedded to the response body.
  */
class RPCResponseFilter(httpLogger: HttpLogger) extends RxHttpFilter with LogSupport {
  override def apply(request: HttpMessage.Request, next: RxHttpEndpoint): Rx[HttpMessage.Response] = {

    val logContext = HttpLogs.LogContext(request, httpLogger, None, Some(RPCContext.current))

    next(request)
      .transform {
        case Success(resp) =>
          logContext.logResponse(setRPCStatus(resp), None)
        case Failure(e) =>
          e match {
            case ex: HttpServerException =>
              val re = RPCStatus.fromHttpStatus(ex.status).newException(ex.getMessage, ex.getCause)
              logContext.logResponse(re.toResponse, Some(re))
            case ex: RPCException =>
              logContext.logResponse(ex.toResponse, Some(ex))
            case other =>
              val ex = RPCStatus.INTERNAL_ERROR_I0.newException(other.getMessage, other)
              // Report the original error to the log
              logContext.logResponse(ex.toResponse, Some(other))
          }
      }
  }

  private def setRPCStatus(resp: HttpMessage.Response): HttpMessage.Response = {
    resp.getHeader(HttpHeader.xAirframeRPCStatus) match {
      case Some(status) =>
        resp
      case None =>
        val status = RPCStatus.fromHttpStatus(resp.status)
        resp.addHeader(HttpHeader.xAirframeRPCStatus, status.code.toString)
    }
  }
}
