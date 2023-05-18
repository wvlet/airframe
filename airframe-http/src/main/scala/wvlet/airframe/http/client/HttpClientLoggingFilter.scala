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
import wvlet.airframe.http.internal.HttpLogs
import wvlet.airframe.http.{HttpLogger, HttpLoggerConfig, HttpMultiMap, RxHttpEndpoint, RxHttpFilter}
import wvlet.airframe.rx.Rx
import wvlet.log.LogSupport

/**
  * A client-side filter for logging HTTP requests and responses
  */
class HttpClientLoggingFilter(httpLogger: HttpLogger) extends HttpClientFilter with AutoCloseable with LogSupport {

  private val excludeHeaders = HttpMultiMap.fromHeaderNames(httpLogger.config.excludeHeaders)

  override def close(): Unit = {
    httpLogger.close()
  }

  def apply(context: HttpClientContext): RxHttpFilter = new RxHttpFilter {
    override def apply(request: Request, next: RxHttpEndpoint): Rx[Response] = {
      HttpLogs.reportLog(httpLogger, excludeHeaders, request, next, Some(context))
    }
  }
}
