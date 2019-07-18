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

trait HttpFilter {
  def apply(req: HttpRequest[_], requestContext: HttpRequestContext): DispatchResult

  def andThen(nextFilter: HttpFilter): HttpFilter = {
    HttpFilter.AndThen(this, nextFilter)
  }

  def andThen(router: Router): Router = {
    router.withBeforeFilter(this)
  }
}

object HttpFilter {
  import wvlet.airframe.http.HttpRequestContext._

  def empty: HttpFilter = EmptyFilter

  case object EmptyFilter extends HttpFilter {
    def apply(req: HttpRequest[_], requestContext: HttpRequestContext): DispatchResult = {
      requestContext.nextRoute
    }
  }

  case class AndThen(prev: HttpFilter, next: HttpFilter) extends HttpFilter {
    def apply(req: HttpRequest[_], requestContext: HttpRequestContext): DispatchResult = {
      prev.apply(req, requestContext) match {
        case NextRoute =>
          next.apply(req, requestContext)
        case other => other
      }
    }
  }
}

class HttpRequestContext {
  import HttpRequestContext._

  def redirectTo(path: String): DispatchResult           = RedirectTo(path)
  def respond(response: HttpResponse[_]): DispatchResult = Respond(response)
  def respond[Resp](response: Resp)(implicit adapter: HttpResponseAdapter[Resp]): DispatchResult =
    Respond(adapter.httpResponseOf(response))
  def nextRoute: DispatchResult = NextRoute
}

sealed trait DispatchResult

object HttpRequestContext {
  case class RedirectTo(path: String)           extends DispatchResult
  case class Respond(response: HttpResponse[_]) extends DispatchResult
  case object NextRoute                         extends DispatchResult
}
