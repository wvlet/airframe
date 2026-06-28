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
  * Pure Scala HTTP client backend for Scala Native.
  *
  * This backend provides zero-dependency HTTP client functionality using direct POSIX socket bindings. It's suitable for
  * environments where libcurl is not available or when a minimal dependency footprint is required.
  *
  * Features:
  *   - Zero external dependencies (uses only Scala Native POSIX bindings)
  *   - Full HTTP/1.1 support
  *   - Connection keep-alive
  *   - Automatic redirect following
  *   - Configurable timeouts
  *
  * Limitations:
  *   - HTTP only (no HTTPS/TLS support)
  *   - Synchronous operations only (async wraps sync)
  *   - No HTTP/2 support
  *
  * For HTTPS support, use [[NativeHttpClientBackend]] which requires libcurl.
  *
  * Usage:
  * {{{
  * // For HTTP-only requests
  * Http.client
  *   .withBackend(ScalaHttpClientBackend)
  *   .newSyncClient("http://example.com")
  *
  * // For HTTPS requests, use NativeHttpClientBackend (requires libcurl)
  * Http.client
  *   .withBackend(NativeHttpClientBackend)
  *   .newSyncClient("https://example.com")
  * }}}
  */
object ScalaHttpClientBackend extends HttpClientBackend {

  override def newHttpChannel(serverAddress: ServerAddress, config: HttpClientConfig): HttpChannel = {
    // Validate that we're not trying to use HTTPS
    if (serverAddress.scheme == "https") {
      throw new IllegalArgumentException(
        "ScalaHttpClientBackend does not support HTTPS. " +
          "Use NativeHttpClientBackend (requires libcurl) for HTTPS support, " +
          "or use HTTP for this backend."
      )
    }
    new ScalaHttpChannel(serverAddress, config)
  }

  override def newSyncClient(serverAddress: ServerAddress, config: HttpClientConfig): SyncClient = {
    new SyncClientImpl(newHttpChannel(serverAddress, config), config)
  }

  override def newAsyncClient(serverAddress: ServerAddress, config: HttpClientConfig): AsyncClient = {
    new AsyncClientImpl(newHttpChannel(serverAddress, config), config)
  }
}
