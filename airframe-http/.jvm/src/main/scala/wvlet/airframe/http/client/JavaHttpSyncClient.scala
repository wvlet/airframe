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
import wvlet.airframe.control.Retry.MaxRetryException
import wvlet.airframe.http.HttpMessage.{Request, Response}
import wvlet.airframe.http._

import java.net.URI
import java.net.http.HttpClient.Redirect
import java.net.http.HttpRequest.BodyPublishers
import java.net.http.HttpResponse.BodyHandlers
import java.net.http.{HttpClient, HttpRequest, HttpResponse}
import scala.jdk.CollectionConverters._
import scala.util.control.NonFatal

/**
  * Http client implementation using a new Java Http Client since Java 11.
  * @param serverAddress
  * @param config
  */
class JavaHttpSyncClient(serverAddress: ServerAddress, val config: HttpClientConfig) extends client.HttpSyncClient {

  private val javaHttpClient: HttpClient     = newClient(config)
  private val circuitBreaker: CircuitBreaker = config.circuitBreaker.withName(s"${serverAddress}")

  private def newClient(config: HttpClientConfig): HttpClient = {
    HttpClient
      .newBuilder()
      .followRedirects(Redirect.NORMAL)
      .connectTimeout(java.time.Duration.ofMillis(config.connectTimeout.toMillis))
      .build()
  }

  override def send(
      req: HttpMessage.Request,
      requestFilter: HttpMessage.Request => HttpMessage.Request
  ): HttpMessage.Response = {

    val request = config.requestFilter(requestFilter(req))
    // New Java's HttpRequest is immutable, so we can reuse the same request instance
    val httpRequest = buildRequest(serverAddress, request, config)

    var lastResponse: Response = null
    try {
      // Send http requst with retry support
      config.retryContext.runWithContext(request, circuitBreaker) {
        val httpResponse: HttpResponse[Array[Byte]] =
          javaHttpClient.send(httpRequest, BodyHandlers.ofByteArray())

        lastResponse = readResponse(httpResponse)
        lastResponse
      }
    } catch {
      case e: MaxRetryException =>
        throw HttpClientMaxRetryException(
          Option(lastResponse).getOrElse(Http.response(HttpStatus.InternalServerError_500)),
          e.retryContext,
          e.retryContext.lastError
        )
      case e: HttpClientException =>
        // Throw as is
        throw e
      case NonFatal(e) =>
        val resp = Option(lastResponse).getOrElse(Http.response(HttpStatus.InternalServerError_500))
        throw new HttpClientException(
          resp,
          status = resp.status,
          message = e.getMessage,
          cause = e
        )
    }
  }

  override def close(): Unit = {
    // It seems Java Http Client has no close method
  }

  private def buildRequest(
      serverAddress: ServerAddress,
      request: Request,
      config: HttpClientConfig
  ): HttpRequest = {
    val uri = s"${serverAddress.uri}${if (request.uri.startsWith("/")) request.uri
      else s"/${request.uri}"}"

    val requestBuilder = HttpRequest
      .newBuilder(URI.create(uri))
      .timeout(java.time.Duration.ofMillis(config.readTimeout.toMillis))

    // Set HTTP request headers
    request.header.entries.foreach(h => requestBuilder.setHeader(h.key, h.value))

    requestBuilder.method(
      request.method,
      request.message match {
        case HttpMessage.EmptyMessage =>
          BodyPublishers.noBody()
        case s: HttpMessage.StringMessage =>
          BodyPublishers.ofString(s.toContentString)
        case m =>
          BodyPublishers.ofByteArray(m.toContentBytes)
      }
    )

    requestBuilder.build()
  }

  private def readResponse(httpResponse: java.net.http.HttpResponse[Array[Byte]]): Response = {
    // Read HTTP response headers
    val header = HttpMultiMap.newBuilder
    httpResponse.headers().map().asScala.map { case (key, values) =>
      values.asScala.foreach { v =>
        header.add(key, v)
      }
    }

    Http
      .response(HttpStatus.ofCode(httpResponse.statusCode()))
      .withHeader(header.result())
      .withContent(HttpMessage.byteArrayMessage(httpResponse.body()))
  }

  def toAsyncClient: JavaHttpAsyncClient = new JavaHttpAsyncClient(this)
}
