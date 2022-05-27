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
package wvlet.airframe.http.client
import org.scalajs.dom
import org.scalajs.dom.ext.Ajax.InputData
import wvlet.airframe.control.Retry.RetryContext
import wvlet.airframe.control.{CircuitBreaker, CircuitBreakerOpenException, ResultClass}
import wvlet.airframe.http.HttpMessage.{Request, Response}
import wvlet.airframe.http._
import wvlet.log.LogSupport

import java.nio.ByteBuffer
import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.scalajs.js.typedarray.{ArrayBuffer, TypedArrayBuffer}
import scala.util.{Failure, Success, Try}

class JSAsyncClient(private[client] val config: HttpClientConfig, serverAddress: Option[ServerAddress] = None)
    extends AsyncClient
    with LogSupport {

  private[client] implicit val executionContext: ExecutionContext = config.newExecutionContext
  private val circuitBreaker: CircuitBreaker                      = config.circuitBreaker.withName(s"${serverAddress}")

  /**
    * Provide the underlying ExecutionContext. This is only for internal-use
    * @return
    */
  private[http] def getExecutionContext: ExecutionContext = executionContext

  override def close(): Unit = {
    // nothing to do
  }

  override def send(
      req: HttpMessage.Request,
      requestFilter: HttpMessage.Request => HttpMessage.Request
  ): Future[HttpMessage.Response] = {
    val request = buildRequest(req, requestFilter)
    dispatch(config.retryContext, request)
  }

  override def sendSafe(
      req: HttpMessage.Request,
      requestFilter: HttpMessage.Request => HttpMessage.Request
  ): Future[HttpMessage.Response] = {
    send(req, requestFilter).transform { ret =>
      ret match {
        case Failure(e: HttpClientException) =>
          Success(e.response.toHttpResponse)
        case _ =>
          ret
      }
    }
  }

  private def buildRequest(request: Request, requestFilter: Request => Request): Request = {
    request
      // Apply the default filter first
      .withFilter(config.requestFilter)
      // Apply RPC encoding filter
      .withFilter { r =>
        config.rpcEncoding match {
          case RPCEncoding.MsgPack =>
            r.withContentTypeMsgPack.withAcceptMsgPack
          case RPCEncoding.JSON =>
            r.withContentTypeJson.withAcceptJson
        }
      }
      // Lastly, apply the user-provided filter
      .withFilter(requestFilter)
  }

  /**
    * Send the request. If necessary, retry the request
    * @param retryContext
    * @param request
    * @return
    */
  private def dispatch(retryContext: RetryContext, request: Request): Future[Response] = {
    try {
      // This will throw CircuitBreakerException if the circuit is open
      circuitBreaker.verifyConnection
      dispatchInternal(retryContext, request)
    } catch {
      case e: CircuitBreakerOpenException =>
        Future.failed(e)
    }
  }

  private def dispatchInternal(retryContext: RetryContext, request: Request): Future[Response] = {
    val xhr = new dom.XMLHttpRequest()
    val uri = serverAddress.map(address => s"${address.uri}${request.uri}").getOrElse(request.uri)

    trace(s"Sending request: ${request}")
    xhr.open(request.method, uri)
    xhr.responseType = "arraybuffer"
    xhr.timeout = 0
    xhr.withCredentials = false
    // Setting the header must be called after xhr.open(...)
    request.header.entries.foreach { x => xhr.setRequestHeader(x.key, x.value) }

    val promise           = Promise[Response]()
    val data: Array[Byte] = request.contentBytes
    if (data.isEmpty) {
      xhr.send()
    } else {
      val input: InputData = ByteBuffer.wrap(data)
      xhr.send(input)
    }

    xhr.onreadystatechange = { (e: dom.Event) =>
      if (xhr.readyState == 4) { // Ajax request is DONE
        // Prepare HttpMessage.Response
        var resp = Http.response(HttpStatus.ofCode(xhr.status))

        // This part needs to be exception-free
        Try {
          // Set response headers
          val header = HttpMultiMap.newBuilder
          xhr
            .getAllResponseHeaders()
            .split("\n")
            .foreach { line =>
              line.split(":") match {
                case Array(k, v) => header += k.trim -> v.trim
                case _           =>
              }
            }
          resp = resp.withHeader(header.result())
        }

        // This part also needs to be exception-free
        Try {
          // Read response content
          Option(xhr.response).foreach { r =>
            val arrayBuffer = r.asInstanceOf[ArrayBuffer]
            val dst         = new Array[Byte](arrayBuffer.byteLength)
            TypedArrayBuffer.wrap(arrayBuffer).get(dst, 0, arrayBuffer.byteLength)
            resp = resp.withContent(dst)
          }
        }
        trace(s"Get response: ${resp}")

        retryContext.resultClassifier(resp) match {
          case ResultClass.Succeeded =>
            circuitBreaker.recordSuccess
            // if ((xhr.status >= 200 && xhr.status < 300) || xhr.status == 304)
            promise.success(resp)
          case ResultClass.Failed(isRetryable, cause, extraWait) =>
            circuitBreaker.recordFailure(cause)
            if (!retryContext.canContinue) {
              promise.failure(HttpClientMaxRetryException(resp, retryContext, cause))
            } else if (!isRetryable) {
              promise.failure(cause)
            } else {
              val nextRetry  = retryContext.nextRetry(cause)
              val waitMillis = retryContext.nextWaitMillis
              // Wait before the next request
              scalajs.js.timers.setTimeout(waitMillis) {
                dispatch(nextRetry, request).onComplete {
                  case Success(resp) =>
                    promise.success(resp)
                  case Failure(e) =>
                    promise.failure(e)
                }
              }
            }
        }
      }
    }

    val future = promise.future
    future
  }

}
