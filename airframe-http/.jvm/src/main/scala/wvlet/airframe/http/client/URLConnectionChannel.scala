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

import wvlet.airframe.control.{Control, IO}
import wvlet.airframe.http.HttpMessage.{Request, Response}
import wvlet.airframe.http.*
import wvlet.airframe.rx.Rx

import java.io.{IOException, InputStream, OutputStream}
import java.net.HttpURLConnection
import java.util.zip.{GZIPInputStream, InflaterInputStream}
import scala.concurrent.duration.Duration
import scala.jdk.CollectionConverters.*

/**
  * Java8-compatible HTTP channel implementation based on {{URL.openConnection}}
  * @param serverAddress
  * @param config
  */
class URLConnectionChannel(val destination: ServerAddress, config: HttpClientConfig) extends HttpChannel {
  override def send(request: Request, channelConfig: HttpChannelConfig): Response = {
    val url = s"${request.dest.getOrElse(destination).uri}${if (request.uri.startsWith("/")) request.uri
      else s"/${request.uri}"}"

    val conn0: HttpURLConnection =
      new java.net.URL(url).openConnection().asInstanceOf[HttpURLConnection]

    request.method match {
      case HttpMethod.PATCH =>
        // URLConnection doesn't support patch, so we need to use POST endpoint + X-HTTP-Method-Override header
        conn0.setRequestMethod(HttpMethod.POST)
        conn0.setRequestProperty("X-HTTP-Method-Override", HttpMethod.PATCH)
      case _ =>
        conn0.setRequestMethod(request.method)
    }
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

    conn0.setReadTimeout(timeoutMillis(channelConfig.readTimeout))
    conn0.setConnectTimeout(timeoutMillis(channelConfig.connectTimeout))
    conn0.setInstanceFollowRedirects(true)

    val conn    = conn0 // config.connectionFilter(conn0)
    val content = request.contentBytes
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

  override def sendAsync(req: Request, channelConfig: HttpChannelConfig): Rx[Response] = {
    Rx.single(send(req, channelConfig))
  }

  override def close(): Unit = {
    // Nothing to close for URLConnection
  }
}
