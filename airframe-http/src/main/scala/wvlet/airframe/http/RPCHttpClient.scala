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
package wvlet.airframe.http

import wvlet.airframe.codec.{MessageCodec, MessageCodecException}
import wvlet.airframe.http.HttpMessage.{Request, Response}
import wvlet.airframe.surface.Surface
import wvlet.log.LogSupport

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

/**
  * A scala Future-based RPC http client implementation, which is mainly for supporting Scala.js
  * @param config
  * @param httpClient
  */
class RPCHttpClient(config: HttpClientConfig, httpClient: Http.AsyncClient)
    extends RPCClientBase
    with AutoCloseable
    with LogSupport {

  override def close(): Unit = {
    httpClient.close()
  }

  private implicit val ec: ExecutionContext = config.executionContextProvider()

  /**
    * Send an RPC request (POST) and return the RPC response. If any failure happens, it will return
    * Future[RPCException]
    */
  def sendRaw(
      resourcePath: String,
      requestSurface: Surface,
      requestContent: Any,
      responseSurface: Surface,
      requestFilter: Request => Request = identity
  ): Future[Any] = {

    Future {
      val request: Request = RPCHttpClient.prepareRPCRequest(config, resourcePath, requestSurface, requestContent)
      request
    }.flatMap { (request: Request) =>
      httpClient
        .sendSafe(request, config.requestFilter.andThen(requestFilter))
        .map { (response: Response) =>
          if (response.status.isSuccessful) {
            RPCHttpClient.parseResponse(config, response, responseSurface)
          } else {
            throw RPCHttpClient.parseRPCException(response)
          }
        }
    }
  }

}

/**
  * HTTP client implementation base for RPC
  * @param config
  * @param httpSyncClient
  */
class RPCHttpSyncClient(config: HttpClientConfig, httpSyncClient: Http.SyncClient)
    extends RPCSyncClientBase
    with AutoCloseable {

  override def close(): Unit = {
    httpSyncClient.close()
  }

  /**
    * Send an RPC request (POST) and return the RPC response. This method will throw RPCException when an error happens
    */
  def sendRaw(
      resourcePath: String,
      requestSurface: Surface,
      requestContent: Any,
      responseSurface: Surface,
      requestFilter: Request => Request = identity
  ): Any = {
    val request: Request = RPCHttpClient.prepareRPCRequest(config, resourcePath, requestSurface, requestContent)

    // sendSafe method internally handles retries and HttpClientException, and then it returns the last response
    val response: Response = httpSyncClient.sendSafe(request, config.requestFilter.andThen(requestFilter))

    // f Parse the RPC response
    if (response.status.isSuccessful) {
      RPCHttpClient.parseResponse(config, response, responseSurface)
    } else {
      // Parse the RPC error message
      throw RPCHttpClient.parseRPCException(response)
    }
  }

}

object RPCHttpClient extends LogSupport {
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

  private[http] def parseResponse(config: HttpClientConfig, response: Response, responseSurface: Surface): Any = {
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
