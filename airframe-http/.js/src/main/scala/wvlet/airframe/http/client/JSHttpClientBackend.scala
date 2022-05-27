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

import wvlet.airframe.control.Retry
import wvlet.airframe.http.js.JSHttpClient
import wvlet.airframe.http.{HttpClientConfig, ServerAddress}

object JSHttpClientBackend extends HttpClientBackend {

  override def defaultRequestRetryer: Retry.RetryContext = {
    // Use this for compatibility. We may be able to use HttpClient.defaultHttpClientRetry in future
    JSHttpClient.defaultHttpClientRetryer
  }

  override def newSyncClient(
      severAddress: String,
      clientConfig: HttpClientConfig
  ): SyncClient = {
    throw new UnsupportedOperationException("sync client is not supported in Scala.js")
  }

  override def newAsyncClient(
      serverAddress: String,
      clientConfig: HttpClientConfig
  ): AsyncClient = {
    val address = if (serverAddress.isEmpty) None else Some(ServerAddress(serverAddress))

    // TODO: Use HttpClientConfig in JSHttpClient
    new JSAsyncClient(clientConfig, address)
  }

  override def newRPCClientForScalaJS(clientConfig: HttpClientConfig): RPCHttpClient = {
    val asyncClient = newAsyncClient(JSHttpClient.resolveServerAddress.getOrElse(""), clientConfig)
    new RPCHttpClient(clientConfig, asyncClient)
  }
}
