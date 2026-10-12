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

/**
  * OpenSSL bindings for HTTPS support in Scala Native.
  *
  * This module provides Scala Native FFI bindings to OpenSSL (libssl + libcrypto), enabling TLS/SSL encrypted
  * communication for HTTPS requests.
  *
  * DESIGN NOTES:
  *
  * This is a placeholder/design document for future HTTPS support. To fully implement HTTPS without libcurl, we need:
  *
  * 1. OpenSSL library bindings (libssl, libcrypto)
  * 2. SSL context creation and configuration
  * 3. Certificate verification
  * 4. SSL handshake integration with socket operations
  * 5. Encrypted read/write operations
  *
  * The implementation would wrap SSL_* functions and integrate with ScalaHttpChannel to provide transparent TLS support.
  *
  * Alternative approaches:
  *   - Use libcurl (current NativeHttpClientBackend) - most portable, handles TLS internally
  *   - Use OpenSSL bindings (this design) - more control, still requires external library
  *   - Pure Scala TLS implementation - complex, not recommended for production
  *
  * Dependencies:
  *   - OpenSSL 1.1.x or 3.x (libssl.so, libcrypto.so)
  *   - System CA certificates for verification
  *
  * Build configuration needed:
  * {{{
  * nativeConfig ~= { _.withLinkingOptions(_ ++ Seq("-lssl", "-lcrypto")) }
  * }}}
  */
object SSLBindings {

  // SSL method types (future implementation)
  // type SSL_METHOD = Ptr[Byte]
  // type SSL_CTX = Ptr[Byte]
  // type SSL = Ptr[Byte]

  /**
    * SSL/TLS initialization status
    */
  sealed trait SSLStatus
  case object SSLNotInitialized extends SSLStatus
  case object SSLInitialized extends SSLStatus
  case class SSLError(message: String) extends SSLStatus

  // Placeholder for future OpenSSL bindings
  // These would be implemented as @extern functions linking to libssl

  /*
  @link("ssl")
  @link("crypto")
  @extern
  object OpenSSLExtern {
    // Library initialization
    @name("OPENSSL_init_ssl")
    def initSSL(opts: CUnsignedLong, settings: Ptr[Byte]): CInt = extern

    // SSL context creation
    @name("TLS_client_method")
    def tlsClientMethod(): SSL_METHOD = extern

    @name("SSL_CTX_new")
    def ctxNew(method: SSL_METHOD): SSL_CTX = extern

    @name("SSL_CTX_free")
    def ctxFree(ctx: SSL_CTX): Unit = extern

    // SSL connection
    @name("SSL_new")
    def sslNew(ctx: SSL_CTX): SSL = extern

    @name("SSL_free")
    def sslFree(ssl: SSL): Unit = extern

    @name("SSL_set_fd")
    def sslSetFd(ssl: SSL, fd: CInt): CInt = extern

    @name("SSL_connect")
    def sslConnect(ssl: SSL): CInt = extern

    // SSL I/O
    @name("SSL_read")
    def sslRead(ssl: SSL, buf: Ptr[Byte], num: CInt): CInt = extern

    @name("SSL_write")
    def sslWrite(ssl: SSL, buf: Ptr[Byte], num: CInt): CInt = extern

    // Certificate verification
    @name("SSL_CTX_set_verify")
    def ctxSetVerify(ctx: SSL_CTX, mode: CInt, callback: Ptr[Byte]): Unit = extern

    @name("SSL_CTX_load_verify_locations")
    def ctxLoadVerifyLocations(ctx: SSL_CTX, caFile: CString, caPath: CString): CInt = extern

    @name("SSL_CTX_set_default_verify_paths")
    def ctxSetDefaultVerifyPaths(ctx: SSL_CTX): CInt = extern

    // Error handling
    @name("SSL_get_error")
    def sslGetError(ssl: SSL, ret: CInt): CInt = extern

    @name("ERR_get_error")
    def errGetError(): CUnsignedLong = extern

    @name("ERR_error_string")
    def errErrorString(e: CUnsignedLong, buf: CString): CString = extern

    // SNI (Server Name Indication)
    @name("SSL_set_tlsext_host_name")
    def sslSetTlsExtHostName(ssl: SSL, name: CString): CInt = extern
  }
  */

  /**
    * Future API for SSL-wrapped socket operations
    *
    * Example usage (design):
    * {{{
    * // Create SSL context
    * val ctx = SSLContext.create()
    *
    * // Wrap existing socket with SSL
    * val sslSocket = SSLSocket.wrap(socket, ctx, hostname)
    *
    * // Use like regular socket
    * sslSocket.send(data)
    * val response = sslSocket.receive(bufferSize)
    *
    * // Cleanup
    * sslSocket.close()
    * ctx.close()
    * }}}
    */
  trait SSLContext extends AutoCloseable {
    def setVerifyPaths(caFile: Option[String], caPath: Option[String]): Unit
    def setDefaultVerifyPaths(): Unit
  }

  trait SSLSocket extends AutoCloseable {
    def send(data: Array[Byte]): SocketBindings.SocketResult[Int]
    def receive(maxBytes: Int): SocketBindings.SocketResult[Array[Byte]]
  }

  /**
    * Factory for creating SSL-enabled HTTP channels (future implementation)
    */
  object SSLHttpChannel {
    /**
      * Check if SSL/TLS support is available at runtime
      */
    def isAvailable: Boolean = {
      // Would check if OpenSSL libraries are loaded
      false
    }
  }
}

/**
  * HTTPS-capable HTTP channel using OpenSSL (future implementation)
  *
  * This class would extend ScalaHttpChannel to add TLS support via OpenSSL bindings. It would:
  * 1. Detect HTTPS scheme and wrap socket with SSL
  * 2. Perform TLS handshake after TCP connection
  * 3. Use SSL_read/SSL_write instead of regular socket I/O
  * 4. Verify server certificates
  * 5. Support SNI (Server Name Indication)
  *
  * Design considerations:
  *   - SSL context should be reusable across connections
  *   - Session resumption for performance
  *   - Certificate chain validation
  *   - Hostname verification
  *   - TLS version negotiation (TLS 1.2, 1.3)
  */
// class SSLHttpChannel(destination: ServerAddress, config: HttpClientConfig)
//   extends ScalaHttpChannel(destination, config) {
//   // Future implementation
// }
