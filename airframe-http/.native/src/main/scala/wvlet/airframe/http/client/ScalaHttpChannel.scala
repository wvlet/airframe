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
import java.nio.charset.StandardCharsets
import scala.collection.mutable.ArrayBuffer
import scala.concurrent.duration.Duration

/**
  * Pure Scala HTTP/1.1 channel implementation for Scala Native.
  *
  * This implementation uses direct POSIX socket bindings to make HTTP requests without requiring external libraries like
  * libcurl. It provides a zero-dependency HTTP client for Scala Native applications.
  *
  * Features:
  *   - Full HTTP/1.1 support (request/response)
  *   - Content-Length and chunked transfer encoding
  *   - Configurable connect and read timeouts
  *   - Automatic redirect following
  *   - Connection reuse (keep-alive)
  *
  * Limitations:
  *   - HTTP only (no HTTPS/TLS support without additional SSL bindings)
  *   - Synchronous operations (async wraps sync in Rx)
  *   - No HTTP/2 support
  *   - No automatic compression/decompression
  *
  * @param destination
  *   The default server address for requests
  * @param config
  *   HTTP client configuration
  */
class ScalaHttpChannel(val destination: ServerAddress, config: HttpClientConfig) extends HttpChannel with LogSupport {
  import SocketBindings.*

  // Maximum header size (8KB)
  private val MaxHeaderSize = 8192
  // Maximum number of redirects to follow
  private val MaxRedirects = 10
  // Buffer size for reading
  private val ReadBufferSize = 8192

  // Cached socket for connection reuse (keep-alive)
  @volatile private var cachedSocket: Option[SocketHandle] = None
  @volatile private var cachedHost: String = ""
  @volatile private var cachedPort: Int = 0

  override def send(request: Request, channelConfig: HttpChannelConfig): Response = {
    sendWithRedirects(request, channelConfig, 0)
  }

  private def sendWithRedirects(request: Request, channelConfig: HttpChannelConfig, redirectCount: Int): Response = {
    if (redirectCount > MaxRedirects) {
      throw new HttpClientException(
        Http.response(HttpStatus.BadGateway_502),
        HttpStatus.BadGateway_502,
        s"Too many redirects (max: $MaxRedirects)"
      )
    }

    val targetDest = request.dest.getOrElse(destination)

    // Check if HTTPS is requested
    if (targetDest.scheme == "https") {
      throw new HttpClientException(
        Http.response(HttpStatus.BadGateway_502),
        HttpStatus.BadGateway_502,
        "HTTPS is not supported by ScalaHttpChannel. Use CurlChannel for HTTPS support."
      )
    }

    val host = targetDest.host
    val port = if (targetDest.port > 0) targetDest.port else 80

    val connectTimeoutMs = timeoutMillis(channelConfig.connectTimeout)
    val readTimeoutMs = timeoutMillis(channelConfig.readTimeout)

    // Get or create socket
    val socket = getOrCreateSocket(host, port, connectTimeoutMs)

    try {
      // Set read timeout
      if (readTimeoutMs > 0) {
        setSocketTimeout(socket, SO_RCVTIMEO, readTimeoutMs)
      }

      // Build and send request
      val requestBytes = Http11Protocol.buildRequest(request, host, port)
      debug(s"Sending request: ${request.method} ${request.uri} to $host:$port")

      sendAll(socket, requestBytes) match {
        case SocketError(code, msg) =>
          closeSocket()
          throw createException(code, msg, "sending request")
        case _ =>
      }

      // Read response headers
      val (statusCode, headers, bodyStartOffset, extraData) = readResponseHeaders(socket)
      debug(s"Received response: $statusCode")

      // Determine body reading strategy
      val bodyStrategy = Http11Protocol.determineBodyStrategy(headers, request.method, statusCode)

      // Read body based on strategy
      val body = bodyStrategy match {
        case Http11Protocol.NoBodyStrategy =>
          Array.empty[Byte]

        case Http11Protocol.ContentLengthStrategy(length) =>
          readContentLengthBody(socket, length.toInt, extraData)

        case Http11Protocol.ChunkedStrategy =>
          readChunkedBody(socket, extraData)

        case Http11Protocol.ReadUntilCloseStrategy =>
          val result = readUntilClose(socket, extraData)
          closeSocket()
          result
      }

      // Check if connection should be closed
      val shouldClose = headers.get("Connection").exists(_.toLowerCase == "close")
      if (shouldClose) {
        closeSocket()
      }

      val response = Http11Protocol.buildResponse(statusCode, headers, body)

      // Handle redirects
      if (statusCode >= 300 && statusCode < 400) {
        headers.get("Location") match {
          case Some(location) =>
            debug(s"Following redirect to: $location")
            val redirectRequest = createRedirectRequest(request, location, targetDest)
            return sendWithRedirects(redirectRequest, channelConfig, redirectCount + 1)
          case None =>
            // No Location header, return the redirect response as-is
        }
      }

      response
    } catch {
      case e: HttpClientException =>
        closeSocket()
        throw e
      case e: IOException =>
        closeSocket()
        throw new HttpClientException(
          Http.response(HttpStatus.BadGateway_502),
          HttpStatus.BadGateway_502,
          s"IO error: ${e.getMessage}"
        )
      case e: Exception =>
        closeSocket()
        throw new HttpClientException(
          Http.response(HttpStatus.InternalServerError_500),
          HttpStatus.InternalServerError_500,
          s"Unexpected error: ${e.getMessage}"
        )
    }
  }

  override def sendAsync(request: Request, channelConfig: HttpChannelConfig): Rx[Response] = {
    // Wrap synchronous call in Rx for Scala Native
    Rx.single(send(request, channelConfig))
  }

  override def close(): Unit = {
    closeSocket()
  }

  private def getOrCreateSocket(host: String, port: Int, connectTimeoutMs: Long): SocketHandle = {
    cachedSocket match {
      case Some(socket) if !socket.isClosed && host == cachedHost && port == cachedPort =>
        socket
      case _ =>
        closeSocket()
        connectToHost(host, port, connectTimeoutMs) match {
          case SocketSuccess(socket) =>
            cachedSocket = Some(socket)
            cachedHost = host
            cachedPort = port
            socket
          case SocketError(code, msg) =>
            throw createException(code, msg, "connecting")
        }
    }
  }

  private def closeSocket(): Unit = {
    cachedSocket.foreach(_.close())
    cachedSocket = None
    cachedHost = ""
    cachedPort = 0
  }

  /**
    * Read HTTP response headers from socket.
    * Returns (statusCode, headers, bodyStartIndex, anyExtraDataReadBeyondHeaders)
    */
  private def readResponseHeaders(socket: SocketHandle): (Int, HttpMultiMap, Int, Array[Byte]) = {
    val buffer = new ArrayBuffer[Byte](4096)
    var headerEnd = -1

    // Read until we find the header end marker
    while (headerEnd < 0 && buffer.length < MaxHeaderSize) {
      receive(socket, ReadBufferSize) match {
        case SocketSuccess(data) =>
          if (data.isEmpty) {
            throw new HttpClientException(
              Http.response(HttpStatus.BadGateway_502),
              HttpStatus.BadGateway_502,
              "Connection closed before headers received"
            )
          }
          val startCheck = math.max(0, buffer.length - 3)
          buffer ++= data
          headerEnd = Http11Protocol.findHeaderEnd(buffer.toArray, startCheck)

        case SocketError(code, msg) =>
          throw createException(code, msg, "reading headers")
      }
    }

    if (headerEnd < 0) {
      throw new HttpClientException(
        Http.response(HttpStatus.BadGateway_502),
        HttpStatus.BadGateway_502,
        s"Headers too large (max: $MaxHeaderSize bytes)"
      )
    }

    val allData = buffer.toArray
    val headerData = allData.take(headerEnd)
    val extraData = allData.drop(headerEnd)

    val (statusCode, headers, _) = Http11Protocol.parseResponseHeaders(headerData)
    (statusCode, headers, headerEnd, extraData)
  }

  /**
    * Read body with known Content-Length
    */
  private def readContentLengthBody(socket: SocketHandle, length: Int, extraData: Array[Byte]): Array[Byte] = {
    if (length == 0) {
      return Array.empty[Byte]
    }

    val result = new Array[Byte](length)
    var offset = 0

    // Copy any extra data read during header parsing
    if (extraData.nonEmpty) {
      val toCopy = math.min(extraData.length, length)
      System.arraycopy(extraData, 0, result, 0, toCopy)
      offset = toCopy
    }

    // Read remaining body
    while (offset < length) {
      receive(socket, math.min(ReadBufferSize, length - offset)) match {
        case SocketSuccess(data) =>
          if (data.isEmpty) {
            throw new HttpClientException(
              Http.response(HttpStatus.BadGateway_502),
              HttpStatus.BadGateway_502,
              s"Connection closed after reading $offset of $length bytes"
            )
          }
          System.arraycopy(data, 0, result, offset, data.length)
          offset += data.length

        case SocketError(code, msg) =>
          throw createException(code, msg, "reading body")
      }
    }

    result
  }

  /**
    * Read chunked transfer encoding body
    */
  private def readChunkedBody(socket: SocketHandle, extraData: Array[Byte]): Array[Byte] = {
    // Buffer to accumulate data
    val buffer = new ArrayBuffer[Byte]()
    buffer ++= extraData

    // Line reader function
    def readLine(): SocketBindings.SocketResult[String] = {
      // Find CRLF in buffer
      var crlfIndex = findCrlf(buffer.toArray, 0)
      while (crlfIndex < 0) {
        receive(socket, ReadBufferSize) match {
          case SocketSuccess(data) =>
            if (data.isEmpty) {
              return SocketError(ECONNRESET, "Connection closed reading chunked data")
            }
            buffer ++= data
            crlfIndex = findCrlf(buffer.toArray, math.max(0, buffer.length - data.length - 1))
          case err: SocketError =>
            return err
        }
      }
      val line = new String(buffer.take(crlfIndex).toArray, StandardCharsets.UTF_8)
      buffer.remove(0, crlfIndex + 2) // Remove line + CRLF
      SocketSuccess(line)
    }

    // Exact bytes reader function
    def readExact(n: Int): SocketBindings.SocketResult[Array[Byte]] = {
      while (buffer.length < n) {
        receive(socket, ReadBufferSize) match {
          case SocketSuccess(data) =>
            if (data.isEmpty) {
              return SocketError(ECONNRESET, "Connection closed reading chunk")
            }
            buffer ++= data
          case err: SocketError =>
            return err
        }
      }
      val result = buffer.take(n).toArray
      buffer.remove(0, n)
      SocketSuccess(result)
    }

    Http11Protocol.parseChunkedBody(readExact, () => readLine())
  }

  /**
    * Read until connection closes
    */
  private def readUntilClose(socket: SocketHandle, extraData: Array[Byte]): Array[Byte] = {
    val buffer = new ArrayBuffer[Byte]()
    buffer ++= extraData

    var done = false
    while (!done) {
      receive(socket, ReadBufferSize) match {
        case SocketSuccess(data) =>
          if (data.isEmpty) {
            done = true
          } else {
            buffer ++= data
          }
        case SocketError(_, _) =>
          done = true
      }
    }

    buffer.toArray
  }

  private def findCrlf(data: Array[Byte], start: Int): Int = {
    var i = start
    while (i < data.length - 1) {
      if (data(i) == '\r' && data(i + 1) == '\n') {
        return i
      }
      i += 1
    }
    -1
  }

  private def createRedirectRequest(original: Request, location: String, currentDest: ServerAddress): Request = {
    if (location.startsWith("http://") || location.startsWith("https://")) {
      // Absolute URL
      val newDest = ServerAddress(location)
      original.withUri(extractPath(location)).withDest(newDest)
    } else if (location.startsWith("/")) {
      // Absolute path
      original.withUri(location)
    } else {
      // Relative path
      val basePath = original.uri.lastIndexOf('/') match {
        case -1  => "/"
        case idx => original.uri.substring(0, idx + 1)
      }
      original.withUri(basePath + location)
    }
  }

  private def extractPath(url: String): String = {
    // Extract path from URL like http://host:port/path
    val withoutScheme = url.replaceFirst("^https?://", "")
    val slashIndex = withoutScheme.indexOf('/')
    if (slashIndex < 0) "/" else withoutScheme.substring(slashIndex)
  }

  private def timeoutMillis(d: Duration): Long = {
    if (d.isFinite) d.toMillis else 0L
  }

  private def createException(code: Int, message: String, operation: String): HttpClientException = {
    val status = code match {
      case ETIMEDOUT =>
        HttpStatus.GatewayTimeout_504
      case ECONNREFUSED | ENETUNREACH | EHOSTUNREACH =>
        HttpStatus.ServiceUnavailable_503
      case _ =>
        HttpStatus.BadGateway_502
    }
    new HttpClientException(
      Http.response(status).withContent(s"Socket error during $operation: $message (code: $code)"),
      status,
      s"Socket error during $operation: $message (code: $code)"
    )
  }
}
