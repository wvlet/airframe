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

import com.twitter.finagle.Http
import com.twitter.finagle.http.{Request, Response}
import com.twitter.util.Await
import wvlet.airframe.AirframeSpec
import wvlet.airframe.http._

/**
  *
  */
trait FilterExample {

  @Endpoint(path = "/auth")
  def needsAuth(request: Request): String = {
    "passed"
  }
}

trait NoAuth {
  @Endpoint(path = "/noauth")
  def get = "hello"
}

object AuthFilterExample extends HttpFilter {
  def apply(request: HttpRequest[_], requestContext: HttpRequestContext): DispatchResult = {
    request.header.get("Authorization") match {
      case Some("valid-user") =>
        requestContext.nextRoute
      case _ =>
        requestContext.respond(SimpleHttpResponse(HttpStatus.Forbidden_403, "auth failure"))
    }
  }
}

object BadRequestFilter extends HttpFilter {
  override def apply(req: HttpRequest[_], requestContext: HttpRequestContext): DispatchResult = {
    requestContext.respond()
  }
}

/**
  *
  */
class HttpFilterTest extends AirframeSpec {

  "apply filter before the route" in {
    val routeWithAuth: Router =
      AuthFilterExample andThen
        Router.add[FilterExample]

    val router =
      Router
        .add[NoAuth]
        .add(routeWithAuth)

    val d = newFinagleServerDesign(router).noLifeCycleLogging

    d.build[FinagleServer] { server =>
      val address = server.localAddress

      val client = Http.client.newService(address)

      Await.result(client(Request("/auth")))
    }

  }

}
