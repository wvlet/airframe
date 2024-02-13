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

import wvlet.airframe.http.HttpMessage.{Request, Response}
import wvlet.airframe.http.{Compat, HttpMessage, HttpMethod, ServerAddress}
import wvlet.airframe.rx.Rx
import wvlet.log.LogSupport

import scala.concurrent.{ExecutionContext, Promise}

/**
  * An http channel implementation based on Fetch API
  * https://developer.mozilla.org/en-US/docs/Web/API/Fetch_API/Using_Fetch
  * @param serverAddress
  * @param config
  */
class JSFetchChannel(serverAddress: ServerAddress, config: HttpClientConfig) extends HttpChannel with LogSupport {

  private[client] implicit val executionContext: ExecutionContext = Compat.defaultExecutionContext

  override def close(): Unit = {
    // nothing to do
  }

  override def send(req: HttpMessage.Request, channelConfig: HttpChannelConfig): HttpMessage.Response = {
    ???
  }

  override def sendAsync(request: HttpMessage.Request, channelConfig: HttpChannelConfig): Rx[HttpMessage.Response] = {
    val path = if (request.uri.startsWith("/")) request.uri else s"/${request.uri}"
    val uri  = s"${serverAddress.uri}${path}"

    val requestInit = new org.scalajs.dom.RequestInit {
      method = request.method match {
        case HttpMethod.GET => org.scalajs.dom.HttpMethod.GET
      }

    }

    val fetchRequest = new org.scalajs.dom.Request(uri, requestInit)

    val promise = Promise[HttpMessage.Response]()

//    val response = org.scalajs.dom.window.fetch(jsRequest).toFuture
//    val result   = org.scalajs.dom.ext.Await.result(response, config.responseTimeout)
//    val status   = result.status
//    val headers  = result.headers.toMap.map { case (k, v) => k -> v.split(",").toSeq }
//    val body     = org.scalajs.dom.ext.Await.result(result.text(), config.responseTimeout)
//
//    Response(status, headers, body)

    Rx.future(promise.future)
  }

}
