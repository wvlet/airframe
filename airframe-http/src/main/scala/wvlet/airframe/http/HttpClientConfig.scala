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
import wvlet.airframe.codec.MessageCodecFactory
import wvlet.airframe.control.Retry.RetryContext
import wvlet.airframe.http.HttpMessage.{Request, Response}

/**
  */
case class HttpClientConfig(
    backend: HttpClientBackend = Compat.defaultHttpClientBackend,
    requestFilter: Request => Request = identity,
    retryContext: RetryContext = HttpClient.defaultHttpClientRetry[Request, Response],
    codecFactory: MessageCodecFactory = MessageCodecFactory.defaultFactoryForJSON
) {
  def newSyncClient(serverAddress: String): HttpSyncClient[Request, Response] =
    backend.newSyncClient(serverAddress, this)

  def withBackend(newBackend: HttpClientBackend): HttpClientConfig =
    this.copy(backend = newBackend)
  def withRequestFilter(newRequestFilter: Request => Request): HttpClientConfig =
    this.copy(requestFilter = newRequestFilter)
  def withCodecFactory(newCodecFactory: MessageCodecFactory): HttpClientConfig =
    this.copy(codecFactory = newCodecFactory)
  def withRetryContext(filter: RetryContext => RetryContext): HttpClientConfig =
    this.copy(retryContext = filter(retryContext))
}
