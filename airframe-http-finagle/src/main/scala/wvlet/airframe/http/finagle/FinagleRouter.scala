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

import com.twitter.finagle.{Service, SimpleFilter}
import com.twitter.finagle.http.{Request, Response}
import com.twitter.util.Future
import wvlet.airframe.http.{Router, ServiceProvider, ServiceResponseHandler}

/**
  * A filter for dispatching http requests with Finagle
  */
class FinagleRouter(router: Router, serviceProvider: ServiceProvider, responseHandler: ServiceResponseHandler[Response])
    extends SimpleFilter[Request, Response] {

  override def apply(request: Request, service: Service[Request, Response]): Future[Response] = {
    router.findRoute(request) match {
      case Some(route) =>
        route.call(serviceProvider, request) match {
          case None =>
            Future.exception(new IllegalStateException(s"${route.serviceSurface} is not found"))
          case Some(x) =>
            Future.value(responseHandler.toHttpResponse(x))
        }
      case None =>
        service(request)
    }
  }
}
