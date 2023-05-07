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
  HttpMessage,
  HttpServerException,
  HttpStatus,
  RPCException,
  RPCStatus,
  RxHttpEndpoint,
  RxHttpFilter
}
import wvlet.airframe.rx.Rx
import wvlet.log.LogSupport

import scala.util.{Failure, Success}

/**
  * Add RPCStatus to the response header and embed the error message to the request body
  */
object RPCResponseFilter extends RxHttpFilter with LogSupport {
  override def apply(request: HttpMessage.Request, next: RxHttpEndpoint): Rx[HttpMessage.Response] = {
    next(request)
      .transform {
        case Success(resp) =>
          setRPCStatus(resp)
        case Failure(e) =>
          e match {
            case ex: HttpServerException =>
              val re = RPCStatus.fromHttpStatus(ex.status).newException(ex.getMessage, ex.getCause)
              rpcExceptionResponse(re)
            case ex: RPCException =>
              rpcExceptionResponse(ex)
            case other =>
              rpcExceptionResponse(RPCStatus.INTERNAL_ERROR_I0.newException(other.getMessage, other))
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

  private[http] def rpcExceptionResponse(e: RPCException): HttpMessage.Response = {
    var resp = Http
      .response(e.status.httpStatus)
      .addHeader(HttpHeader.xAirframeRPCStatus, e.status.code.toString)

    try {
      // Embed RPCError into the response body
      val jsonBody = e.toJson
      resp = resp.withJson(e.toJson)
    } catch {
      case ex: Throwable =>
        // Show warning
        logger.warn(s"Failed to serialize RPCException: ${e}", ex)
    }
    resp
  }
}
