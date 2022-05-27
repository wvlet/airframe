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
import wvlet.airframe.http.HttpMessage.{Request, Response}
import wvlet.airframe.http._
import wvlet.airframe.surface.Surface

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

/**
  * A standard blocking http client interface
  */
trait SyncClient extends RPCSyncClientBase with AutoCloseable {

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
  def send(req: Request, requestFilter: Request => Request = identity): Response

  /**
    * Send an HTTP request and returns a response (or the last response if the request is retried). Unlike [[send()]],
    * this method returns a regular Http Response object even for non-retryable responses (e.g., 4xx error code). For
    * retryable responses (e.g., 5xx) this continues retry until the max retry count.
    *
    * After reaching the max retry count, it will return a the last response even for 5xx status code.
    */
  def sendSafe(req: Request, requestFilter: Request => Request = identity): Response = {
    try {
      send(req, requestFilter)
    } catch {
      case e: HttpClientException =>
        e.response.toHttpResponse
    }
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
      responseSurface: Surface,
      requestFilter: Request => Request = identity
  ): Any = {
    val request: Request = HttpClients.prepareRPCRequest(config, resourcePath, requestSurface, requestContent)

    // sendSafe method internally handles retries and HttpClientException, and then it returns the last response
    val response: Response = sendSafe(request, requestFilter)

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
trait AsyncClient extends RPCAsyncClientBase with AutoCloseable {
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
  def send(req: Request, requestFilter: Request => Request = identity): Future[Response]

  /**
    * Send an HTTP request and returns a response (or the last response if the request is retried)
    *
    * @param req
    * @param requestFilter
    * @return
    */
  def sendSafe(req: Request, requestFilter: Request => Request = identity): Future[Response]

  def sendRPC[Req](
      resourcePath: String,
      requestSurface: Surface,
      requestContent: Req,
      responseSurface: Surface,
      requestFilter: Request => Request = identity
  ): Future[Any] = {
    Future {
      val request: Request = HttpClients.prepareRPCRequest(config, resourcePath, requestSurface, requestContent)
      request
    }.flatMap { (request: Request) =>
      sendSafe(request, config.requestFilter.andThen(requestFilter))
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
