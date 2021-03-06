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
package wvlet.airframe.http.js
import java.nio.ByteBuffer
import org.scalajs.dom
import org.scalajs.dom.ext.Ajax.InputData
import org.scalajs.dom.window
import wvlet.airframe.codec.{MessageCodec, MessageCodecFactory}
import wvlet.airframe.control.Retry.RetryContext
import wvlet.airframe.control.{CircuitBreaker, CircuitBreakerOpenException, ResultClass, Retry}
import wvlet.airframe.http.HttpClient.defaultBeforeRetryAction
import wvlet.airframe.http.HttpMessage._
import wvlet.airframe.http._
import wvlet.airframe.http.js.JSHttpClient.MessageEncoding
import wvlet.airframe.surface.{Primitive, Surface}
import wvlet.log.LogSupport

import scala.concurrent.{Future, Promise}
import scala.scalajs.js.typedarray.{ArrayBuffer, TypedArrayBuffer}
import scala.util.{Failure, Success, Try}

object JSHttpClient {

  sealed trait MessageEncoding
  object MessageEncoding {
    object MessagePackEncoding extends MessageEncoding
    object JsonEncoding        extends MessageEncoding
  }

  // An http client for production-use
  def defaultClient = {
    val protocol = window.location.protocol.stripSuffix(":")
    val hostname = window.location.hostname
    if (hostname == "localhost" && protocol == "http") {
      // Use local client for testing
      localClient
    } else {
      val port    = Option(window.location.port).map(x => if (x.isEmpty) "" else s":${x}").getOrElse("")
      val address = ServerAddress(s"${protocol}://${hostname}${port}")
      JSHttpClient(JSHttpClientConfig(serverAddress = Some(address)))
    }
  }

  // An http client that can be used for local testing
  def localClient = JSHttpClient()

  def defaultHttpClientRetryer: RetryContext = {
    Retry
      .withBackOff(maxRetry = 3)
      .withResultClassifier(HttpClientException.classifyHttpResponse[Response])
      .withErrorClassifier { e: Throwable => Retry.nonRetryableFailure(e) }
      .beforeRetry(defaultBeforeRetryAction[Request])
  }
}

case class JSHttpClientConfig(
    serverAddress: Option[ServerAddress] = None,
    requestEncoding: MessageEncoding = MessageEncoding.MessagePackEncoding,
    requestFilter: Request => Request = identity,
    retryContext: RetryContext = JSHttpClient.defaultHttpClientRetryer,
    codecFactory: MessageCodecFactory = MessageCodecFactory.defaultFactoryForJSON,
    // The default circuit breaker, which will be open after 5 consecutive failures
    circuitBreaker: CircuitBreaker = CircuitBreaker.withConsecutiveFailures(5)
) {
  def withServerAddress(newServerAddress: ServerAddress): JSHttpClientConfig = {
    this.copy(serverAddress = Some(newServerAddress))
  }
  def withRequestEncoding(newRequestEncoding: MessageEncoding): JSHttpClientConfig = {
    this.copy(requestEncoding = newRequestEncoding)
  }
  def withRequestFilter(newRequestFilter: Request => Request): JSHttpClientConfig =
    this.copy(requestFilter = newRequestFilter)

  def withRetry(f: RetryContext => RetryContext): JSHttpClientConfig = {
    this.copy(retryContext = f(retryContext))
  }
  def noRetry: JSHttpClientConfig = this.copy(retryContext = retryContext.noRetry)

  def withCodecFactory(newCodecFactory: MessageCodecFactory): JSHttpClientConfig = {
    this.copy(codecFactory = newCodecFactory)
  }
  def withCircuitBreaker(f: CircuitBreaker => CircuitBreaker): JSHttpClientConfig = {
    this.copy(circuitBreaker = f(circuitBreaker))
  }
  def noCircuitBreaker: JSHttpClientConfig = {
    this.copy(circuitBreaker = CircuitBreaker.alwaysClosed)
  }
}

/**
  * HttpClient utilities for Scala.js.
  *
  * We do not implement HttpClient[F, Request, Response] interface as no TypeTag is available in Scala.js
  */
case class JSHttpClient(config: JSHttpClientConfig = JSHttpClientConfig()) extends LogSupport {
  import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

  private def codecFactory   = config.codecFactory.withMapOutput
  private def circuitBreaker = config.circuitBreaker

  /**
    * Modify the configuration based on the current configuration
    * @param configFilter
    * @return
    */
  def withConfig(configFilter: JSHttpClientConfig => JSHttpClientConfig): JSHttpClient = {
    this.copy(config = configFilter(config))
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
    val uri = config.serverAddress.map(address => s"${address.uri}${request.uri}").getOrElse(request.uri)
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
            //if ((xhr.status >= 200 && xhr.status < 300) || xhr.status == 304)
            promise.success(resp)
          case ResultClass.Failed(isRetryable, cause, extraWait) =>
            circuitBreaker.recordFailure(cause)
            if (!retryContext.canContinue) {
              promise.failure(HttpClientMaxRetryException(resp, retryContext, cause))
            } else if (!isRetryable) {
              promise.failure(cause)
            } else {
              dispatch(retryContext.nextRetry(cause), request).onComplete {
                case Success(resp) =>
                  promise.success(resp)
                case Failure(e) =>
                  promise.failure(e)
              }
            }
        }
      }
    }

    val future = promise.future
    future
  }

  def sendRaw(request: Request, requestFilter: Request => Request = identity): Future[Response] = {
    dispatch(config.retryContext, finalizeRequest(request, requestFilter))
  }

  def send[OperationResponse](
      originalRequest: Request,
      operationResponseSurface: Surface,
      requestFilter: Request => Request = identity
  ): Future[OperationResponse] = {
    // Apply the default request filter first, and then apply the custom filter
    val request = finalizeRequest(originalRequest, requestFilter)
    dispatch(config.retryContext, request).map { resp =>
      operationResponseSurface match {
        case s if s.rawType == classOf[HttpMessage.Response] =>
          resp.asInstanceOf[OperationResponse]
        case Primitive.Unit =>
          null.asInstanceOf[OperationResponse]
        case _ =>
          val responseCodec =
            codecFactory.of(operationResponseSurface).asInstanceOf[MessageCodec[OperationResponse]]
          // Read the response body as MessagePack or JSON
          val ct = resp.contentType
          resp.contentType match {
            case Some("application/x-msgpack") =>
              responseCodec.fromMsgPack(resp.contentBytes)
            case _ =>
              val json = resp.contentString
              if (json.nonEmpty) {
                responseCodec.fromJson(json)
              } else {
                throw new HttpClientException(resp, resp.status, "Empty response from the server")
              }
          }
      }
    }
  }

  private[js] def prepareRequestBody[Resource](
      request: Request,
      resource: Resource,
      resourceSurface: Surface
  ): Request = {
    val resourceCodec = codecFactory.of(resourceSurface).asInstanceOf[MessageCodec[Resource]]
    // Support MsgPack or JSON RPC
    config.requestEncoding match {
      case MessageEncoding.MessagePackEncoding =>
        request.withContentTypeMsgPack.withAcceptMsgPack.withContent(resourceCodec.toMsgPack(resource))
      case MessageEncoding.JsonEncoding =>
        request.withContentTypeJson.withContent(resourceCodec.toJson(resource))
    }
  }

  private def finalizeRequest(request: Request, requestFilter: Request => Request): Request = {
    request
      .withFilter(config.requestFilter)
      .withFilter { r =>
        config.requestEncoding match {
          case MessageEncoding.MessagePackEncoding =>
            r.withContentTypeMsgPack.withAcceptMsgPack
          case MessageEncoding.JsonEncoding =>
            r.withContentTypeJson
        }
      }
      .withFilter(requestFilter)
  }

  def sendResource[Resource](
      request: Request,
      resource: Resource,
      resourceSurface: Surface,
      requestFilter: Request => Request
  ): Future[Resource] = {
    send(prepareRequestBody(request, resource, resourceSurface), resourceSurface, requestFilter)
  }

  def sendResourceOps[Resource, OperationResponse](
      request: Request,
      resource: Resource,
      resourceSurface: Surface,
      operationResponseSurface: Surface,
      requestFilter: Request => Request
  ): Future[OperationResponse] = {
    send(
      prepareRequestBody(request, resource, resourceSurface),
      operationResponseSurface,
      requestFilter = requestFilter
    )
  }

  def get[Resource](
      resourcePath: String,
      resourceSurface: Surface,
      requestFilter: Request => Request = identity
  ): Future[Resource] = {
    send(Http.request(HttpMethod.GET, resourcePath), resourceSurface, requestFilter = requestFilter)
  }

  def getOps[Resource, OperationResponse](
      resourcePath: String,
      resource: Resource,
      resourceSurface: Surface,
      operationResponseSurface: Surface,
      requestFilter: Request => Request = identity
  ): Future[OperationResponse] = {
    val path = HttpClient.buildResourceUri(resourcePath, resource, resourceSurface)
    send(
      Http.request(HttpMethod.GET, path),
      operationResponseSurface,
      requestFilter = requestFilter
    )
  }

  def post[Resource](
      resourcePath: String,
      resource: Resource,
      resourceSurface: Surface,
      requestFilter: Request => Request = identity
  ): Future[Resource] = {
    sendResource(Http.request(HttpMethod.POST, resourcePath), resource, resourceSurface, requestFilter)
  }

  def postOps[Resource, OperationResponse](
      resourcePath: String,
      resource: Resource,
      resourceSurface: Surface,
      operationResponseSurface: Surface,
      requestFilter: Request => Request = identity
  ): Future[OperationResponse] = {
    sendResourceOps[Resource, OperationResponse](
      Http.request(HttpMethod.POST, resourcePath),
      resource,
      resourceSurface,
      operationResponseSurface,
      requestFilter
    )
  }

  def put[Resource](
      resourcePath: String,
      resource: Resource,
      resourceSurface: Surface,
      requestFilter: Request => Request = identity
  ): Future[Resource] = {
    sendResource(Http.request(HttpMethod.PUT, resourcePath), resource, resourceSurface, requestFilter)
  }

  def putOps[Resource, OperationResponse](
      resourcePath: String,
      resource: Resource,
      resourceSurface: Surface,
      operationResponseSurface: Surface,
      requestFilter: Request => Request = identity
  ): Future[OperationResponse] = {
    sendResourceOps[Resource, OperationResponse](
      Http.request(HttpMethod.PUT, resourcePath),
      resource,
      resourceSurface,
      operationResponseSurface,
      requestFilter
    )
  }

  def delete[Resource](
      resourcePath: String,
      resource: Resource,
      resourceSurface: Surface,
      requestFilter: Request => Request = identity
  ): Future[Resource] = {
    sendResource(Http.request(HttpMethod.DELETE, resourcePath), resource, resourceSurface, requestFilter)
  }

  def deleteOps[Resource, OperationResponse](
      resourcePath: String,
      resource: Resource,
      resourceSurface: Surface,
      operationResponseSurface: Surface,
      requestFilter: Request => Request = identity
  ): Future[OperationResponse] = {
    sendResourceOps[Resource, OperationResponse](
      Http.request(HttpMethod.DELETE, resourcePath),
      resource,
      resourceSurface,
      operationResponseSurface,
      requestFilter
    )
  }

  def patch[Resource](
      resourcePath: String,
      resource: Resource,
      resourceSurface: Surface,
      requestFilter: Request => Request = identity
  ): Future[Resource] = {
    sendResource(Http.request(HttpMethod.PATCH, resourcePath), resource, resourceSurface, requestFilter)
  }

  def patchOps[Resource, OperationResponse](
      resourcePath: String,
      resource: Resource,
      resourceSurface: Surface,
      operationResponseSurface: Surface,
      requestFilter: Request => Request = identity
  ): Future[OperationResponse] = {
    sendResourceOps[Resource, OperationResponse](
      Http.request(HttpMethod.PATCH, resourcePath),
      resource,
      resourceSurface,
      operationResponseSurface,
      requestFilter
    )
  }
}
