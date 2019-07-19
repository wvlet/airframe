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

/**
  * A filter interface to define actions before/after handling HTTP requests
  */
trait HttpFilter {
  // A filter applied before processing the request
  def beforeFilter(req: HttpRequest[_], requestContext: HttpRequestContext): DispatchResult = {
    requestContext.nextRoute
  }

  // A filter applied after processing the request
  def afterFilter(request: HttpRequest[_],
                  response: HttpResponse[_],
                  requestContext: HttpRequestContext): DispatchResult = {
    requestContext.respond(response)
  }

  // Inject another filter:
  // current before filter -> next before filter -> next after filter -> current after filter
  def andThen(nextFilter: HttpFilter): HttpFilter = {
    HttpFilter.AndThen(this, nextFilter)
  }
}

object HttpFilter {
  import wvlet.airframe.http.HttpRequestContext._

  def empty: HttpFilter = EmptyFilter

  case object EmptyFilter extends HttpFilter

  case class AndThen(prev: HttpFilter, next: HttpFilter) extends HttpFilter {
    override def beforeFilter(req: HttpRequest[_], requestContext: HttpRequestContext): DispatchResult = {
      prev.beforeFilter(req, requestContext) match {
        case NextRoute =>
          next.beforeFilter(req, requestContext)
        case other =>
          next.beforeFilter(req, requestContext)
          other
      }
    }

    override def afterFilter(request: HttpRequest[_],
                             response: HttpResponse[_],
                             requestContext: HttpRequestContext): DispatchResult = {
      next.afterFilter(request, response, requestContext) match {
        case NextRoute =>
          prev.afterFilter(request, response, requestContext)
        case Respond(newResponse) =>
          prev.afterFilter(request, newResponse, requestContext)
        case other =>
          prev.afterFilter(request, response, requestContext)
          other
      }
    }
  }
}

/***
  * Used for telling the next action to Router
  */
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
