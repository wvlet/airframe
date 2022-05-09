/*
 * Licensed under the Apache License, Version 2.0 (the vLicense");
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

import wvlet.airframe.codec.{MessageCodec, MessageCodecException, MessageCodecFactory}
import wvlet.airframe.control.Retry.RetryContext
import wvlet.airframe.http.HttpMessage.{Request, Response}
import wvlet.airframe.surface.Surface

import scala.util.Try

/**
  * Configuration for RPC clients
  * @param requestFilter
  * @param retryContext
  * @param codecFactory
  * @param rpcEncoding
  */
case class RPCClientConfig(
    requestFilter: HttpMessage.Request => HttpMessage.Request = identity,
    retryContext: RetryContext = HttpClient.defaultHttpClientRetry[Request, Response],
    codecFactory: MessageCodecFactory = MessageCodecFactory.defaultFactoryForJSON,
    rpcEncoding: RPCEncoding = RPCEncoding.MsgPack
)

/**
  * RPC client implementation base
  * @param config
  * @param httpSyncClient
  */
class RPCSyncClient(config: RPCClientConfig, httpSyncClient: Http.SyncClient) extends AutoCloseable {

  override def close(): Unit = {
    httpSyncClient.close()
  }

  def send(
      path: String,
      requestSurface: Surface,
      requestContent: Any,
      responseSurface: Surface,
      requestFilter: Request => Request = identity
  ): Any = {
    val requestEncoder: MessageCodec[Any] =
      config.codecFactory.ofSurface(requestSurface).asInstanceOf[MessageCodec[Any]]

    val request: Request =
      try {
        Http
          .POST(path)
          .withContentType(config.rpcEncoding.applicationType)
          .withContent(config.rpcEncoding.encodeWithCodec[Any](requestContent, requestEncoder))
      } catch {
        case e: MessageCodecException =>
          throw RPCStatus.INVALID_ARGUMENT_U2.newException(
            message = s"Failed to encode RPC request arguments: ${requestContent}",
            cause = e
          )
      }

    // sendSafe handles HttpClientException and return the response
    val response: Response = httpSyncClient.sendSafe(request, config.requestFilter.andThen(requestFilter))

    // Parse the RPC response
    if (response.status.isSuccessful) {
      parseResponse(response, responseSurface)
    } else {
      // Parse the RPC error message
      val ex = parseRPCException(response)
      throw ex
    }
  }

  private val standardResponseCodec = new HttpResponseCodec[Response]

  private def parseResponse(response: Response, responseSurface: Surface): Any = {
    if (classOf[Response].isAssignableFrom(responseSurface.rawType)) {
      response
    } else {
      try {
        val msgpack        = standardResponseCodec.toMsgPack(response)
        val codec          = config.codecFactory.ofSurface(responseSurface)
        val responseObject = codec.fromMsgPack(msgpack)
        responseObject
      } catch {
        case e: MessageCodecException =>
          throw RPCStatus.DATA_LOSS_I8.newException(s"Failed to parse the RPC response from the server: ${response}", e)
      }
    }
  }

  private def parseRPCException(response: Response): RPCException = {
    response
      .getHeader(HttpHeader.xAirframeRPCStatus)
      .flatMap(x => Try(x.toInt).toOption) match {
      case Some(rpcStatus) =>
        try {
          if (response.isContentTypeJson) {
            RPCException.fromJson(response.contentString)
          } else {
            RPCException.fromMsgPack(response.contentBytes)
          }
        } catch {
          case e: MessageCodecException =>
            RPCStatus.ofCode(rpcStatus).newException(s"N/A", e)
        }
      case None =>
        RPCStatus.DATA_LOSS_I8.newException(s"Invalid RPC response: ${response}")
    }
  }
}
