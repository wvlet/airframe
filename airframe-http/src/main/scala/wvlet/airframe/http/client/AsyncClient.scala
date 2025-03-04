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

import wvlet.airframe.control.CircuitBreaker
import wvlet.airframe.http.HttpMessage.{Request, Response}
import wvlet.airframe.http.{HttpClientException, HttpLogger, RPCException, RPCMethod}
import wvlet.airframe.rx.Rx
import wvlet.airframe.surface.Surface
import wvlet.log.LogSupport

import java.util.concurrent.atomic.AtomicReference

/**
  * A standard async http client interface for Rx[_]
  */
trait AsyncClient extends AsyncClientCompat with HttpClientFactory[AsyncClient] with AutoCloseable with LogSupport {
  protected def channel: HttpChannel
  def config: HttpClientConfig

  private val httpLogger: HttpLogger          = config.newHttpLogger(channel.destination)
  private val loggingFilter: HttpClientFilter = config.newLoggingFilter(httpLogger)
  private val circuitBreaker: CircuitBreaker  = config.circuitBreaker

  override def close(): Unit = {
    httpLogger.close()
  }

  /**
    * Send an HTTP request and get the response in Rx[Response] type.
    *
    * It will return `Rx[HttpClientException]` for non-successful responses. For example, when receiving non-retryable
    * status code (e.g., 4xx), it will return Rx[HttpClientException]. For server side failures (5xx responses), this
    * continues request retry until the max retry count.
    *
    * If it exceeds the number of max retry attempts, it will return Rx[HttpClientMaxRetryException].
    */
  def send(req: Request, context: HttpClientContext = HttpClientContext.empty): Rx[Response] = {
    val request                        = config.requestFilter(req)
    var lastResponse: Option[Response] = None
    // Build a chain of request filters
    def requestPipeline =
      loggingFilter(context.withClientName(config.name))
        .andThen { req =>
          channel
            .sendAsync(req, config)
            .tap { resp =>
              // Remember the last response for the error reporting purpose
              lastResponse = Some(resp)
            }
        }

    val rx =
      // Apply the client filter first to handle only the last response
      config.clientFilter.andThen { req =>
        // Wrap http request with the default error retry handler
        config.retryContext
          .runAsyncWithContext(req, circuitBreaker) {
            requestPipeline(req)
          }
          .map { resp =>
            // Apply the response filter for the successful response
            config.responseFilter(resp)
          }
          .recover {
            // Or if request has been failing, apply the response filter only to the last response
            HttpClients.defaultHttpClientErrorHandler(() => lastResponse.map(config.responseFilter(_)))
          }
      }

    // Run the filter chain
    rx.apply(request)
  }

  /**
    * Send an HTTP request and returns a response (or the last response if the request is retried)
    *
    * @param req
    * @return
    */
  def sendSafe(req: Request, context: HttpClientContext = HttpClientContext.empty): Rx[Response] = {
    send(req, context).toRx.recover { case e: HttpClientException =>
      e.response.toHttpResponse
    }
  }

  def readAsInternal[Resp](
      req: Request,
      responseSurface: Surface,
      context: HttpClientContext = HttpClientContext.empty
  ): Rx[Resp] = {
    send(req, context).toRx.map { resp =>
      HttpClients.parseResponse[Resp](config, responseSurface, resp)
    }
  }

  def callInternal[Req, Resp](
      req: Request,
      requestSurface: Surface,
      responseSurface: Surface,
      requestContent: Req,
      context: HttpClientContext = HttpClientContext.empty
  ): Rx[Resp] = {
    Rx
      .const(HttpClients.prepareRequest(config, req, requestSurface, requestContent))
      .flatMap { (newRequest: Request) =>
        send(newRequest, context).toRx.map { resp =>
          HttpClients.parseResponse[Resp](config, responseSurface, resp)
        }
      }
  }

  /**
    * @param method
    * @param requestContent
    * @tparam Req
    * @tparam Resp
    * @return
    *   Rx of the response. If the RPC request fails, Rx[RPCException] will be returned.
    */
  def rpc[Req, Resp](
      method: RPCMethod,
      requestContent: Req,
      context: HttpClientContext = HttpClientContext.empty
  ): Rx[Resp] = {
    Rx
      .const(HttpClients.prepareRPCRequest(config, method.path, method.requestSurface, requestContent))
      .flatMap { (request: Request) =>
        val ctx = context.copy(
          rpcMethod = Some(method),
          rpcInput = Some(requestContent)
        )
        sendSafe(request, ctx).toRx
          .map { (response: Response) =>
            if (response.status.isSuccessful) {
              val ret = HttpClients.parseRPCResponse(config, response, method.responseSurface)
              ret.asInstanceOf[Resp]
            } else {
              throw RPCException.fromResponse(response)
            }
          }
      }
  }
}

class AsyncClientImpl(protected val channel: HttpChannel, val config: HttpClientConfig) extends AsyncClient {
  override protected def build(newConfig: HttpClientConfig): AsyncClient = new AsyncClientImpl(channel, newConfig)
  override def close(): Unit = {
    super.close()
    channel.close()
  }
}
