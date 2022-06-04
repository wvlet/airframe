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
import wvlet.airframe.http.HttpMessage.{Request, Response}
import wvlet.airframe.http.{HttpClient, HttpClientConfig, HttpClientException, ServerAddress}

object JSHttpClientBackend extends HttpClientBackend {

  override def defaultRequestRetryer: Retry.RetryContext = {
    HttpClient
      .baseHttpClientRetry[Request, Response]
      // defaultHttpClientRetry has many JVM specific exception classifiers,
      // so we need to use a simple one for Scala.js
      .withErrorClassifier(HttpClientException.classifyExecutionFailureScalaJS)
  }

  override def newHttpChannel(serverAddress: ServerAddress, config: HttpClientConfig): HttpChannel = {
    new JSClientChannel(serverAddress, config)
  }

  override def newSyncClient(
      severAddress: ServerAddress,
      clientConfig: HttpClientConfig
  ): SyncClient = {
    throw new UnsupportedOperationException("sync client is not supported in Scala.js")
  }
}
