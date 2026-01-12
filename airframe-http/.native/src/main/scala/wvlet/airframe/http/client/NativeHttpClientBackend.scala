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

import wvlet.airframe.http.ServerAddress

/**
  * HTTP client backend for Scala Native using libcurl.
  *
  * This backend provides HTTP client functionality for Scala Native applications by leveraging libcurl. It supports
  * both synchronous and asynchronous (wrapped synchronous) operations.
  *
  * Requirements:
  *   - libcurl 7.56.0 or newer must be installed on the system
  *   - For HTTPS support, libcurl must be built with SSL support (OpenSSL, etc.)
  *
  * Usage: This backend is automatically selected when running on Scala Native. You can also explicitly set it in
  * HttpClientConfig:
  * {{{
  * Http.client
  *   .withBackend(NativeHttpClientBackend)
  *   .newSyncClient("http://example.com")
  * }}}
  */
object NativeHttpClientBackend extends HttpClientBackend {
  import CurlBindings.*
  import scala.scalanative.unsafe.CInt

  // Initialize curl globally when this object is loaded
  // CURL_GLOBAL_DEFAULT initializes both SSL and Win32 sockets (on Windows)
  private val initResult: CInt = curl_global_init(CURL_GLOBAL_DEFAULT)

  /**
    * Check if curl was initialized successfully
    */
  def isInitialized: Boolean = initResult == CURLE_OK

  /**
    * Get the curl version string
    */
  def curlVersion: String = {
    import scala.scalanative.unsafe.*
    fromCString(curl_version())
  }

  override def newHttpChannel(serverAddress: ServerAddress, config: HttpClientConfig): HttpChannel = {
    if (!isInitialized) {
      throw new IllegalStateException(
        s"Failed to initialize libcurl (error code: $initResult). " +
          "Please ensure libcurl is properly installed on your system."
      )
    }
    new CurlChannel(serverAddress, config)
  }

  /**
    * Native platform supports synchronous clients, unlike Scala.js
    */
  override def newSyncClient(serverAddress: ServerAddress, config: HttpClientConfig): SyncClient = {
    new SyncClientImpl(newHttpChannel(serverAddress, config), config)
  }

  /**
    * Async client wraps synchronous operations in Rx for Scala Native. For true asynchronous operations, consider using
    * curl_multi API in future implementations.
    */
  override def newAsyncClient(serverAddress: ServerAddress, config: HttpClientConfig): AsyncClient = {
    new AsyncClientImpl(newHttpChannel(serverAddress, config), config)
  }
}
