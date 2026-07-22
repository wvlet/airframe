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

import java.nio.charset.StandardCharsets
import scala.collection.mutable.ArrayBuffer

/**
  * Pure Scala HTTP/1.1 protocol implementation.
  *
  * This module provides HTTP/1.1 request serialization and response parsing without external dependencies. It handles:
  *   - Request line formatting (METHOD PATH HTTP/1.1)
  *   - Header serialization and parsing
  *   - Content-Length based body handling
  *   - Chunked transfer encoding
  *   - Connection keep-alive (optional)
  *
  * Limitations:
  *   - Does not support HTTP/2 or HTTP/3
  *   - Does not handle compressed content (gzip/deflate) - caller should decompress
  *   - Does not support request pipelining
  */
object Http11Protocol {

  private val CRLF: Array[Byte]         = "\r\n".getBytes(StandardCharsets.US_ASCII)
  private val HEADER_END: Array[Byte]   = "\r\n\r\n".getBytes(StandardCharsets.US_ASCII)
  private val CHUNKED_END: Array[Byte]  = "0\r\n\r\n".getBytes(StandardCharsets.US_ASCII)

  /**
    * Build an HTTP/1.1 request as a byte array ready to send over the wire.
    *
    * Format:
    * {{{
    * METHOD PATH HTTP/1.1\r\n
    * Host: hostname\r\n
    * Content-Length: N\r\n
    * Header1: Value1\r\n
    * ...\r\n
    * \r\n
    * [body bytes]
    * }}}
    */
  def buildRequest(request: Request, host: String, port: Int): Array[Byte] = {
    val builder = new StringBuilder()

    // Request line
    val path = if (request.uri.startsWith("/")) request.uri else s"/${request.uri}"
    builder.append(request.method).append(" ").append(path).append(" HTTP/1.1\r\n")

    // Host header (required for HTTP/1.1)
    val hostHeader = if (port == 80 || port == 443) host else s"$host:$port"
    if (request.header.get(HttpHeader.Host).isEmpty) {
      builder.append("Host: ").append(hostHeader).append("\r\n")
    }

    // Content-Length for body
    val body = request.contentBytes
    if (body.nonEmpty) {
      if (request.header.get(HttpHeader.ContentLength).isEmpty) {
        builder.append("Content-Length: ").append(body.length).append("\r\n")
      }
    }

    // Connection: keep-alive (optional, HTTP/1.1 default is keep-alive)
    if (request.header.get("Connection").isEmpty) {
      builder.append("Connection: keep-alive\r\n")
    }

    // User-Agent
    if (request.header.get(HttpHeader.UserAgent).isEmpty) {
      builder.append("User-Agent: airframe-http/scala-native\r\n")
    }

    // All other headers
    for (entry <- request.header.entries) {
      builder.append(entry.key).append(": ").append(entry.value).append("\r\n")
    }

    // End of headers
    builder.append("\r\n")

    // Combine header and body
    val headerBytes = builder.toString().getBytes(StandardCharsets.UTF_8)
    if (body.nonEmpty) {
      val result = new Array[Byte](headerBytes.length + body.length)
      System.arraycopy(headerBytes, 0, result, 0, headerBytes.length)
      System.arraycopy(body, 0, result, headerBytes.length, body.length)
      result
    } else {
      headerBytes
    }
  }

  /**
    * Parse HTTP response headers from raw bytes.
    * Returns (status code, headers map, remaining bytes after header section)
    */
  def parseResponseHeaders(headerData: Array[Byte]): (Int, HttpMultiMap, Int) = {
    val headerString = new String(headerData, StandardCharsets.UTF_8)
    val lines = headerString.split("\r\n")

    if (lines.isEmpty) {
      throw new HttpClientException(
        Http.response(HttpStatus.BadGateway_502),
        HttpStatus.BadGateway_502,
        "Invalid HTTP response: empty header"
      )
    }

    // Parse status line: HTTP/1.1 200 OK
    val statusLine = lines.head
    val statusCode = parseStatusLine(statusLine)

    // Parse headers
    val headerBuilder = HttpMultiMap.newBuilder
    var i = 1
    while (i < lines.length) {
      val line = lines(i)
      if (line.nonEmpty) {
        val colonIndex = line.indexOf(':')
        if (colonIndex > 0) {
          val key = line.substring(0, colonIndex).trim
          val value = line.substring(colonIndex + 1).trim
          headerBuilder.add(key, value)
        }
      }
      i += 1
    }

    (statusCode, headerBuilder.result(), 0)
  }

  /**
    * Parse the HTTP status line and return the status code.
    * Example: "HTTP/1.1 200 OK" -> 200
    */
  def parseStatusLine(statusLine: String): Int = {
    // Format: HTTP/1.x STATUS_CODE REASON_PHRASE
    val parts = statusLine.split("\\s+", 3)
    if (parts.length < 2) {
      throw new HttpClientException(
        Http.response(HttpStatus.BadGateway_502),
        HttpStatus.BadGateway_502,
        s"Invalid HTTP status line: $statusLine"
      )
    }

    if (!parts(0).startsWith("HTTP/")) {
      throw new HttpClientException(
        Http.response(HttpStatus.BadGateway_502),
        HttpStatus.BadGateway_502,
        s"Invalid HTTP version: ${parts(0)}"
      )
    }

    try {
      parts(1).toInt
    } catch {
      case _: NumberFormatException =>
        throw new HttpClientException(
          Http.response(HttpStatus.BadGateway_502),
          HttpStatus.BadGateway_502,
          s"Invalid HTTP status code: ${parts(1)}"
        )
    }
  }

  /**
    * Determine how to read the response body based on headers.
    */
  sealed trait BodyReadStrategy
  case class ContentLengthStrategy(length: Long) extends BodyReadStrategy
  case object ChunkedStrategy extends BodyReadStrategy
  case object NoBodyStrategy extends BodyReadStrategy
  case object ReadUntilCloseStrategy extends BodyReadStrategy

  def determineBodyStrategy(headers: HttpMultiMap, method: String, statusCode: Int): BodyReadStrategy = {
    // 1xx, 204, 304 responses have no body
    if (statusCode < 200 || statusCode == 204 || statusCode == 304) {
      return NoBodyStrategy
    }

    // HEAD requests have no body
    if (method == HttpMethod.HEAD) {
      return NoBodyStrategy
    }

    // Check Transfer-Encoding
    headers.get("Transfer-Encoding") match {
      case Some(te) if te.toLowerCase.contains("chunked") =>
        return ChunkedStrategy
      case _ =>
    }

    // Check Content-Length
    headers.get(HttpHeader.ContentLength) match {
      case Some(cl) =>
        try {
          return ContentLengthStrategy(cl.toLong)
        } catch {
          case _: NumberFormatException =>
        }
      case None =>
    }

    // Connection: close means read until connection closes
    headers.get("Connection") match {
      case Some(conn) if conn.toLowerCase == "close" =>
        return ReadUntilCloseStrategy
      case _ =>
    }

    // Default: no body if we can't determine length
    NoBodyStrategy
  }

  /**
    * Parse a chunked transfer encoding body.
    * Format:
    * {{{
    * SIZE_HEX\r\n
    * CHUNK_DATA\r\n
    * SIZE_HEX\r\n
    * CHUNK_DATA\r\n
    * 0\r\n
    * \r\n
    * }}}
    */
  def parseChunkedBody(
      receiveFunc: Int => SocketBindings.SocketResult[Array[Byte]],
      receiveLineFunc: () => SocketBindings.SocketResult[String]
  ): Array[Byte] = {
    val chunks = new ArrayBuffer[Array[Byte]]()
    var totalSize = 0

    var done = false
    while (!done) {
      // Read chunk size line
      receiveLineFunc() match {
        case SocketBindings.SocketSuccess(sizeLine) =>
          val trimmed = sizeLine.trim
          // Chunk size may have extensions after semicolon
          val sizeStr = trimmed.split(";").head.trim
          if (sizeStr.isEmpty) {
            throw new HttpClientException(
              Http.response(HttpStatus.BadGateway_502),
              HttpStatus.BadGateway_502,
              "Invalid chunked encoding: empty size line"
            )
          }

          val chunkSize = try {
            java.lang.Long.parseLong(sizeStr, 16).toInt
          } catch {
            case _: NumberFormatException =>
              throw new HttpClientException(
                Http.response(HttpStatus.BadGateway_502),
                HttpStatus.BadGateway_502,
                s"Invalid chunk size: $sizeStr"
              )
          }

          if (chunkSize == 0) {
            done = true
            // Read trailing CRLF
            receiveLineFunc()
          } else {
            // Read chunk data
            receiveFunc(chunkSize) match {
              case SocketBindings.SocketSuccess(data) =>
                chunks += data
                totalSize += data.length
                // Read trailing CRLF after chunk
                receiveLineFunc()
              case SocketBindings.SocketError(code, msg) =>
                throw new HttpClientException(
                  Http.response(HttpStatus.BadGateway_502),
                  HttpStatus.BadGateway_502,
                  s"Failed to read chunk data: $msg"
                )
            }
          }

        case SocketBindings.SocketError(code, msg) =>
          throw new HttpClientException(
            Http.response(HttpStatus.BadGateway_502),
            HttpStatus.BadGateway_502,
            s"Failed to read chunk size: $msg"
          )
      }
    }

    // Combine all chunks
    val result = new Array[Byte](totalSize)
    var offset = 0
    for (chunk <- chunks) {
      System.arraycopy(chunk, 0, result, offset, chunk.length)
      offset += chunk.length
    }
    result
  }

  /**
    * Find the end of HTTP headers in a byte buffer.
    * Returns the index after "\r\n\r\n" or -1 if not found.
    */
  def findHeaderEnd(data: Array[Byte], startIndex: Int): Int = {
    var i = startIndex
    while (i <= data.length - 4) {
      if (data(i) == '\r' && data(i + 1) == '\n' && data(i + 2) == '\r' && data(i + 3) == '\n') {
        return i + 4
      }
      i += 1
    }
    -1
  }

  /**
    * Build an HTTP response from parsed components.
    */
  def buildResponse(statusCode: Int, headers: HttpMultiMap, body: Array[Byte]): Response = {
    val status = HttpStatus.ofCode(statusCode)
    Http.response(status)
      .withHeader(headers)
      .withContent(body)
  }
}
