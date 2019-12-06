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
package wvlet.airframe.http.router

import wvlet.airframe.Session
import wvlet.airframe.http._
import wvlet.log.LogSupport

import scala.language.higherKinds

/**
  * Create a filter for dispatching HTTP requests to controller methods with @Endpoint annotation
  */
object HttpRequestDispatcher extends LogSupport {

  private[router] case class RouteFilter[Req, Resp, F[_]](filter: HttpFilter[Req, Resp, F], controller: Any)

  def newDispatcher[Req: HttpRequestAdapter, Resp, F[_]](
      session: Session,
      router: Router,
      controllerProvider: ControllerProvider,
      backend: HttpBackend[Req, Resp, F],
      responseHandler: ResponseHandler[Req, Resp]
  ): HttpFilter[Req, Resp, F] = {
    // A table for Route -> matching HttpFilter
    val routeToFilterMappings: Map[Route, RouteFilter[Req, Resp, F]] = {
      HttpRequestDispatcher.buildMappingsFromRouteToFilter(
        session,
        router,
        backend.defaultFilter,
        controllerProvider
      )
    }

    backend.newFilter { (request: Req, context: HttpContext[Req, Resp, F]) =>
      router.findRoute(request) match {
        case Some(routeMatch) =>
          // Find a filter for the matched route
          val routeFilter = routeToFilterMappings(routeMatch.route)
          // Create a new context for processing the matched route with the controller
          val context =
            new HttpEndpointExecutionContext(backend, routeMatch, responseHandler, routeFilter.controller)
          val currentService = routeFilter.filter.andThen(context)
          currentService(request)
        case None =>
          // No matching route is found
          context.apply(request)
      }
    }
  }

  /**
    * Traverse the Router tree and build mappings from local routes to HttpFilters
    */
  private[http] def buildMappingsFromRouteToFilter[Req, Resp, F[_]](
      session: Session,
      router: Router,
      parentFilter: HttpFilter[Req, Resp, F],
      controllerProvider: ControllerProvider
  ): Map[Route, RouteFilter[Req, Resp, F]] = {
    val localFilterOpt: Option[HttpFilter[Req, Resp, F]] =
      router.filterSurface
        .map(fs => controllerProvider.findController(session, fs))
        .filter(_.isDefined)
        .map(_.get.asInstanceOf[HttpFilter[Req, Resp, F]])

    val currentFilter: HttpFilter[Req, Resp, F] =
      localFilterOpt
        .map { l =>
          parentFilter.andThen(l)
        }
        .getOrElse(parentFilter)

    val m = Map.newBuilder[Route, RouteFilter[Req, Resp, F]]
    for (route <- router.localRoutes) {
      val controllerOpt = controllerProvider.findController(session, route.controllerSurface)
      if (controllerOpt.isEmpty) {
        throw new IllegalStateException(s"Missing controller. Add ${route.controllerSurface} to the design")
      }
      m += (route -> RouteFilter(currentFilter, controllerOpt.get))
    }
    for (c <- router.children) {
      m ++= buildMappingsFromRouteToFilter(session, c, currentFilter, controllerProvider)
    }
    m.result()
  }

}
