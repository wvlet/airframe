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
import org.scalajs.dom.window
import wvlet.airframe.codec.{MessageCodec, MessageCodecFactory}
import wvlet.airframe.control.Retry.RetryContext
import wvlet.airframe.control.{CircuitBreaker, Retry}
import wvlet.airframe.http.HttpClient.defaultBeforeRetryAction
import wvlet.airframe.http.HttpMessage._
import wvlet.airframe.http._
import wvlet.airframe.http.client.{AsyncClientImpl, JSClientChannel, JSHttpClientBackend}
import wvlet.airframe.http.js.JSHttpClient.MessageEncoding
import wvlet.airframe.rx.{Rx, RxStream}
import wvlet.airframe.surface.{Primitive, Surface}
import wvlet.log.LogSupport

import scala.concurrent.{ExecutionContext, Future}

object JSHttpClient {

  @deprecated("Use RPCEncoding instead", "21.5.0")
  sealed trait MessageEncoding
  object MessageEncoding {
    object MessagePackEncoding extends MessageEncoding
    object JsonEncoding        extends MessageEncoding
  }

  // An http client for production-use
  @deprecated("Use Http.client.newJSClient instead", "22.6.0")
  def defaultClient = {
    resolveServerAddress match {
      case None =>
        // Use local client for testing
        localClient
      case Some(address) =>
        JSHttpClient(JSHttpClientConfig(serverAddress = Some(ServerAddress(address))))
    }
  }

  // An http client that can be used for local testing
  def localClient = JSHttpClient()

  /**
    * Find the host server address from the browser context
    */
  def resolveServerAddress: Option[String] = {
    val protocol = window.location.protocol.stripSuffix(":")
    val hostname = window.location.hostname
    if (hostname == "localhost" && protocol == "http") {
      None
    } else {
      val port = Option(window.location.port).map(x => if (x.isEmpty) "" else s":${x}").getOrElse("")
      Some(s"${protocol}://${hostname}${port}")
    }
  }

  def defaultHttpClientRetryer: RetryContext = {
    Retry
      .withBackOff(maxRetry = 3)
      .withResultClassifier(HttpClientException.classifyHttpResponse[Response])
      .withErrorClassifier { (e: Throwable) => Retry.nonRetryableFailure(e) }
      .beforeRetry(defaultBeforeRetryAction[Request])
  }
}

/**
  * TODO: We can use HttpClientConfig instead. This config is left here for the compatibility
  */
case class JSHttpClientConfig(
    serverAddress: Option[ServerAddress] = None,
    requestEncoding: MessageEncoding = MessageEncoding.MessagePackEncoding,
    requestFilter: Request => Request = identity,
    retryContext: RetryContext = JSHttpClient.defaultHttpClientRetryer,
    codecFactory: MessageCodecFactory = MessageCodecFactory.defaultFactoryForJSON,
    // The default circuit breaker, which will be open after 5 consecutive failures
    circuitBreaker: CircuitBreaker = CircuitBreaker.withConsecutiveFailures(5),
    rxConverter: Future[_] => RxStream[_] = { (f: Future[_]) =>
      Rx.future(f)(scala.scalajs.concurrent.JSExecutionContext.queue)
    }
) {
  def toHttpClientConfig: HttpClientConfig = {
    Http.client
      .withRPCEncoding {
        requestEncoding match {
          case MessageEncoding.MessagePackEncoding => RPCEncoding.MsgPack
          case MessageEncoding.JsonEncoding        => RPCEncoding.JSON
        }
      }
      .withRequestFilter(requestFilter)
      .withRetryContext(_ => retryContext)
      .withCodecFactory(codecFactory)
      .withCircuitBreaker(_ => circuitBreaker)
      .withRxConverter(rxConverter)
  }

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

  /**
    * Converter Future[A] to Rx[A]. Use this method when you need to add a common error handler (e.g., with Rx.recover)
    */
  def withRxConverter(f: Future[_] => RxStream[_]): JSHttpClientConfig = {
    this.copy(rxConverter = f)
  }
}

/**
  * HttpClient utilities for Scala.js.
  *
  * We do not implement HttpClient[F, Request, Response] interface as no TypeTag is available in Scala.js
  */
@deprecated("Use Http.client.newJSClient", "22.6.0")
case class JSHttpClient(config: JSHttpClientConfig = JSHttpClientConfig()) extends LogSupport {
  private def codecFactory = config.codecFactory.withMapOutput

  private val channel =
    new JSClientChannel(config.serverAddress.getOrElse(ServerAddress.empty), config.toHttpClientConfig)
  private val client = new AsyncClientImpl(channel, config.toHttpClientConfig)

  private implicit val ec: ExecutionContext = channel.getExecutionContext

  /**
    * Modify the configuration based on the current configuration
    * @param configFilter
    * @return
    */
  def withConfig(configFilter: JSHttpClientConfig => JSHttpClientConfig): JSHttpClient = {
    this.copy(config = configFilter(config))
  }

  def sendRaw(request: Request, requestFilter: Request => Request = identity): Future[Response] = {
    client.withRequestFilter(requestFilter).send(request)
  }

  def send[OperationResponse](
      request: Request,
      operationResponseSurface: Surface,
      requestFilter: Request => Request = identity
  ): Future[OperationResponse] = {
    sendRaw(request, requestFilter).map { resp =>
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
          if (resp.isContentTypeMsgPack) {
            responseCodec.fromMsgPack(resp.contentBytes)
          } else {
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
