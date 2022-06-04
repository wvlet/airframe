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
import wvlet.airframe.http.HttpMessage
import wvlet.log.LogSupport

import scala.concurrent.Future

class ClientLoggingFilter extends ClientFilter with LogSupport {
  override def chain(req: HttpMessage.Request, context: ClientContext): HttpMessage.Response = {
    info(req)
    info(req.header)
    try {
      val resp = context.chain(req)
      info(resp)
      info(resp.header)
      resp
    } catch {
      case e: Throwable =>
        warn(e)
        throw e
    }
  }

  override def chainAsync(req: HttpMessage.Request, context: ClientContext): Future[HttpMessage.Response] = {
    context.chainAsync(req)
  }
}
