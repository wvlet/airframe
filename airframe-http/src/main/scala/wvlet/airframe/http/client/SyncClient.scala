r/*
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
import wvlet.airframe.http.*
import wvlet.airframe.rx.{OnCompletion, OnError, OnNext, Rx, RxRunner}
import wvlet.airframe.surface.Surface

/**
  * A standard blocking http client interface
  */
trait SyncClient extends SyncClientCompat with HttpClientFactory[SyncClient] with AutoCloseable {

  protected def channel: HttpChannel
  def config: HttpClientConfig

  private val clientLogger: HttpLogger        = config.newHttpLogger
  private val loggingFilter: HttpClientFilter = config.newLoggingFilter(clientLogger)
  private val circuitBreaker: CircuitBreaker  = config.circuitBreaker

  override def close(): Unit = {
    clientLogger.close()
  }

  /**
    * Send an HTTP request and get the response. It will throw an exception for non-successful responses. For example,
    * when receiving non-retryable status code (e.g., 4xx), it will throw HttpClientException. For server side failures
    * (5xx responses), this continues request retry until the max retry count.
    *
    * If it exceeds the number of max retry attempts, HttpClientMaxRetryException will be thrown.
    *
    * @throws HttpClientMaxRetryException
    *   if max retry reaches
    * @throws HttpClientException
    *   for non-retryable error is occurred
    */
  def send(req: Request, context: HttpClientContext = HttpClientContext.empty): Response = {
    val request                        = config.requestFilter(req)
    var lastResponse: Option[Response] = None

    // Build a chain of request filters
    def requestPipeline: RxHttpEndpoint = {
      loggingFilter(context)
        .andThen { req =>
          Rx.single(channel.send(req, config))
            .tap { resp =>
              // Remember the last response for the error reporting purpose
              lastResponse = Some(resp)
            }
        }
    }

    val rx =
      // Apply the client filter first to handle only the last response
      config.clientFilter.andThen { req =>
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

    // Await the response
    rx.apply(request).await
  }

  /**
    * Send an HTTP request and returns a response (or the last response if the request is retried). Unlike [[send()]],
    * this method returns a regular Http Response object even for non-retryable responses (e.g., 4xx error code). For
    * retryable responses (e.g., 5xx) this continues retry until the max retry count.
    *
    * After reaching the max retry count, it will return a the last response even for 5xx status code.
    */
  def sendSafe(req: Request, context: HttpClientContext = HttpClientContext.empty): Response = {
    try {
      send(req, context)
    } catch {
      case e: HttpClientException =>
        e.response.toHttpResponse
    }
  }

  def readAsInternal[Resp](
      req: Request,
      responseSurface: Surface
  ): Resp = {
    val resp: Response = send(req, HttpClientContext(config.name))
    HttpClients.parseResponse[Resp](config, responseSurface, resp)
  }

  def callInternal[Req, Resp](
      req: Request,
      requestSurface: Surface,
      responseSurface: Surface,
      requestContent: Req
  ): Resp = {
    val newRequest     = HttpClients.prepareRequest(config, req, requestSurface, requestContent)
    val resp: Response = send(newRequest, HttpClientContext(config.name))
    HttpClients.parseResponse[Resp](config, responseSurface, resp)
  }

  /**
    * Send an RPC request (POST) and return the RPC response. This method will throw RPCException when an error happens
    * @param method
    * @param request
    * @tparam Req
    * @return
    *
    * @throws RPCException
    *   when RPC request fails
    */
  def rpc[Req, Resp](method: RPCMethod, requestContent: Req): Resp = {
    val request: Request =
      HttpClients.prepareRPCRequest(config, method.path, method.requestSurface, requestContent)

    val context = HttpClientContext(
      clientName = config.name,
      rpcMethod = Some(method),
      rpcInput = Some(requestContent)
    )
    // sendSafe method internally handles retries and HttpClientException, and then it returns the last response
    val response: Response = sendSafe(request, context = context)

    // Parse the RPC response
    if (response.status.isSuccessful) {
      val ret = HttpClients.parseRPCResponse(config, response, method.responseSurface)
      ret.asInstanceOf[Resp]
    } else {
      // Parse the RPC error message
      throw RPCException.fromResponse(response)
    }
  }
}

class SyncClientImpl(protected val channel: HttpChannel, val config: HttpClientConfig) extends SyncClient {
  override protected def build(newConfig: HttpClientConfig): SyncClient = {
    new SyncClientImpl(channel, newConfig)
  }
  override def close(): Unit = {
    super.close()
    channel.close()
  }
}
