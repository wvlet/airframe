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

import wvlet.airframe.control.{CircuitBreaker, IO}
import wvlet.airframe.control.Control.withResource
import wvlet.airframe.control.Retry.MaxRetryException
import wvlet.airframe.http.HttpMessage.{Request, Response}
import wvlet.airframe.http._

import java.io.{ByteArrayInputStream, InputStream}
import java.net.URI
import java.net.http.HttpClient.Redirect
import java.net.http.HttpRequest.BodyPublishers
import java.net.http.HttpResponse.BodyHandlers
import java.net.http.{HttpClient, HttpRequest, HttpResponse}
import java.util.concurrent.{Executor, ExecutorService}
import java.util.zip.{GZIPInputStream, InflaterInputStream}
import scala.concurrent.{ExecutionContext, Future}
import scala.jdk.CollectionConverters._
import scala.util.control.NonFatal

/**
  * Http client implementation using a new Java Http Client since Java 11.
  * @param serverAddress
  * @param config
  */
class JavaHttpSyncClient(serverAddress: ServerAddress, config: HttpClientConfig) extends client.HttpSyncClient {

  private val javaHttpClient: HttpClient     = newClient(config)
  private val circuitBreaker: CircuitBreaker = config.circuitBreaker.withName(s"${serverAddress}")
  private implicit val ec: ExecutionContext  = config.executionContextProvider(config)

  override def close(): Unit = {
    // It seems Java Http Client has no close method
    ec match {
      case e: ExecutorService =>
        // Close the thread pool
        e.shutdownNow()
      case _ =>
    }
  }

  private def newClient(config: HttpClientConfig): HttpClient = {
    HttpClient
      .newBuilder()
      .followRedirects(Redirect.NORMAL)
      .connectTimeout(java.time.Duration.ofMillis(config.connectTimeout.toMillis))
      // Wrap Scala's ExecutionContext as Java's Executor
      .executor(new Executor {
        override def execute(command: Runnable): Unit = ec.execute(command)
      })
      .build()
  }

  override def send(
      req: HttpMessage.Request,
      requestFilter: HttpMessage.Request => HttpMessage.Request
  ): HttpMessage.Response = {

    val request = requestFilter(config.requestFilter(req))
    // New Java's HttpRequest is immutable, so we can reuse the same request instance
    val httpRequest = buildRequest(serverAddress, request, config)

    var lastResponse: Response = null
    try {
      // Send http requst with retry support
      config.retryContext.runWithContext(request, circuitBreaker) {
        val httpResponse: HttpResponse[InputStream] =
          javaHttpClient.send(httpRequest, BodyHandlers.ofInputStream())

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

  def sendAsync(
      req: HttpMessage.Request,
      requestFilter: HttpMessage.Request => HttpMessage.Request
  ): Future[HttpMessage.Response] = {
    Future.apply {
      send(req, requestFilter)
    }
  }

  def sendSafeAsync(
      req: HttpMessage.Request,
      requestFilter: HttpMessage.Request => HttpMessage.Request
  ): Future[HttpMessage.Response] = {
    Future.apply {
      sendSafe(req, requestFilter)
    }
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

  private def readResponse(httpResponse: java.net.http.HttpResponse[InputStream]): Response = {
    // Read HTTP response headers
    val header: HttpMultiMap = {
      val h = HttpMultiMap.newBuilder
      httpResponse.headers().map().asScala.map { case (key, values) =>
        values.asScala.foreach { v =>
          h.add(key, v)
        }
      }
      h.result()
    }

    // Decompress contents
    val body: Array[Byte] = withResource {
      header.get(HttpHeader.ContentEncoding).map(_.toLowerCase()) match {
        case Some("gzip") =>
          new GZIPInputStream(httpResponse.body())
        case Some("deflate") =>
          new InflaterInputStream(httpResponse.body())
        case _ =>
          httpResponse.body()
      }
    } { (in: InputStream) =>
      IO.readFully(in)
    }

    Http
      .response(HttpStatus.ofCode(httpResponse.statusCode()))
      .withHeader(header)
      .withContent(HttpMessage.byteArrayMessage(body))
  }

  def toAsyncClient: JavaHttpAsyncClient = new JavaHttpAsyncClient(this)
}
