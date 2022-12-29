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
import wvlet.airframe.codec.MessageCodecFactory
import wvlet.airframe.http._
import wvlet.log.LogSupport

import scala.language.higherKinds

/**
  * Create a filter for dispatching HTTP requests to controller methods with @Endpoint or @RPC annotation
  */
object HttpRequestDispatcher extends LogSupport {

  private[router] case class RouteFilter[Req, Resp, F[_]](filter: HttpFilter[Req, Resp, F], controller: Any)

  case class RoutingTable[Req, Resp, F[_]](
      routeToFilterMappings: Map[Route, RouteFilter[Req, Resp, F]],
      leafFilter: Option[HttpFilter[Req, Resp, F]]
  ) {
    def findFilter(route: Route): RouteFilter[Req, Resp, F] = {
      routeToFilterMappings(route)
    }
  }

  def newDispatcher[Req: HttpRequestAdapter, Resp, F[_]](
      session: Session,
      router: Router,
      controllerProvider: ControllerProvider,
      backend: HttpBackend[Req, Resp, F],
      responseHandler: ResponseHandler[Req, Resp],
      codecFactory: MessageCodecFactory
  ): HttpFilter[Req, Resp, F] = {
    // Generate a table for Route -> matching HttpFilter
    val routingTable = buildRoutingTable(backend, session, router, backend.defaultFilter, controllerProvider)

    backend.newFilter { (request: Req, context: HttpContext[Req, Resp, F]) =>
      router.findRoute(request) match {
        case Some(routeMatch) =>
          // Find a filter for the matched route
          val routeFilter = routingTable.findFilter(routeMatch.route)

          // Create a new context for processing the matched route with the controller
          val context =
            new HttpEndpointExecutionContext(backend, routeMatch, responseHandler, routeFilter.controller, codecFactory)
          val currentService = routeFilter.filter.andThen(context)
          currentService(request)
        case None =>
          // If no matching route is found, use the leaf filter if exists
          routingTable.leafFilter match {
            case Some(f) =>
              f.apply(request, context)
            case None =>
              context.apply(request)
          }
      }
    }
  }

  /**
    * Traverse the Router tree and build mappings from local routes to HttpFilters
    */
  private[http] def buildRoutingTable[Req, Resp, F[_]](
      backend: HttpBackend[Req, Resp, F],
      session: Session,
      rootRouter: Router,
      baseFilter: HttpFilter[Req, Resp, F],
      controllerProvider: ControllerProvider
  ): RoutingTable[Req, Resp, F] = {
    val leafFilters = Seq.newBuilder[HttpFilter[Req, Resp, F]]

    def buildMappingsFromRouteToFilter(
        router: Router,
        parentFilter: HttpFilter[Req, Resp, F]
    ): Map[Route, RouteFilter[Req, Resp, F]] = {
      val localFilterOpt: Option[HttpFilter[Req, Resp, F]] =
        router.filterSurface
          .map(fs => controllerProvider.findController(session, fs))
          .filter(_.isDefined)
          .map(_.get.asInstanceOf[HttpFilter[Req, Resp, F]])
          .orElse {
            router.filterInstance
              .asInstanceOf[Option[HttpFilter[Req, Resp, F]]]
          }
          .map { filter =>
            backend.filterAdapter(filter)
          }

      val currentFilter: HttpFilter[Req, Resp, F] =
        localFilterOpt
          .map { l =>
            parentFilter.andThen(l)
          }
          .getOrElse(parentFilter)

      val m = Map.newBuilder[Route, RouteFilter[Req, Resp, F]]
      for (route <- router.localRoutes) {
        val controllerOpt =
          controllerProvider.findController(session, route.controllerSurface)
        if (controllerOpt.isEmpty) {
          throw new IllegalStateException(s"Missing controller. Add ${route.controllerSurface} to the design")
        }
        m += (route -> RouteFilter(currentFilter, controllerOpt.get))
      }
      for (c <- router.children) {
        m ++= buildMappingsFromRouteToFilter(c, currentFilter)
      }
      if (router.isLeafFilter) {
        leafFilters += currentFilter
      }

      m.result()
    }

    val mappings = buildMappingsFromRouteToFilter(rootRouter, baseFilter)

    val lf = leafFilters.result()
    if (lf.size > 1) {
      warn(s"Multiple leaf filters are found in the router. Using the first one: ${lf.head}")
    }

    RoutingTable(mappings, lf.headOption)
  }

}
