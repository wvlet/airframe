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
package wvlet.airframe.http.finagle

/**
  * An entry point for building customized Finagle services
  */
object Finagle {
  @deprecated("Use Http.client", "23.5.0")
  def client: FinagleClientConfig = FinagleClientConfig()
  @deprecated("Use Netty.server in airframe-http-netty instead", "24.1.0")
  def server: FinagleServerConfig = FinagleServerConfig()

  @deprecated("Use Http.client.newAsyncClient", "23.5.0")
  def newClient(hostAndPort: String): FinagleClient = client.newClient(hostAndPort)
  @deprecated("Use Http.client.newSyncClient", "23.5.0")
  def newSyncClient(hostAndPort: String): FinagleSyncClient = client.newSyncClient(hostAndPort)
}
