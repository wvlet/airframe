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
package wvlet.airframe.http

import wvlet.airframe.http.client.{HttpClientBackend, JSHttpClientBackend}
import scala.concurrent.ExecutionContext

/**
  * Scala.js specific implementation
  */
private object Compat extends CompatApi {
  override def urlEncode(s: String): String = {
    scala.scalajs.js.URIUtils.encodeURI(s)
  }
  override def defaultHttpClientBackend: HttpClientBackend = JSHttpClientBackend

  override def defaultExecutionContext: ExecutionContext = {
    org.scalajs.macrotaskexecutor.MacrotaskExecutor.Implicits.global
  }
}
