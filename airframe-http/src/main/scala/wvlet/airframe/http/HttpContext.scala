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
import scala.language.higherKinds

/***
  * Used for passing the subsequent actions to HttpFilter
  */
trait HttpContext[Req, Resp, F[_]] {
  protected def backend: HttpBackend[Req, Resp, F]

  def apply(request: Req): F[Resp]

  // Prepare a thread-local context parameter holder
  def withContextParam(body: => F[Resp]): F[Resp] = {
    backend.withContextParam(body)
  }

  def setContextParam[A](key: String, value: A): Unit = {
    backend.setContextParam(key, value)
  }
  def getContextParam[A](key: String): Option[A] = {
    backend.getContextParam(key)
  }

  private[http] def prependFilter(
      filter: HttpFilter[Req, Resp, F]
  ): HttpContext[Req, Resp, F] = {
    backend.filterAndThenContext(filter, this)
  }
}

object HttpContext {}
