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
import wvlet.airframe.rx.{Rx, RxStream}

import scala.concurrent.Future

/**
  */
case class HttpClientConfig(
    backend: HttpClientBackend = Compat.defaultHttpClientBackend,
    requestFilter: Request => Request = identity,
    rpcEncoding: RPCEncoding = RPCEncoding.MsgPack,
    retryContext: RetryContext = Compat.defaultHttpClientBackend.defaultRequestRetryer,
    codecFactory: MessageCodecFactory = MessageCodecFactory.defaultFactoryForJSON,
    // The default circuit breaker, which will be open after 5 consecutive failures
    circuitBreaker: CircuitBreaker = CircuitBreaker.withConsecutiveFailures(5),
    /**
      * For converting Future[A] to Rx[A]. Use this method when you need to add a common error handler for Rx (e.g.,
      * with Rx.recover)
      */
    rxConverter: Future[_] => RxStream[_] = { (f: Future[_]) =>
      Rx.future(f)(Compat.defaultHttpClientBackend.defaultExecutionContext)
    }
) {
  def newSyncClient(serverAddress: String): Http.SyncClient =
    backend.newSyncClient(serverAddress, this)

  def newAsyncClient(serverAddress: String): Http.AsyncClient =
    backend.newAsyncClient(serverAddress, this)

  def newRPCSyncClient(serverAddress: String): RPCHttpSyncClient = {
    new RPCHttpSyncClient(this, newSyncClient(serverAddress))
  }

  def newRPCClientForScalaJS: RPCHttpClient = {
    backend.newRPCClientForScalaJS(this)
  }

  def withBackend(newBackend: HttpClientBackend): HttpClientConfig =
    this.copy(backend = newBackend)
  def withRequestFilter(newRequestFilter: Request => Request): HttpClientConfig =
    this.copy(requestFilter = newRequestFilter)

  def withRPCEncoding(newEncoding: RPCEncoding): HttpClientConfig = {
    this.copy(rpcEncoding = newEncoding)
  }
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

  /**
    * Set a converter from Future[A] to Rx[A]
    */
  def withRxConverter(f: Future[_] => RxStream[_]): HttpClientConfig = {
    this.copy(rxConverter = f)
  }
}
