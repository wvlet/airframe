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

import wvlet.airframe.codec.{MessageCodec, MessageCodecFactory}
import wvlet.airframe.control.Retry.RetryContext
import wvlet.airframe.http.HttpMessage.{Request, Response}
import wvlet.airframe.surface.Surface

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

  def sendRequest(
      path: String,
      requestSurface: Surface,
      requestContent: Any,
      requestFilter: Request => Request = identity
  ): HttpMessage.Response = {

    val requestEncoder: MessageCodec[Any] =
      config.codecFactory.ofSurface(requestSurface).asInstanceOf[MessageCodec[Any]]

    val request: Request =
      Http
        .POST(path)
        .withContentType(config.rpcEncoding.applicationType)
        .withContent(config.rpcEncoding.encodeWithCodec[Any](requestContent, requestEncoder))

    val response = httpSyncClient.sendSafe(request, config.requestFilter.andThen(requestFilter))
    response
  }

}
