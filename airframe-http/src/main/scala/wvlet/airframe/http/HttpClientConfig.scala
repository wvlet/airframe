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
import wvlet.airframe.control.CircuitBreaker
import wvlet.airframe.control.Retry.RetryContext
import wvlet.airframe.http.HttpMessage.{Request, Response}
import wvlet.airframe.http.client.{
  AsyncClient,
  AsyncClientImpl,
  ClientFilter,
  HttpClientBackend,
  SyncClient,
  SyncClientImpl
}
import wvlet.airframe.rx.{Rx, RxStream}

import java.util.concurrent.TimeUnit
import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration.Duration

object HttpClientConfig {
  // Tell the IntelliJ that Compat object implements CompatAPI. This a workaround of IntelliJ, which cannot properly highlight cross-build project code:
  // https://youtrack.jetbrains.com/issue/SCL-19567/Support-of-CrossType-Full-wanted
  private def compat: CompatApi = wvlet.airframe.http.Compat
}

import HttpClientConfig._

/**
  */
case class HttpClientConfig(
    backend: HttpClientBackend = compat.defaultHttpClientBackend,
    requestFilter: Request => Request = identity,
    rpcEncoding: RPCEncoding = RPCEncoding.MsgPack,
    retryContext: RetryContext = compat.defaultHttpClientBackend.defaultRequestRetryer,
    codecFactory: MessageCodecFactory = MessageCodecFactory.defaultFactoryForJSON,
    // The default circuit breaker, which will be open after 5 consecutive failures
    circuitBreaker: CircuitBreaker = CircuitBreaker.withConsecutiveFailures(5),
    // timeout applied when establishing HTTP connection to the target host
    connectTimeout: Duration = Duration(90, TimeUnit.SECONDS),
    // timeout applied when receiving data from the target host
    readTimeout: Duration = Duration(90, TimeUnit.SECONDS),
    // Provide a thread executor for managing Scala Future responses
    executionContextProvider: HttpClientConfig => ExecutionContext = { _ => compat.defaultExecutionContext },
    clientFilter: ClientFilter = ClientFilter.identity,
    /**
      * For converting Future[A] to Rx[A]. Use this method when you need to add a common error handler for Rx (e.g.,
      * with Rx.recover). This is mainly used in generated RPC clients for Scala.js
      */
    rxConverter: Future[_] => RxStream[_] = { (f: Future[_]) =>
      // TODO: This execution context needs to reference a global one if we need to use it in Scala JVM
      Rx.future(f)(compat.defaultExecutionContext)
    }
) {
  def newSyncClient(serverAddress: String): SyncClient =
    new SyncClientImpl(backend.newHttpChannel(ServerAddress(serverAddress), this), this)

  def newAsyncClient(serverAddress: String): AsyncClient =
    new AsyncClientImpl(backend.newHttpChannel(ServerAddress(serverAddress), this), this)

  /**
    * Create a default Async client for Scala.js in web browsers
    */
  def newJSClient: AsyncClient = {
    new AsyncClientImpl(backend.newHttpChannel(compat.hostServerAddress, this), this)
  }

  def withBackend(newBackend: HttpClientBackend): HttpClientConfig =
    this.copy(backend = newBackend)

  /**
    * Add a custom request filter
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

  def withExecutionContextProvider(provider: HttpClientConfig => ExecutionContext): HttpClientConfig = {
    this.copy(executionContextProvider = provider)
  }

  def newExecutionContext: ExecutionContext = executionContextProvider(this)

  def withConnectTimeout(duration: Duration): HttpClientConfig = {
    this.copy(connectTimeout = duration)
  }
  def withReadTimeout(duration: Duration): HttpClientConfig = {
    this.copy(readTimeout = duration)
  }

  /**
    * Add a new client filter
    * @param filter
    * @return
    */
  def withClientFilter(filter: ClientFilter): HttpClientConfig = {
    this.copy(clientFilter = clientFilter.andThen(filter))
  }
  def noClientFilter: HttpClientConfig = this.copy(clientFilter = ClientFilter.identity)

  /**
    * Set a converter from Future[A] to Rx[A]
    */
  def withRxConverter(f: Future[_] => RxStream[_]): HttpClientConfig = {
    this.copy(rxConverter = f)
  }
}
