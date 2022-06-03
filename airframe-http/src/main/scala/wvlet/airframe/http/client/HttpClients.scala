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
import wvlet.airframe.control.Retry.RetryContext
import wvlet.airframe.http.HttpMessage.{Request, Response}
import wvlet.airframe.http._
import wvlet.airframe.surface.Surface

import scala.concurrent.duration.Duration
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try


trait HttpChannel extends AutoCloseable {
  def send(req: Request): Response
}

trait HttpClientBase[Impl] {

  protected def config: HttpClientConfig

  protected def newClient(config:HttpClientConfig): Impl

  def withRequestFilter(requestFilter: Request => Request): Impl = {
    newClient(config.withRequestFilter(requestFilter))
  }
  def withClientFilter(filter: ClientFilter): Impl = {
    newClient(config.withClientFilter(filter))
  }
  def withRetryContext(filter: RetryContext => RetryContext): Impl = {
    newClient(config.withRetryContext(filter))
  }
  def withConfig(filter: HttpClientConfig => HttpClientConfig): Impl = {
    newClient(filter(config))
  }
  def withConnectTimeout(duration: Duration): Impl = {
    newClient(config.withConnectTimeout(duration))
  }
  def withReadTimeout(duration: Duration): Impl = {
    newClient(config.withReadTimeout(duration))
  }
}

/**
  * A standard blocking http client interface
  */
trait SyncClient extends SyncClientCompat with AutoCloseable {

  private[client] def config: HttpClientConfig

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
  def send(req: Request): Response

  /**
    * Send an HTTP request and returns a response (or the last response if the request is retried). Unlike [[send()]],
    * this method returns a regular Http Response object even for non-retryable responses (e.g., 4xx error code). For
    * retryable responses (e.g., 5xx) this continues retry until the max retry count.
    *
    * After reaching the max retry count, it will return a the last response even for 5xx status code.
    */
  def sendSafe(req: Request): Response = {
    try {
      send(req)
    } catch {
      case e: HttpClientException =>
        e.response.toHttpResponse
    }
  }

  def readAsInternal[Resp](
      req: Request,
      responseSurface: Surface
  ): Resp = {
    val resp: Response = send(req)
    HttpClients.parseResponse[Resp](config, responseSurface, resp)
  }

  def callInternal[Req, Resp](
      req: Request,
      requestSurface: Surface,
      responseSurface: Surface,
      requestContent: Req
  ): Resp = {
    val newRequest     = HttpClients.prepareRequest(config, req, requestSurface, requestContent)
    val resp: Response = send(newRequest)
    HttpClients.parseResponse[Resp](config, responseSurface, resp)
  }

  /**
    * Send an RPC request (POST) and return the RPC response. This method will throw RPCException when an error happens
    *
    * @param resourcePath
    * @param requestSurface
    * @param requestContent
    * @param responseSurface
    * @param requestFilter
    * @tparam Req
    *   request type
    * @return
    *   response
    *
    * @throws RPCException
    */
  def sendRPC[Req](
      resourcePath: String,
      requestSurface: Surface,
      requestContent: Req,
      responseSurface: Surface
  ): Any = {
    val request: Request = HttpClients.prepareRPCRequest(config, resourcePath, requestSurface, requestContent)

    // sendSafe method internally handles retries and HttpClientException, and then it returns the last response
    val response: Response = sendSafe(request)

    // f Parse the RPC response
    if (response.status.isSuccessful) {
      HttpClients.parseRPCResponse(config, response, responseSurface)
    } else {
      // Parse the RPC error message
      throw HttpClients.parseRPCException(response)
    }
  }

}

/**
  * A standard async http client interface for Scala Future
  */
trait AsyncClient extends AsyncClientCompat with AutoCloseable {
  private[client] def config: HttpClientConfig
  private[client] implicit val executionContext: ExecutionContext

  /**
    * Send an HTTP request and get the response in Scala Future type.
    *
    * It will return `Future[HttpClientException]` for non-successful responses. For example, when receiving
    * non-retryable status code (e.g., 4xx), it will return Future[HttpClientException]. For server side failures (5xx
    * responses), this continues request retry until the max retry count.
    *
    * If it exceeds the number of max retry attempts, it will return Future[HttpClientMaxRetryException].
    */
  def send(req: Request): Future[Response]

  /**
    * Send an HTTP request and returns a response (or the last response if the request is retried)
    *
    * @param req
    * @return
    */
  def sendSafe(req: Request): Future[Response]

  def readAsInternal[Resp](
      req: Request,
      responseSurface: Surface
  ): Future[Resp] = {
    send(req).map { resp =>
      HttpClients.parseResponse[Resp](config, responseSurface, resp)
    }
  }

  def callInternal[Req, Resp](
      req: Request,
      requestSurface: Surface,
      responseSurface: Surface,
      requestContent: Req
  ): Future[Resp] = {
    Future
      .apply {
        HttpClients.prepareRequest(config, req, requestSurface, requestContent)
      }
      .flatMap { newRequest =>
        send(newRequest).map { resp =>
          HttpClients.parseResponse[Resp](config, responseSurface, resp)
        }
      }
  }

  def sendRPC[Req](
      resourcePath: String,
      requestSurface: Surface,
      requestContent: Req,
      responseSurface: Surface
  ): Future[Any] = {
    Future {
      val request: Request = HttpClients.prepareRPCRequest(config, resourcePath, requestSurface, requestContent)
      request
    }.flatMap { (request: Request) =>
      sendSafe(request)
        .map { (response: Response) =>
          if (response.status.isSuccessful) {
            HttpClients.parseRPCResponse(config, response, responseSurface)
          } else {
            throw HttpClients.parseRPCException(response)
          }
        }
    }
  }
}

object HttpClients {
  private val responseBodyCodec = new HttpResponseBodyCodec[Response]

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
        val msgpack        = responseBodyCodec.toMsgPack(resp)
        val codec          = config.codecFactory.ofSurface(responseSurface)
        val responseObject = codec.fromMsgPack(msgpack)
        responseObject.asInstanceOf[Resp]
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
        val msgpack        = responseBodyCodec.toMsgPack(response)
        val codec          = config.codecFactory.ofSurface(responseSurface)
        val responseObject = codec.fromMsgPack(msgpack)
        responseObject
      } catch {
        case e: Throwable =>
          throw RPCStatus.DATA_LOSS_I8.newException(
            s"Failed to parse the RPC response from the server ${response}: ${e.getMessage}",
            e
          )
      }
    }
  }

  private[http] def parseRPCException(response: Response): RPCException = {
    response
      .getHeader(HttpHeader.xAirframeRPCStatus)
      .flatMap(x => Try(x.toInt).toOption) match {
      case Some(rpcStatus) =>
        try {
          val msgpack = responseBodyCodec.toMsgPack(response)
          RPCException.fromMsgPack(msgpack)
        } catch {
          case e: Throwable =>
            RPCStatus.ofCode(rpcStatus).newException(s"Failed to parse the RPC error details: ${e.getMessage}", e)
        }
      case None =>
        RPCStatus.DATA_LOSS_I8.newException(s"Invalid RPC response: ${response}")
    }
  }

}
