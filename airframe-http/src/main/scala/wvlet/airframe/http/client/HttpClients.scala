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

import wvlet.airframe.codec.MessageCodec
import wvlet.airframe.codec.PrimitiveCodec.UnitCodec
import wvlet.airframe.control.Retry.MaxRetryException
import wvlet.airframe.control.{CircuitBreaker, CircuitBreakerOpenException}
import wvlet.airframe.http.HttpMessage.{Request, Response}
import wvlet.airframe.http._
import wvlet.airframe.http.internal.HttpResponseBodyCodec
import wvlet.airframe.rx.Rx
import wvlet.airframe.surface.Surface
import wvlet.log.LogSupport

import scala.util.Try
import scala.util.control.NonFatal

class SyncClientImpl(protected val channel: HttpChannel, val config: HttpClientConfig) extends SyncClient {
  override protected def build(newConfig: HttpClientConfig): SyncClient = {
    new SyncClientImpl(channel, newConfig)
  }
  override def close(): Unit = {
    super.close()
    channel.close()
  }
}

class AsyncClientImpl(protected val channel: HttpChannel, val config: HttpClientConfig) extends AsyncClient {
  override protected def build(newConfig: HttpClientConfig): AsyncClient = new AsyncClientImpl(channel, newConfig)
  override def close(): Unit = {
    super.close()
    channel.close()
  }
}

/**
  * A standard blocking http client interface
  */
trait SyncClient extends SyncClientCompat with HttpClientFactory[SyncClient] with AutoCloseable {

  protected def channel: HttpChannel
  def config: HttpClientConfig

  private val clientLogger: HttpLogger        = config.newHttpLogger
  private val loggingFilter: HttpClientFilter = config.newLoggingFilter(clientLogger)
  private val circuitBreaker: CircuitBreaker  = config.circuitBreaker

  override def close(): Unit = {
    clientLogger.close()
  }

  /**
    * Send an HTTP request and get the response. It will throw an exception for non-successful responses. For example,
    * when receiving non-retryable status code (e.g., 4xx), it will throw HttpClientException. For server side failures
    * (5xx responses), this continues request retry until the max retry count.
    *
    * If it exceeds the number of max retry attempts, HttpClientMaxRetryException will be thrown.
    *
    * @throws HttpClientMaxRetryException
    *   if max retry reaches
    * @throws HttpClientException
    *   for non-retryable error is occurred
    */
  def send(req: Request, context: HttpClientContext = HttpClientContext.empty): Response = {
    val request = config.requestFilter(req)

    var lastResponse: Option[Response] = None
    try {
      config.retryContext.runWithContext(request, circuitBreaker) {
        loggingFilter
          .andThen(config.clientFilter)
          .apply(context)
          .andThen(req => Rx.single(channel.send(req, config)))
          .apply(request)
          .run { resp =>
            lastResponse = Some(resp)
          }
        lastResponse.get
      }
    } catch {
      HttpClients.defaultHttpClientErrorHandler(lastResponse)
    }
  }

  /**
    * Send an HTTP request and returns a response (or the last response if the request is retried). Unlike [[send()]],
    * this method returns a regular Http Response object even for non-retryable responses (e.g., 4xx error code). For
    * retryable responses (e.g., 5xx) this continues retry until the max retry count.
    *
    * After reaching the max retry count, it will return a the last response even for 5xx status code.
    */
  def sendSafe(req: Request, context: HttpClientContext = HttpClientContext.empty): Response = {
    try {
      send(req, context)
    } catch {
      case e: HttpClientException =>
        e.response.toHttpResponse
    }
  }

  def readAsInternal[Resp](
      req: Request,
      responseSurface: Surface
  ): Resp = {
    val resp: Response = send(req, HttpClientContext(config.name))
    HttpClients.parseResponse[Resp](config, responseSurface, resp)
  }

  def callInternal[Req, Resp](
      req: Request,
      requestSurface: Surface,
      responseSurface: Surface,
      requestContent: Req
  ): Resp = {
    val newRequest     = HttpClients.prepareRequest(config, req, requestSurface, requestContent)
    val resp: Response = send(newRequest, HttpClientContext(config.name))
    HttpClients.parseResponse[Resp](config, responseSurface, resp)
  }

  /**
    * Send an RPC request (POST) and return the RPC response. This method will throw RPCException when an error happens
    * @param method
    * @param request
    * @tparam Req
    * @return
    *
    * @throws RPCException
    *   when RPC request fails
    */
  def rpc[Req, Resp](method: RPCMethod, requestContent: Req): Resp = {
    val request: Request =
      HttpClients.prepareRPCRequest(config, method.path, method.requestSurface, requestContent)

    val context = HttpClientContext(
      clientName = config.name,
      rpcMethod = Some(method),
      rpcInput = Some(requestContent)
    )
    // sendSafe method internally handles retries and HttpClientException, and then it returns the last response
    val response: Response = sendSafe(request, context = context)

    // Parse the RPC response
    if (response.status.isSuccessful) {
      val ret = HttpClients.parseRPCResponse(config, response, method.responseSurface)
      ret.asInstanceOf[Resp]
    } else {
      // Parse the RPC error message
      throw RPCException.fromResponse(response)
    }
  }
}

/**
  * A standard async http client interface for Rx[_]
  */
trait AsyncClient extends AsyncClientCompat with HttpClientFactory[AsyncClient] with AutoCloseable {
  protected def channel: HttpChannel
  def config: HttpClientConfig

  private val httpLogger: HttpLogger          = config.newHttpLogger
  private val loggingFilter: HttpClientFilter = config.newLoggingFilter(httpLogger)
  private val circuitBreaker: CircuitBreaker  = config.circuitBreaker

  override def close(): Unit = {
    httpLogger.close()
  }

  /**
    * Send an HTTP request and get the response in Rx[Response] type.
    *
    * It will return `Rx[HttpClientException]` for non-successful responses. For example, when receiving non-retryable
    * status code (e.g., 4xx), it will return Rx[HttpClientException]. For server side failures (5xx responses), this
    * continues request retry until the max retry count.
    *
    * If it exceeds the number of max retry attempts, it will return Rx[HttpClientMaxRetryException].
    */
  def send(req: Request, context: HttpClientContext = HttpClientContext.empty): Rx[Response] = {
    val request                        = config.requestFilter(req)
    var lastResponse: Option[Response] = None
    config.retryContext
      .runAsyncWithContext(request, circuitBreaker) {
        loggingFilter
          .andThen(config.clientFilter)
          .apply(context)
          .andThen(req => channel.sendAsync(req, config))
          .apply(request)
          .map { resp =>
            // Remember the last response for error reporting purpose
            lastResponse = Some(resp)
            resp
          }
      }
      .recover {
        HttpClients.defaultHttpClientErrorHandler(lastResponse)
      }
  }

  /**
    * Send an HTTP request and returns a response (or the last response if the request is retried)
    *
    * @param req
    * @return
    */
  def sendSafe(req: Request, context: HttpClientContext = HttpClientContext.empty): Rx[Response] = {
    send(req, context).toRx.recover { case e: HttpClientException =>
      e.response.toHttpResponse
    }
  }

  def readAsInternal[Resp](
      req: Request,
      responseSurface: Surface
  ): Rx[Resp] = {
    send(req).toRx.map { resp =>
      HttpClients.parseResponse[Resp](config, responseSurface, resp)
    }
  }

  def callInternal[Req, Resp](
      req: Request,
      requestSurface: Surface,
      responseSurface: Surface,
      requestContent: Req
  ): Rx[Resp] = {
    Rx
      .const(HttpClients.prepareRequest(config, req, requestSurface, requestContent))
      .flatMap { (newRequest: Request) =>
        send(newRequest, HttpClientContext(config.name)).toRx.map { resp =>
          HttpClients.parseResponse[Resp](config, responseSurface, resp)
        }
      }
  }

  /**
    * @param method
    * @param requestContent
    * @tparam Req
    * @tparam Resp
    * @return
    *
    * @throws RPCException
    *   when RPC request fails
    */
  def rpc[Req, Resp](
      method: RPCMethod,
      requestContent: Req
  ): Rx[Resp] = {
    Rx
      .const(HttpClients.prepareRPCRequest(config, method.path, method.requestSurface, requestContent))
      .flatMap { (request: Request) =>
        val context = HttpClientContext(
          clientName = config.name,
          rpcMethod = Some(method),
          rpcInput = Some(requestContent)
        )
        sendSafe(request, context).toRx
          .map { (response: Response) =>
            if (response.status.isSuccessful) {
              val ret = HttpClients.parseRPCResponse(config, response, method.responseSurface)
              ret.asInstanceOf[Resp]
            } else {
              throw RPCException.fromResponse(response)
            }
          }
      }
  }
}

object HttpClients extends LogSupport {
  private val responseBodyCodec = new HttpResponseBodyCodec[Response]

  private[client] def defaultHttpClientErrorHandler(
      lastResponse: Option[Response]
  ): PartialFunction[Throwable, Nothing] = {
    case e: HttpClientException =>
      val resp = e.response.toHttpResponse
      resp.getHeader(HttpHeader.xAirframeRPCStatus) match {
        case Some(status) =>
          // Throw RPCException if RPCStatus code is given
          val ex = RPCException.fromResponse(e.response.toHttpResponse)
          throw new HttpClientException(resp, ex.status.httpStatus, ex.message, ex)
        case None =>
          // Throw as is for known client exception
          throw e
      }
    case e: CircuitBreakerOpenException =>
      val resp = lastResponse.getOrElse(Http.response(HttpStatus.ServiceUnavailable_503))
      throw new HttpClientException(
        resp,
        status = resp.status,
        message = e.getMessage,
        cause = e
      )
    case e: MaxRetryException =>
      throw HttpClientMaxRetryException(
        lastResponse.getOrElse(Http.response(HttpStatus.InternalServerError_500)),
        e.retryContext,
        e.retryContext.lastError
      )
    case NonFatal(e) =>
      val resp = lastResponse.getOrElse(Http.response(HttpStatus.InternalServerError_500))
      throw new HttpClientException(
        resp,
        status = resp.status,
        message = e.getMessage,
        cause = e
      )
  }

  private[client] def prepareRequest[Req](
      config: HttpClientConfig,
      baseRequest: Request,
      requestSurface: Surface,
      requestBody: Req
  ): Request = {
    try {
      baseRequest.method match {
        case HttpMethod.GET =>
          val newPath = HttpClient.buildResourceUri[Req](baseRequest.path, requestBody, requestSurface)
          baseRequest.withUri(newPath)
        case _ =>
          val requestCodec: MessageCodec[Req] =
            config.codecFactory.ofSurface(requestSurface).asInstanceOf[MessageCodec[Req]]
          val bytes = config.rpcEncoding.encodeWithCodec(requestBody, requestCodec)
          config.rpcEncoding match {
            case RPCEncoding.MsgPack =>
              baseRequest.withMsgPack(bytes)
            case RPCEncoding.JSON =>
              baseRequest.withJson(bytes)
          }
      }
    } catch {
      case e: Throwable =>
        throw new HttpClientException(
          Http.response(HttpStatus.BadRequest_400),
          HttpStatus.BadRequest_400,
          s"Failed to encode the HTTP request body: ${requestBody}",
          e
        )
    }
  }

  private[client] def parseResponse[Resp](
      config: HttpClientConfig,
      responseSurface: Surface,
      resp: Response
  ): Resp = {
    // If the response type is Response, return as is
    if (classOf[Response].isAssignableFrom(responseSurface.rawType)) {
      resp.asInstanceOf[Resp]
    } else {
      try {
        val msgpack = responseBodyCodec.toMsgPack(resp)
        val codec   = config.codecFactory.ofSurface(responseSurface)
        codec match {
          case UnitCodec =>
            null.asInstanceOf[Resp]
          case _ =>
            val responseObject = codec.fromMsgPack(msgpack)
            responseObject.asInstanceOf[Resp]
        }
      } catch {
        case e: Throwable =>
          throw new HttpClientException(
            resp,
            resp.status,
            s"Failed to parse the response from the server: ${resp}: ${e.getMessage}",
            e
          )
      }
    }
  }

  private[http] def prepareRPCRequest(
      config: HttpClientConfig,
      resourcePath: String,
      requestSurface: Surface,
      requestContent: Any
  ): Request = {
    val requestEncoder: MessageCodec[Any] =
      config.codecFactory.ofSurface(requestSurface).asInstanceOf[MessageCodec[Any]]

    try {
      Http
        .POST(resourcePath)
        .withContentType(config.rpcEncoding.applicationType)
        // Encode request body
        .withContent(config.rpcEncoding.encodeWithCodec[Any](requestContent, requestEncoder))
    } catch {
      case e: Throwable =>
        throw RPCStatus.INVALID_ARGUMENT_U2.newException(
          message = s"Failed to encode the RPC request argument ${requestContent}: ${e.getMessage}",
          cause = e
        )
    }
  }

  private[http] def parseRPCResponse(config: HttpClientConfig, response: Response, responseSurface: Surface): Any = {
    if (classOf[Response].isAssignableFrom(responseSurface.rawType)) {
      response
    } else {
      try {
        val msgpack = responseBodyCodec.toMsgPack(response)
        val codec   = config.codecFactory.ofSurface(responseSurface)
        codec match {
          case UnitCodec =>
            null
          case _ =>
            val responseObject = codec.fromMsgPack(msgpack)
            responseObject
        }
      } catch {
        case e: Throwable =>
          throw RPCStatus.DATA_LOSS_I8.newException(
            s"Failed to parse the RPC response from the server ${response}: ${e.getMessage}",
            e
          )
      }
    }
  }

}
