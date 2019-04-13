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

import com.twitter.finagle.http.Response
import com.twitter.finagle.service.{ReqRep, ResponseClass}
import com.twitter.finagle.{Http, http}
import com.twitter.util.{Future, Return, Throw}
import wvlet.airframe.control.ResultClass
import wvlet.airframe.control.ResultClass.{Failed, Successful}
import wvlet.airframe.http.{HttpClient, HttpClientException, HttpRequestAdapter, HttpResponseAdapter, ServerAddress}

case class FinagleClientConfig(address: ServerAddress)

class FinagleClient(config: FinagleClientConfig) extends HttpClient[Future, http.Request, http.Response] {
  import FinagleClient._
  private val client =
    Http.client
      .withResponseClassifier {
        case ReqRep(_, Return(r: Response)) =>
          toFinagleResponseClassifier(HttpClientException.defaultResponseClassifier(r))
        case ReqRep(_, Throw(ex)) =>
          toFinagleResponseClassifier(HttpClientException.defaultClientExceptionClassifier(ex))
      }
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

  private[finagle] def toFinagleResponseClassifier(cls: ResultClass): ResponseClass = {
    cls match {
      case Successful =>
        ResponseClass.Success
      case Failed(isRetryable) =>
        if (isRetryable) {
          ResponseClass.RetryableFailure
        } else {
          ResponseClass.NonRetryableFailure
        }
    }
  }
}
