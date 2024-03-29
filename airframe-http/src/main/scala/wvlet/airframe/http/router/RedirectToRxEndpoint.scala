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
package wvlet.airframe.http.router

import wvlet.airframe.http.{Endpoint, HttpMessage, HttpMethod, RPCContext, RxHttpEndpoint}
import wvlet.airframe.rx.Rx
import wvlet.log.LogSupport

/**
  * An Http endpoint definition for bypassing the request to the given endpoint
  * @param endpoint
  */
class RedirectToRxEndpoint(endpoint: RxHttpEndpoint) extends LogSupport {
  @Endpoint(path = "/*path", method = HttpMethod.GET)
  def get(): Rx[HttpMessage.Response] = process()

  @Endpoint(path = "/*path", method = HttpMethod.POST)
  def post(): Rx[HttpMessage.Response] = process()

  @Endpoint(path = "/*path", method = HttpMethod.PUT)
  def put(): Rx[HttpMessage.Response] = process()

  @Endpoint(path = "/*path", method = HttpMethod.HEAD)
  def head(): Rx[HttpMessage.Response] = process()

  @Endpoint(path = "/*path", method = HttpMethod.PATCH)
  def patch(): Rx[HttpMessage.Response] = process()

  @Endpoint(path = "/*path", method = HttpMethod.DELETE)
  def delete(): Rx[HttpMessage.Response] = process()

  @Endpoint(path = "/*path", method = HttpMethod.OPTIONS)
  def options(): Rx[HttpMessage.Response] = process()

  @Endpoint(path = "/*path", method = HttpMethod.TRACE)
  def trace(): Rx[HttpMessage.Response] = process()

  private def process(): Rx[HttpMessage.Response] = {
    val req = RPCContext.current.httpRequest
    endpoint.apply(req)
  }
}
