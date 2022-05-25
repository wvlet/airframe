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
import wvlet.airframe.http.HttpMessage.{Message, Response}
import wvlet.airframe.http.{Http, HttpClientConfig, HttpHeader, HttpMessage, HttpMultiMap, HttpStatus, ServerAddress}

import java.net.{Authenticator, URI}
import java.net.http.{HttpClient, HttpRequest, HttpResponse}
import java.net.http.HttpClient.Redirect
import java.net.http.HttpRequest.{BodyPublisher, BodyPublishers}
import java.net.http.HttpResponse.BodyHandlers
import scala.jdk.CollectionConverters._

/**
  * Http client implmeentation using a new Java Http Client since Java 11.
  * @param serverAddress
  * @param config
  */
class JDKSyncHttpClient(serverAddress: ServerAddress, config: HttpClientConfig) extends HttpSyncClient {

  private val javaHttpClient: HttpClient = {
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

    val request = requestFilter(config.requestFilter(req))
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
        case b: HttpMessage.ByteArrayMessage =>
          BodyPublishers.ofByteArray(b.toContentBytes)
        case m =>
          BodyPublishers.ofByteArray(m.toContentBytes)
      }
    )

    val httpRequest = requestBuilder.build()
    config.retryContext.runWithContext(request) {

      val httpResponse: HttpResponse[Array[Byte]] =
        javaHttpClient.send(httpRequest, BodyHandlers.ofByteArray())

      // Read HTTP response headers
      val header = HttpMultiMap.newBuilder
      httpResponse.headers().map().asScala.map { case (key, values) =>
        values.asScala.foreach { v =>
          header.add(key, v)
        }
      }

      val resp = Http
        .response(HttpStatus.ofCode(httpResponse.statusCode()))
        .withHeader(header.result())
        .withContent(HttpMessage.byteArrayMessage(httpResponse.body()))
      resp
    }
  }
}
