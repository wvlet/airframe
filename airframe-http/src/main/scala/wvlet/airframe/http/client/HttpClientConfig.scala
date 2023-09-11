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
import wvlet.airframe.http.HttpMessage.{Request, Response}
import wvlet.airframe.http.*

import java.util.concurrent.TimeUnit
import scala.collection.immutable.ListMap
import scala.concurrent.duration.Duration

/**
  * A common immutable configuration for all HTTP clients in airframe-http. To modify any configuration, use withXXX
  * methods.
  *
  * The generated HTTP client has multiple layers of filters:
  *   - requestFilter: A filter to modify the request before sending it to the backend. This can be used for adding
  *     common HTTP headers (e.g., User-Agent, Authentication header, etc.)
  *   - clientFilter: A filter to modify the request/response.
  *   - loggingFilter: A filter to log individual requests and responses, including retried requests. The default
  *     behavior is logging each request with its response stats to log/http_client.json file.
  *   - responseFilter: A filter to modify the response before returning it to the caller.
  */
case class HttpClientConfig(
    name: String = "default",
    backend: HttpClientBackend = Compat.defaultHttpClientBackend,
    requestFilter: Request => Request = identity,
    responseFilter: Response => Response = identity,
    rpcEncoding: RPCEncoding = RPCEncoding.JSON,
    retryContext: RetryContext = Compat.defaultHttpClientBackend.defaultRequestRetryer,
    codecFactory: MessageCodecFactory = MessageCodecFactory.defaultFactoryForJSON,
    // The default circuit breaker, which will be open after 5 consecutive failures
    circuitBreaker: CircuitBreaker = CircuitBreaker.withConsecutiveFailures(5),
    // timeout applied when establishing HTTP connection to the target host
    connectTimeout: Duration = Duration(90, TimeUnit.SECONDS),
    // timeout applied when receiving data from the target host
    readTimeout: Duration = Duration(90, TimeUnit.SECONDS),
    clientFilter: RxHttpFilter = RxHttpFilter.identity,
    httpLoggerConfig: HttpLoggerConfig = HttpLoggerConfig(logFileName = "log/http_client.json"),
    httpLoggerProvider: HttpLoggerConfig => HttpLogger = Compat.defaultHttpClientLoggerFactory,
    loggingFilter: HttpLogger => HttpClientFilter = { (l: HttpLogger) => new HttpClientLoggingFilter(l) }
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

  /**
    * Add a custom response filter to the last response, mostly for debugging purpose
    * @param newResponseFilter
    * @return
    */
  def withResponseFilter(newResponseFilter: Response => Response): HttpClientConfig =
    this.copy(responseFilter = newResponseFilter)
  def noResponseFilter: HttpClientConfig = this.copy(responseFilter = identity)

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
    * Add a new RxClientFilter. This filter is useful for adding a common error handling logic for the Rx[Response].
    *
    * @param filter
    * @return
    */
  def withClientFilter(filter: RxHttpFilter): HttpClientConfig = {
    this.copy(clientFilter = clientFilter.andThen(filter))
  }

  /**
    * Remove any client-side filter
    */
  def noClientFilter: HttpClientConfig = this.copy(clientFilter = RxHttpFilter.identity)

  /**
    * Customize logger configuration
    * @param f
    * @return
    */
  def withLoggerConfig(f: HttpLoggerConfig => HttpLoggerConfig): HttpClientConfig = {
    this.copy(httpLoggerConfig = f(httpLoggerConfig))
  }

  /**
    * Disable http-client side logging
    * @return
    */
  def noLogging: HttpClientConfig = {
    this.copy(
      httpLoggerProvider = { HttpLogger.emptyLogger(_) },
      loggingFilter = { _ => HttpClientFilter.identity }
    )
  }

  /**
    * Use Debug Console http logging
    */
  def withDebugConsoleLogger: HttpClientConfig = {
    this.copy(
      httpLoggerProvider = { _.consoleLogger }
    )
  }

  def withHttpLogger(loggerProvider: HttpLoggerConfig => HttpLogger): HttpClientConfig = {
    this.copy(httpLoggerProvider = loggerProvider)
  }

  def newHttpLogger: HttpLogger = {
    httpLoggerProvider(httpLoggerConfig.addExtraTags(ListMap("client_name" -> name)))
  }

  def newLoggingFilter(logger: HttpLogger): HttpClientFilter = {
    loggingFilter(logger)
  }
}
