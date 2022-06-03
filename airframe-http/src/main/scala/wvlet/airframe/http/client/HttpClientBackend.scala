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

import wvlet.airframe.control.Retry.RetryContext
import wvlet.airframe.http.HttpMessage.{Request, Response}
import wvlet.airframe.http.{HttpClient, HttpClientConfig, ServerAddress}

/**
  */
trait HttpClientBackend {
  def defaultRequestRetryer: RetryContext = HttpClient.defaultHttpClientRetry[Request, Response]

  def newHttpChannel(serverAddress: ServerAddress, config: HttpClientConfig): HttpChannel

  def newSyncClient(serverAddress: ServerAddress, clientConfig: HttpClientConfig): SyncClient = {
    new SyncClientImpl(newHttpChannel(serverAddress, clientConfig), clientConfig)
  }

  def newAsyncClient(
      serverAddress: ServerAddress,
      clientConfig: HttpClientConfig
  ): AsyncClient = {
    new AsyncClientImpl(newHttpChannel(serverAddress, clientConfig), clientConfig)
  }
}
