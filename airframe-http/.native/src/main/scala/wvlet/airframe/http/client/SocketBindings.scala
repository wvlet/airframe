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

import scala.scalanative.unsafe.*
import scala.scalanative.unsigned.*
import scala.scalanative.posix.netdb.*
import scala.scalanative.posix.netdbOps.*
import scala.scalanative.posix.sys.socket.*
import scala.scalanative.posix.sys.socketOps.*
import scala.scalanative.posix.arpa.inet.*
import scala.scalanative.posix.netinet.in.*
import scala.scalanative.posix.netinet.inOps.*
import scala.scalanative.posix.sys.time.*
import scala.scalanative.posix.sys.timeOps.*
import scala.scalanative.posix.unistd
import scala.scalanative.libc.string

/**
  * Pure Scala Native POSIX socket bindings for HTTP communication.
  *
  * This provides direct access to BSD sockets via Scala Native's POSIX bindings, enabling HTTP client implementation
  * without external dependencies like libcurl.
  *
  * Features:
  *   - TCP socket creation and connection
  *   - DNS resolution via getaddrinfo
  *   - Configurable connect and read timeouts
  *   - Blocking I/O for simplicity
  *
  * Limitations:
  *   - No SSL/TLS support (HTTPS requires additional bindings to OpenSSL or similar)
  *   - Blocking operations only (non-blocking would require select/poll/epoll)
  */
object SocketBindings {

  // Socket types
  final val SOCK_STREAM: CInt = 1

  // Protocol families
  final val AF_INET: CInt  = 2
  final val AF_INET6: CInt = 10

  // AI flags for getaddrinfo
  final val AI_PASSIVE: CInt     = 0x0001
  final val AI_CANONNAME: CInt   = 0x0002
  final val AI_NUMERICHOST: CInt = 0x0004

  // Socket options
  final val SOL_SOCKET: CInt   = 1
  final val SO_RCVTIMEO: CInt  = 20
  final val SO_SNDTIMEO: CInt  = 21
  final val SO_KEEPALIVE: CInt = 9
  final val SO_ERROR: CInt     = 4

  // Error codes
  final val EINPROGRESS: CInt   = 115
  final val EWOULDBLOCK: CInt   = 11
  final val ETIMEDOUT: CInt     = 110
  final val ECONNREFUSED: CInt  = 111
  final val ENETUNREACH: CInt   = 101
  final val EHOSTUNREACH: CInt  = 113
  final val ECONNRESET: CInt    = 104
  final val ENOTCONN: CInt      = 107
  final val EPIPE: CInt         = 32

  /**
    * Represents an open socket connection
    */
  class SocketHandle(val fd: CInt) {
    private var closed: Boolean = false

    def isClosed: Boolean = closed

    def close(): Unit = {
      if (!closed) {
        unistd.close(fd)
        closed = true
      }
    }
  }

  /**
    * Result of a socket operation
    */
  sealed trait SocketResult[+A]
  case class SocketSuccess[A](value: A)    extends SocketResult[A]
  case class SocketError(code: Int, message: String) extends SocketResult[Nothing]

  /**
    * Create a TCP socket
    */
  def createSocket(family: CInt = AF_INET): SocketResult[SocketHandle] = {
    val fd = socket(family, SOCK_STREAM, 0)
    if (fd < 0) {
      SocketError(errno, s"Failed to create socket: ${strerror(errno)}")
    } else {
      SocketSuccess(new SocketHandle(fd))
    }
  }

  /**
    * Resolve hostname to IP address and connect
    */
  def connectToHost(host: String, port: Int, connectTimeoutMs: Long): SocketResult[SocketHandle] = {
    Zone.acquire { implicit z =>
      val hints = stackalloc[addrinfo]()
      string.memset(hints.asInstanceOf[Ptr[Byte]], 0, sizeof[addrinfo])
      hints.ai_family = AF_INET
      hints.ai_socktype = SOCK_STREAM

      val result = stackalloc[Ptr[addrinfo]]()

      val hostCStr = toCString(host)
      val portCStr = toCString(port.toString)

      val ret = getaddrinfo(hostCStr, portCStr, hints, result)
      if (ret != 0) {
        return SocketError(ret, s"DNS resolution failed for $host: ${fromCString(gai_strerror(ret))}")
      }

      val addrInfo = !result
      if (addrInfo == null) {
        return SocketError(-1, s"No addresses found for $host")
      }

      try {
        val fd = socket(addrInfo.ai_family, addrInfo.ai_socktype, addrInfo.ai_protocol)
        if (fd < 0) {
          return SocketError(errno, s"Failed to create socket: ${strerror(errno)}")
        }

        val handle = new SocketHandle(fd)

        // Set connect timeout via SO_SNDTIMEO
        if (connectTimeoutMs > 0) {
          setSocketTimeout(handle, SO_SNDTIMEO, connectTimeoutMs)
        }

        val connectResult = connect(fd, addrInfo.ai_addr, addrInfo.ai_addrlen)
        if (connectResult < 0) {
          val err = errno
          handle.close()
          return SocketError(err, s"Failed to connect to $host:$port: ${strerror(err)}")
        }

        SocketSuccess(handle)
      } finally {
        freeaddrinfo(addrInfo)
      }
    }
  }

  /**
    * Set socket timeout (for SO_RCVTIMEO or SO_SNDTIMEO)
    */
  def setSocketTimeout(handle: SocketHandle, option: CInt, timeoutMs: Long): Unit = {
    if (timeoutMs > 0) {
      Zone.acquire { implicit z =>
        val tv = stackalloc[timeval]()
        tv.tv_sec = (timeoutMs / 1000).toSize
        tv.tv_usec = ((timeoutMs % 1000) * 1000).toSize
        setsockopt(handle.fd, SOL_SOCKET, option, tv.asInstanceOf[Ptr[Byte]], sizeof[timeval].toUInt)
      }
    }
  }

  /**
    * Send data through socket
    */
  def sendAll(handle: SocketHandle, data: Array[Byte]): SocketResult[Int] = {
    if (handle.isClosed) {
      return SocketError(ENOTCONN, "Socket is closed")
    }

    Zone.acquire { implicit z =>
      val buffer = alloc[Byte](data.length)
      var i = 0
      while (i < data.length) {
        !(buffer + i) = data(i)
        i += 1
      }

      var totalSent = 0
      while (totalSent < data.length) {
        val remaining = data.length - totalSent
        val sent = send(handle.fd, (buffer + totalSent).asInstanceOf[Ptr[Byte]], remaining.toCSize, 0)
        if (sent < 0) {
          val err = errno
          return SocketError(err, s"Send failed: ${strerror(err)}")
        }
        if (sent == 0.toCSize) {
          return SocketError(EPIPE, "Connection closed by peer")
        }
        totalSent += sent.toInt
      }
      SocketSuccess(totalSent)
    }
  }

  /**
    * Receive data from socket into a byte array
    */
  def receive(handle: SocketHandle, maxBytes: Int): SocketResult[Array[Byte]] = {
    if (handle.isClosed) {
      return SocketError(ENOTCONN, "Socket is closed")
    }

    Zone.acquire { implicit z =>
      val buffer = alloc[Byte](maxBytes)
      val received = recv(handle.fd, buffer.asInstanceOf[Ptr[Byte]], maxBytes.toCSize, 0)

      if (received < 0) {
        val err = errno
        SocketError(err, s"Receive failed: ${strerror(err)}")
      } else if (received == 0.toCSize) {
        SocketSuccess(Array.empty[Byte])
      } else {
        val result = new Array[Byte](received.toInt)
        var i = 0
        while (i < received.toInt) {
          result(i) = !(buffer + i)
          i += 1
        }
        SocketSuccess(result)
      }
    }
  }

  /**
    * Receive exactly n bytes (blocking until complete or error)
    */
  def receiveExact(handle: SocketHandle, n: Int): SocketResult[Array[Byte]] = {
    val result = new Array[Byte](n)
    var totalReceived = 0

    while (totalReceived < n) {
      receive(handle, n - totalReceived) match {
        case SocketSuccess(data) =>
          if (data.isEmpty) {
            return SocketError(ECONNRESET, "Connection closed before receiving all data")
          }
          System.arraycopy(data, 0, result, totalReceived, data.length)
          totalReceived += data.length
        case err: SocketError =>
          return err
      }
    }
    SocketSuccess(result)
  }

  /**
    * Read until a delimiter is found (e.g., "\r\n\r\n" for HTTP headers)
    * Returns the data including the delimiter
    */
  def receiveUntil(handle: SocketHandle, delimiter: Array[Byte], maxBytes: Int): SocketResult[Array[Byte]] = {
    val builder = new scala.collection.mutable.ArrayBuffer[Byte](1024)
    val singleByte = new Array[Byte](1)

    while (builder.length < maxBytes) {
      receive(handle, 1) match {
        case SocketSuccess(data) =>
          if (data.isEmpty) {
            return SocketError(ECONNRESET, "Connection closed before delimiter found")
          }
          builder += data(0)

          // Check if we've received the delimiter
          if (builder.length >= delimiter.length) {
            var matches = true
            var i = 0
            while (i < delimiter.length && matches) {
              if (builder(builder.length - delimiter.length + i) != delimiter(i)) {
                matches = false
              }
              i += 1
            }
            if (matches) {
              return SocketSuccess(builder.toArray)
            }
          }
        case err: SocketError =>
          return err
      }
    }
    SocketError(-1, s"Delimiter not found within $maxBytes bytes")
  }

  // Helper to get errno value
  private def errno: CInt = {
    scala.scalanative.posix.errno.errno
  }

  // Helper to get error string
  private def strerror(err: CInt): String = {
    Zone.acquire { implicit z =>
      fromCString(scala.scalanative.libc.string.strerror(err))
    }
  }
}
