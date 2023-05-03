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
package wvlet.airframe.http.internal

import wvlet.airframe.http.{HttpLogger, HttpMessage, HttpMultiMap, RPCContext, RxHttpEndpoint, RxHttpFilter}
import wvlet.airframe.rx.Rx
import wvlet.log.LogSupport

class HttpServerLoggingFilter(httpLogger: HttpLogger) extends RxHttpFilter with LogSupport {
  private val excludeHeaders = HttpMultiMap.fromHeaderNames(httpLogger.config.excludeHeaders)

  override def apply(request: HttpMessage.Request, next: RxHttpEndpoint): Rx[HttpMessage.Response] = {
    val rpcContext = RPCContext.current
    HttpLogs.reportLog(httpLogger, excludeHeaders, request, next, None, Some(rpcContext))
  }
}
