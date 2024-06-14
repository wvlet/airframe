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

object JavaHttpClientBackend extends HttpClientBackend {
  private val isJava8: Boolean = sys.props.get("java.version").exists(_.startsWith("1.8"))

  override def newHttpChannel(serverAddress: ServerAddress, config: HttpClientConfig): HttpChannel = {
    // For JDK8, use URLConnectionChannel
    if isJava8 then new URLConnectionChannel(serverAddress, config)
    else
      new JavaHttpClientChannel(serverAddress, config)
  }
}
