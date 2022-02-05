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
import wvlet.airframe.control.Retry.RetryContext
import wvlet.airframe.control.{Control, IO}
import wvlet.airframe.http.HttpMessage.{Request, Response}
import wvlet.airframe.http._
import wvlet.airframe.surface.Surface

import java.io.{IOException, InputStream, OutputStream}
import java.net.HttpURLConnection
import java.util.concurrent.TimeUnit
import java.util.zip.{GZIPInputStream, InflaterInputStream}
import scala.concurrent.duration.Duration
import scala.jdk.CollectionConverters._

case class URLConnectionClientConfig(
    requestFilter: Request => Request = identity,
    connectionFilter: HttpURLConnection => HttpURLConnection = identity,
    readTimeout: Duration = Duration(90, TimeUnit.SECONDS),
    connectTimeout: Duration = Duration(90, TimeUnit.SECONDS),
    retryContext: RetryContext = HttpClient.defaultHttpClientRetry[Request, Response],
    codecFactory: MessageCodecFactory = MessageCodecFactory.defaultFactoryForJSON,
    followRedirect: Boolean = true
)

/**
  * Http sync client implementation using URLConnection
  */
class URLConnectionClient(address: ServerAddress, protected val config: URLConnectionClientConfig)
    extends URLConnectionClientBase {

  override def send(
      req: Request,
      requestFilter: Request => Request
  ): Response = {

    // Apply the default filter first and then the given custom filter
    val request = requestFilter(config.requestFilter(req))

    val url = s"${address.uri}${if (request.uri.startsWith("/")) request.uri
      else s"/${request.uri}"}"

    // Send the request with retry support. Setting the context request is necessary to properly show
    // the request path upon errors
    config.retryContext.runWithContext(request) {
      val conn0: HttpURLConnection =
        new java.net.URL(url).openConnection().asInstanceOf[HttpURLConnection]
      conn0.setRequestMethod(request.method)
      for (e <- request.header.entries) {
        conn0.setRequestProperty(e.key, e.value)
      }
      conn0.setDoInput(true)

      def timeoutMillis(d: Duration): Int = {
        if (d.isFinite) {
          d.toMillis.toInt
        } else {
          0
        }
      }

      conn0.setReadTimeout(timeoutMillis(config.readTimeout))
      conn0.setConnectTimeout(timeoutMillis(config.connectTimeout))
      conn0.setInstanceFollowRedirects(config.followRedirect)

      val conn    = config.connectionFilter(conn0)
      val content = req.contentBytes
      if (content.nonEmpty) {
        conn.setDoOutput(true)
        Control.withResource(conn.getOutputStream()) { (out: OutputStream) =>
          out.write(content)
          out.flush()
        }
      }

      try {
        Control.withResource(conn.getInputStream()) { (in: InputStream) =>
          readResponse(conn, in)
        }
      } catch {
        case e: IOException if conn.getResponseCode != -1 =>
          // When the request fails, but the server still returns meaningful responses
          // (e.g., 404 NotFound throws FileNotFoundException)
          Control.withResource(conn.getErrorStream()) { (err: InputStream) =>
            readResponse(conn, err)
          }
      }
    }
  }

  private def readResponse(conn: HttpURLConnection, in: InputStream): Response = {
    val status = HttpStatus.ofCode(conn.getResponseCode)

    val h = HttpMultiMap.newBuilder
    for ((k, vv) <- conn.getHeaderFields().asScala if k != null; v <- vv.asScala) {
      h += k -> v
    }
    val response = Http.response(status).withHeader(h.result())
    val is = response.contentEncoding.map(_.toLowerCase) match {
      case _ if in == null => in
      case Some("gzip")    => new GZIPInputStream(in)
      case Some("deflate") => new InflaterInputStream(in)
      case other           =>
        // For unsupported encoding, read content as bytes
        in
    }

    // TODO: For supporting streaming read, we need to extend HttpMessage class
    val responseContentBytes = IO.readFully(is)
    response.withContent(responseContentBytes)
  }

  override def sendSafe(
      req: Request,
      requestFilter: Request => Request
  ): Response = {
    try {
      send(req, requestFilter)
    } catch {
      case e: HttpClientException =>
        e.response.toHttpResponse
    }
  }

  override def close(): Unit = {}

  protected def getInternal[Resource](
      resourcePath: String,
      requestFilter: Request => Request,
      resourceSurface: Surface
  ): Resource = {
    convertAs[Resource](send(Http.request(resourcePath), requestFilter), resourceSurface)
  }
//
//  protected def getOpsInternal[Resource, OperationResponse](
//    resourcePath: String,
//    resource: Resource,
//    requestFilter: Request => Request
//  ): OperationResponse = {
//    getResource[Resource, OperationResponse](resourcePath, resource, requestFilter)
//  }
//

}
