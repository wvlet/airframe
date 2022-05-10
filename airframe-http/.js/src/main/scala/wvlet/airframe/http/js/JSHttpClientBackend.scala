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

import wvlet.airframe.control.Retry
import wvlet.airframe.http.js.JSHttpClient.MessageEncoding
import wvlet.airframe.http.{
  HttpClient,
  HttpClientBackend,
  HttpClientConfig,
  HttpMessage,
  HttpSyncClient,
  RPCHttpClient,
  RPCEncoding,
  ServerAddress
}

import scala.concurrent.{ExecutionContext, Future}

object JSHttpClientBackend extends HttpClientBackend {

  override def defaultExecutionContext: ExecutionContext = {
    scala.scalajs.concurrent.JSExecutionContext.queue
  }
  override def defaultRequestRetryer: Retry.RetryContext = {
    // Use this for compatibility. We may be able to use HttpClient.defaultHttpClientRetry in future
    JSHttpClient.defaultHttpClientRetryer
  }

  override def newSyncClient(
      severAddress: String,
      clientConfig: HttpClientConfig
  ): HttpSyncClient[HttpMessage.Request, HttpMessage.Response] = {
    throw new UnsupportedOperationException("sync client is not supported in Scala.js")
  }

  override def newAsyncClient(
      serverAddress: String,
      clientConfig: HttpClientConfig
  ): HttpClient[Future, HttpMessage.Request, HttpMessage.Response] = {
    val address = if (serverAddress.isEmpty) None else Some(ServerAddress(serverAddress))

    // TODO: Use HttpClientConfig in JSHttpClient
    val config = JSHttpClientConfig(serverAddress = address)
      .withRequestEncoding(
        if (clientConfig.rpcEncoding == RPCEncoding.JSON) MessageEncoding.JsonEncoding
        else MessageEncoding.MessagePackEncoding
      )
      .withRequestFilter(clientConfig.requestFilter)
      .withRetry(r => clientConfig.retryContext)
      .withCodecFactory(clientConfig.codecFactory)
      .withCircuitBreaker(c => clientConfig.circuitBreaker)
      .withRxConverter(clientConfig.rxConverter)

    new JSHttpClientAdaptor(JSHttpClient(config))
  }

  override def newRPCClientForScalaJS(clientConfig: HttpClientConfig): RPCHttpClient = {
    val asyncClient = newAsyncClient(JSHttpClient.resolveServerAddress.getOrElse(""), clientConfig)
    new RPCHttpClient(clientConfig, asyncClient)
  }
}
