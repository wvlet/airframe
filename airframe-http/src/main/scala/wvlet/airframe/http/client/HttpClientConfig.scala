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

import wvlet.airframe.codec.MessageCodecFactory
import wvlet.airframe.control.CircuitBreaker
import wvlet.airframe.control.Retry.RetryContext
import wvlet.airframe.http.HttpMessage.Request
import wvlet.airframe.http.{Compat, RPCEncoding, RxHttpFilter, ServerAddress}
import wvlet.airframe.rx.{Rx, RxStream}

import java.util.concurrent.TimeUnit
import scala.concurrent.Future
import scala.concurrent.duration.Duration

/**
  */
case class HttpClientConfig(
    name: String = "airframe-http-client",
    backend: HttpClientBackend = Compat.defaultHttpClientBackend,
    requestFilter: Request => Request = identity,
    rpcEncoding: RPCEncoding = RPCEncoding.JSON,
    retryContext: RetryContext = Compat.defaultHttpClientBackend.defaultRequestRetryer,
    codecFactory: MessageCodecFactory = MessageCodecFactory.defaultFactoryForJSON,
    // The default circuit breaker, which will be open after 5 consecutive failures
    circuitBreaker: CircuitBreaker = CircuitBreaker.withConsecutiveFailures(5),
    // timeout applied when establishing HTTP connection to the target host
    connectTimeout: Duration = Duration(90, TimeUnit.SECONDS),
    // timeout applied when receiving data from the target host
    readTimeout: Duration = Duration(90, TimeUnit.SECONDS),
    rxHttpFilter: RxHttpFilter = RxHttpFilter.identity,
    /**
      * For converting Future[A] to Rx[A]. Use this method when you need to add a common error handler for Rx (e.g.,
      * with Rx.recover). This is mainly used in generated RPC clients for Scala.js
      */
    @deprecated("Use rxFilter instead", "23.5.0")
    rxConverter: Future[_] => RxStream[_] = { (f: Future[_]) =>
      // TODO: This execution context needs to reference a global one if we need to use it in Scala JVM
      Rx.future(f)(Compat.defaultExecutionContext)
    }
) extends HttpChannelConfig {

  def newSyncClient(serverAddress: String): SyncClient =
    backend.newSyncClient(ServerAddress(serverAddress), this)

  def newAsyncClient(serverAddress: String): AsyncClient =
    backend.newAsyncClient(ServerAddress(serverAddress), this)

  /**
    * Create a default Async client for Scala.js in web browsers
    */
  def newJSClient: AsyncClient =
    backend.newAsyncClient(Compat.hostServerAddress, this)

  def withName(name: String): HttpClientConfig = {
    this.copy(name = name)
  }

  def withBackend(newBackend: HttpClientBackend): HttpClientConfig =
    this.copy(backend = newBackend)

  /**
    * Add a custom request filter, e.g., for adding Authentication headers
    * @param newRequestFilter
    * @return
    */
  def withRequestFilter(newRequestFilter: Request => Request): HttpClientConfig =
    this.copy(requestFilter = requestFilter.andThen(newRequestFilter))

  def noRequestFilter: HttpClientConfig = this.copy(requestFilter = identity)

  def withRPCEncoding(newEncoding: RPCEncoding): HttpClientConfig = {
    this.copy(rpcEncoding = newEncoding)
  }
  def withJSONEncoding: HttpClientConfig    = withRPCEncoding(RPCEncoding.JSON)
  def withMsgPackEncoding: HttpClientConfig = withRPCEncoding(RPCEncoding.MsgPack)

  def withCodecFactory(newCodecFactory: MessageCodecFactory): HttpClientConfig =
    this.copy(codecFactory = newCodecFactory)
  def withRetryContext(filter: RetryContext => RetryContext): HttpClientConfig =
    this.copy(retryContext = filter(retryContext))
  def withCircuitBreaker(f: CircuitBreaker => CircuitBreaker): HttpClientConfig = {
    this.copy(circuitBreaker = f(circuitBreaker))
  }
  def noCircuitBreaker: HttpClientConfig = {
    this.copy(circuitBreaker = CircuitBreaker.alwaysClosed)
  }
  def withConnectTimeout(duration: Duration): HttpClientConfig = {
    this.copy(connectTimeout = duration)
  }
  def withReadTimeout(duration: Duration): HttpClientConfig = {
    this.copy(readTimeout = duration)
  }

  /**
    * Add a new RxHttpFilter. This filter is useful for adding a common error handling logic for the Rx[Response].
    * @param filter
    * @return
    */
  def withRxHttpFilter(filter: RxHttpFilter): HttpClientConfig = {
    this.copy(rxHttpFilter = rxHttpFilter.andThen(filter))
  }
  def noRxHttpFilter: HttpClientConfig = this.copy(rxHttpFilter = RxHttpFilter.identity)

  /**
    * Set a converter from Future[A] to Rx[A]
    */
  @deprecated("Use withRxHttpFilter instead", "23.5.0")
  def withRxConverter(f: Future[_] => RxStream[_]): HttpClientConfig = {
    this.copy(rxConverter = f)
  }
}
