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
import wvlet.airframe.http.{ServerAddress}

/**
  */
@deprecated("This backend will be used automatically for Java8. No need to explicitly set this backend", "24.4.0")
object URLConnectionClientBackend extends HttpClientBackend {
  override def newHttpChannel(serverAddress: ServerAddress, config: HttpClientConfig): HttpChannel = {
    new URLConnectionChannel(serverAddress, config)
  }
}
