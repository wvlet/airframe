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

import wvlet.airframe.control.Control.withResource
import wvlet.airframe.control.IO
import wvlet.airframe.http.HttpMessage.{Request, Response}
import wvlet.airframe.http.*
import wvlet.airframe.rx.Rx

import java.io.InputStream
import java.net.URI
import java.net.http.HttpClient.Redirect
import java.net.http.HttpRequest.BodyPublishers
import java.net.http.HttpResponse.BodyHandlers
import java.net.http.{HttpClient, HttpRequest, HttpResponse}
import java.util.function.Consumer
import java.util.zip.{GZIPInputStream, InflaterInputStream}
import scala.jdk.CollectionConverters.*

/**
  * Http connection implementation using Http Client of Java 11
  * @param serverAddress
  * @param config
  */
class JavaHttpClientChannel(serverAddress: ServerAddress, private[http] val config: HttpClientConfig)
    extends HttpChannel {
  private val javaHttpClient: HttpClient = initClient(config)

  private def initClient(config: HttpClientConfig): HttpClient = {
    HttpClient
      .newBuilder()
      .followRedirects(Redirect.NORMAL)
      // Note: We tried to set a custom executor here for Java HttpClient, but
      // internally the executor will be shared between multiple HttpClients and closing the executor will block
      // other http clients, so we do not use the custom executor here.
      // .executor(new Executor {
      //  override def execute(command: Runnable): Unit = executionContext.execute(command)
      // })
      .build()
  }

  override def close(): Unit = {
    // It seems Java Http Client has no close() method
  }

  override def send(req: Request, channelConfig: HttpChannelConfig): Response = {
    // New Java's HttpRequest is immutable, so we can reuse the same request instance
    val httpRequest = buildRequest(serverAddress, req, channelConfig)
    val httpResponse: HttpResponse[InputStream] =
      javaHttpClient.send(httpRequest, BodyHandlers.ofInputStream())

    readResponse(httpResponse)
  }

  override def sendAsync(req: Request, channelConfig: HttpChannelConfig): Rx[Response] = {
    val v = Rx.variable[Option[Response]](None)
    try {
      val httpRequest = buildRequest(serverAddress, req, channelConfig)
      javaHttpClient
        .sendAsync(httpRequest, BodyHandlers.ofInputStream())
        .thenAccept(new Consumer[HttpResponse[InputStream]] {
          override def accept(r: HttpResponse[InputStream]): Unit = {
            val resp = readResponse(r)
            v.set(Some(resp))
            // Close the variable as it will have no further update
            v.stop()
          }
        })
        .exceptionally { (ex: Throwable) =>
          v.setException(ex)
          null
        }
    } catch {
      case e: Throwable =>
        v.setException(e)
    }
    v.filter(_.isDefined).map(_.get)
  }

  private def buildRequest(
      serverAddress: ServerAddress,
      request: Request,
      channelConfig: HttpChannelConfig
  ): HttpRequest = {
    val uri = s"${serverAddress.uri}${if (request.uri.startsWith("/")) request.uri
      else s"/${request.uri}"}"

    val requestBuilder = HttpRequest
      .newBuilder(URI.create(uri))
      .timeout(java.time.Duration.ofMillis(channelConfig.readTimeout.toMillis))

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
      httpResponse.headers().map().asScala.foreach { case (key, values) =>
        if (!key.startsWith(":")) {
          values.asScala.foreach { v =>
            h.add(key, v)
          }
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
}
