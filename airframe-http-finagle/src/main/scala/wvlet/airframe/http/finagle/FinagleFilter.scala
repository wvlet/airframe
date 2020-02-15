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

import com.twitter.finagle.http.{Request, Response}
import com.twitter.finagle.{Service, SimpleFilter}
import com.twitter.util.Future
import wvlet.airframe.Session
import wvlet.airframe.http.{HttpBackend, HttpFilter}
import wvlet.airframe.http.router.HttpRequestDispatcher

/**
  * An wrapper of HttpFilter for Finagle backend implementation
  */
abstract class FinagleFilter extends HttpFilter[Request, Response, Future] {
  override def backend: HttpBackend[Request, Response, Future] = FinagleBackend
}

class FinagleRouter(session: Session, private[finagle] val config: FinagleServerConfig)
    extends SimpleFilter[Request, Response] {
  private val dispatcher =
    HttpRequestDispatcher.newDispatcher(
      session,
      config.router,
      config.controllerProvider,
      FinagleBackend,
      config.responseHandler
    )

  override def apply(request: Request, service: Service[Request, Response]): Future[Response] = {
    dispatcher.apply(request, FinagleBackend.newContext { request: Request => service(request) })
  }
}
