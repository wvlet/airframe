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
import wvlet.airframe.http.RxHttpEndpoint
import wvlet.airframe.rx.{Rx, RxEvent}

trait HttpClientContext extends RxHttpEndpoint {
  private var props = Map.empty[String, Any]
  def setProperty(key: String, value: Any): Unit = {
    props += key -> value
  }
  def getProperty(key: String): Option[Any] = {
    props.get(key)
  }
}

object HttpClientContext {
  def passThroughChannel(channel: HttpChannel, config: HttpClientConfig): HttpClientContext = new HttpClientContext {
    override def apply(request: Request): Rx[Response] = {
      channel.sendAsync(request, config)
    }
  }

}
