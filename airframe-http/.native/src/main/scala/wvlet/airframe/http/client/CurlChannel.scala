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

import wvlet.airframe.http.*
import wvlet.airframe.http.HttpMessage.{Request, Response}
import wvlet.airframe.rx.Rx
import wvlet.log.LogSupport

import java.io.IOException
import scala.concurrent.duration.Duration
import scala.scalanative.unsafe.*

/**
  * HTTP channel implementation using libcurl for Scala Native.
  *
  * This implementation uses libcurl's easy interface for making HTTP requests. It supports:
  *   - All standard HTTP methods (GET, POST, PUT, DELETE, PATCH, HEAD, OPTIONS)
  *   - Custom headers
  *   - Request body
  *   - Response headers and body
  *   - Timeouts (connect and read)
  *   - Automatic redirect following
  *   - Automatic decompression (via Accept-Encoding)
  *
  * @param destination
  *   The default server address for requests
  * @param config
  *   HTTP client configuration
  */
class CurlChannel(val destination: ServerAddress, config: HttpClientConfig) extends HttpChannel with LogSupport {
  import CurlBindings.*
  import CurlCallbacks.*

  override def send(request: Request, channelConfig: HttpChannelConfig): Response = {
    val curl = curl_easy_init()
    if (curl == null) {
      throw new IOException("Failed to initialize curl handle")
    }

    var headerList: Ptr[CurlSlist] = null
    val bodyBuffer                 = allocResponseBuffer()
    val headerBuffer               = allocResponseBuffer()

    try {
      // Build the full URL
      val targetDest = request.dest.getOrElse(destination)
      val uri        = if (request.uri.startsWith("/")) request.uri else s"/${request.uri}"
      val url        = s"${targetDest.uri}${uri}"

      Zone.acquire { implicit z =>
        // Set URL
        checkCurlError(curl_easy_setopt_str(curl, CURLOPT_URL, toCString(url)), "setting URL")

        // Set HTTP method
        request.method match {
          case HttpMethod.GET =>
            curl_easy_setopt_long(curl, CURLOPT_HTTPGET, 1L)
          case HttpMethod.POST =>
            curl_easy_setopt_long(curl, CURLOPT_POST, 1L)
          case HttpMethod.HEAD =>
            curl_easy_setopt_long(curl, CURLOPT_NOBODY, 1L)
          case HttpMethod.PUT | HttpMethod.DELETE | HttpMethod.PATCH | HttpMethod.OPTIONS =>
            curl_easy_setopt_str(curl, CURLOPT_CUSTOMREQUEST, toCString(request.method))
          case other =>
            curl_easy_setopt_str(curl, CURLOPT_CUSTOMREQUEST, toCString(other))
        }

        // Set request headers
        for (entry <- request.header.entries) {
          val headerLine = s"${entry.key}: ${entry.value}"
          headerList = curl_slist_append(headerList, toCString(headerLine))
        }

        // Add Accept-Encoding for automatic decompression
        if (request.header.get(HttpHeader.AcceptEncoding).isEmpty) {
          headerList = curl_slist_append(headerList, c"Accept-Encoding: gzip, deflate")
        }

        if (headerList != null) {
          curl_easy_setopt_slist(curl, CURLOPT_HTTPHEADER, headerList)
        }

        // Set request body
        val contentBytes = request.contentBytes
        if (contentBytes.nonEmpty) {
          val bodyPtr = alloc[Byte](contentBytes.length)
          var i       = 0
          while (i < contentBytes.length) {
            !(bodyPtr + i) = contentBytes(i)
            i += 1
          }
          curl_easy_setopt_ptr(curl, CURLOPT_POSTFIELDS, bodyPtr)
          curl_easy_setopt_long(curl, CURLOPT_POSTFIELDSIZE, contentBytes.length.toLong)
        }

        // Set timeouts
        val connectTimeoutMs = timeoutMillis(channelConfig.connectTimeout)
        val readTimeoutMs    = timeoutMillis(channelConfig.readTimeout)

        if (connectTimeoutMs > 0) {
          curl_easy_setopt_long(curl, CURLOPT_CONNECTTIMEOUT_MS, connectTimeoutMs.toLong)
        }
        if (readTimeoutMs > 0) {
          curl_easy_setopt_long(curl, CURLOPT_TIMEOUT_MS, readTimeoutMs.toLong)
        }

        // Follow redirects
        curl_easy_setopt_long(curl, CURLOPT_FOLLOWLOCATION, 1L)
        curl_easy_setopt_long(curl, CURLOPT_MAXREDIRS, 10L)

        // Use HTTP/1.1 if configured
        if (config.useHttp1) {
          curl_easy_setopt_long(curl, CURLOPT_HTTP_VERSION, CURL_HTTP_VERSION_1_1)
        }

        // Set write callback for response body
        curl_easy_setopt_callback(curl, CURLOPT_WRITEFUNCTION, writeCallback)
        curl_easy_setopt_ptr(curl, CURLOPT_WRITEDATA, bodyBuffer.asInstanceOf[Ptr[Byte]])

        // Set header callback for response headers
        curl_easy_setopt_callback(curl, CURLOPT_HEADERFUNCTION, headerCallback)
        curl_easy_setopt_ptr(curl, CURLOPT_HEADERDATA, headerBuffer.asInstanceOf[Ptr[Byte]])

        // Perform the request
        val result = curl_easy_perform(curl)

        if (result != CURLE_OK) {
          val errorMsg = fromCString(curl_easy_strerror(result))
          val status = result match {
            case CURLE_COULDNT_RESOLVE_HOST | CURLE_COULDNT_RESOLVE_PROXY =>
              HttpStatus.BadGateway_502
            case CURLE_COULDNT_CONNECT =>
              HttpStatus.ServiceUnavailable_503
            case CURLE_OPERATION_TIMEDOUT =>
              HttpStatus.GatewayTimeout_504
            case CURLE_SSL_CONNECT_ERROR | CURLE_PEER_FAILED_VERIFICATION =>
              HttpStatus.BadGateway_502
            case _ =>
              HttpStatus.InternalServerError_500
          }
          // Create a dummy response for the exception
          val errorResponse = Http.response(status).withContent(s"curl error: $errorMsg (code: $result)")
          throw new HttpClientException(errorResponse, status, s"curl error: $errorMsg (code: $result)")
        }

        // Get response status code
        val statusCodePtr = stackalloc[Long]()
        curl_easy_getinfo_long(curl, CURLINFO_RESPONSE_CODE, statusCodePtr)
        val statusCode = (!statusCodePtr).toInt

        // Parse response headers
        val headerString   = getBufferString(headerBuffer)
        val responseHeader = parseHeaders(headerString)

        // Get response body
        val responseBody = getBufferData(bodyBuffer)

        // Build response
        val status = HttpStatus.ofCode(statusCode)
        Http
          .response(status)
          .withHeader(responseHeader)
          .withContent(responseBody)
      }
    } finally {
      // Cleanup
      if (headerList != null) {
        curl_slist_free_all(headerList)
      }
      freeResponseBuffer(bodyBuffer)
      freeResponseBuffer(headerBuffer)
      curl_easy_cleanup(curl)
    }
  }

  override def sendAsync(request: Request, channelConfig: HttpChannelConfig): Rx[Response] = {
    // For Scala Native, we wrap the synchronous call in Rx.single
    // True async would require curl_multi API which adds complexity
    Rx.single(send(request, channelConfig))
  }

  override def close(): Unit = {
    // Nothing to close for curl easy interface
    // Each request creates and destroys its own handle
  }

  private def timeoutMillis(d: Duration): Int = {
    if (d.isFinite) {
      d.toMillis.toInt
    } else {
      0
    }
  }

  private def checkCurlError(code: CInt, operation: String): Unit = {
    if (code != CURLE_OK) {
      val errorMsg = fromCString(curl_easy_strerror(code))
      throw new IOException(s"curl error while $operation: $errorMsg (code: $code)")
    }
  }

  /**
    * Parse HTTP headers from curl's header output.
    *
    * Curl returns headers in the format: "HTTP/1.1 200 OK\r\n" "Header-Name: Header-Value\r\n" ...
    */
  private def parseHeaders(headerString: String): HttpMultiMap = {
    val builder = HttpMultiMap.newBuilder
    val lines   = headerString.split("\r\n")

    for (line <- lines) {
      // Skip empty lines and status line (starts with HTTP/)
      if (line.nonEmpty && !line.startsWith("HTTP/")) {
        val colonIndex = line.indexOf(':')
        if (colonIndex > 0) {
          val key   = line.substring(0, colonIndex).trim
          val value = line.substring(colonIndex + 1).trim
          builder.add(key, value)
        }
      }
    }

    builder.result()
  }
}
