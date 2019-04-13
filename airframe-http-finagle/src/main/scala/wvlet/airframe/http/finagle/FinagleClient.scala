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

import com.twitter.finagle.{Http, http}
import com.twitter.util.Future
import wvlet.airframe.http.{HttpClient, HttpRequestAdapter, HttpResponseAdapter, ServerAddress}

case class FinagleClientConfig(address: ServerAddress)

class FinagleClient(config: FinagleClientConfig) extends HttpClient[Future, http.Request, http.Response] {

  private val client =
    Http.client
      .newService(config.address.hostAndPort)

  override def request(request: http.Request)(implicit ev: HttpRequestAdapter[http.Request]): Future[http.Response] = {
    client(request)
  }

}

/**
  *
  */
object FinagleClient {

  def newClient(hostAndPort: String): FinagleClient = {
    new FinagleClient(FinagleClientConfig(address = ServerAddress(hostAndPort)))
  }

}
