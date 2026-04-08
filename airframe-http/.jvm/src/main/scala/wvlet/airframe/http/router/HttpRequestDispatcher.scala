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
import wvlet.airframe.http.*
import wvlet.log.LogSupport

import scala.annotation.tailrec
import scala.concurrent.ExecutionContext
import scala.language.higherKinds

/**
  * Create a filter for dispatching HTTP requests to controller methods with @Endpoint or @RPC annotation
  */
object HttpRequestDispatcher extends LogSupport {

  private[router] case class RouteFilter[Req, Resp, F[_]](filter: HttpFilter[Req, Resp, F], controller: Any)

  case class RoutingTable[Req, Resp, F[_]](
      routeToFilterMappings: Map[Route, RouteFilter[Req, Resp, F]],
      leafFilter: Option[HttpFilter[Req, Resp, F]],
      // Global filter chain extracted from the root of the Router tree.
      // Applied before route matching so filters like CORS can intercept all requests.
      globalFilter: Option[HttpFilter[Req, Resp, F]] = None
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
      codecFactory: MessageCodecFactory,
      executionContext: ExecutionContext
  ): HttpFilter[Req, Resp, F] = {
    // Generate a table for Route -> matching HttpFilter
    val routingTable = buildRoutingTable(backend, session, router, backend.defaultFilter, controllerProvider)

    // Route dispatcher: matches requests to routes and applies route-specific filters
    val routeDispatcher = backend.newFilter { (request: Req, context: HttpContext[Req, Resp, F]) =>
      router.findRoute(request) match {
        case Some(routeMatch) =>
          // Find a filter for the matched route
          val routeFilter = routingTable.findFilter(routeMatch.route)
          // Create a new context for processing the matched route with the controller
          val context =
            new HttpEndpointExecutionContext(
              backend,
              routeMatch,
              responseHandler,
              routeFilter.controller,
              codecFactory,
              executionContext
            )
          val currentService = routeFilter.filter.andThen(context)
          currentService.apply(request)
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

    // Wrap with global filter so it runs before route matching.
    // This allows filters like CORS to intercept requests (e.g., OPTIONS preflight)
    // regardless of whether a route matches.
    routingTable.globalFilter match {
      case Some(gf) => gf.andThen(routeDispatcher)
      case None     => routeDispatcher
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

    def adaptFilter(router: Router): Option[HttpFilter[Req, Resp, F]] =
      router.filterInstance
        .orElse {
          router.filterSurface
            .map(fs => controllerProvider.findController(session, fs))
            .filter(_.isDefined)
            .map(_.get)
        }
        .map {
          case rxFilter: RxHttpFilter =>
            backend.rxFilterAdapter(rxFilter)
          case legacyFilter: HttpFilter[Req, Resp, F] @unchecked =>
            backend.filterAdapter(legacyFilter)
          case other =>
            throw RPCStatus.UNIMPLEMENTED_U8.newException(s"Invalid filter type: ${other}")
        }

    // Extract global filters from the root's linear single-child path.
    // These filters wrap the entire dispatch so they run before route matching.
    val (globalFilter, routeRoot) = {
      @tailrec
      def walk(
          r: Router,
          acc: Option[HttpFilter[Req, Resp, F]]
      ): (Option[HttpFilter[Req, Resp, F]], Router) = {
        val localFilter = adaptFilter(r)
        val newAcc      = localFilter.map(lf => acc.map(_.andThen(lf)).getOrElse(lf)).orElse(acc)

        if (localFilter.isDefined && r.localRoutes.isEmpty && r.children.size == 1) {
          // Filter-only node with single child — extract and continue
          walk(r.children.head, newAcc)
        } else if (localFilter.isDefined) {
          // Filter with routes or branching — extract filter, return node without it
          (newAcc, r.copy(filterInstance = None, filterSurface = None))
        } else if (r.localRoutes.isEmpty && !r.isLeafFilter && r.children.size == 1) {
          // No filter, single child, no routes — skip and continue
          walk(r.children.head, acc)
        } else {
          (acc, r)
        }
      }
      walk(rootRouter, None)
    }

    val leafFilters = Seq.newBuilder[HttpFilter[Req, Resp, F]]

    def buildMappingsFromRouteToFilter(
        router: Router,
        parentFilter: HttpFilter[Req, Resp, F]
    ): Map[Route, RouteFilter[Req, Resp, F]] = {
      val localFilterOpt = adaptFilter(router)

      val currentFilter: HttpFilter[Req, Resp, F] =
        localFilterOpt
          .map { l =>
            parentFilter.andThen(l)
          }
          .getOrElse(parentFilter)

      val m = Map.newBuilder[Route, RouteFilter[Req, Resp, F]]
      for (route <- router.localRoutes) {
        val controllerOpt = router.controllerInstance.orElse {
          controllerProvider.findController(session, route.controllerSurface)
        }
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

    val mappings = buildMappingsFromRouteToFilter(routeRoot, baseFilter)

    val lf = leafFilters.result()
    if (lf.size > 1) {
      warn(s"Multiple leaf filters are found in the router. Using the first one: ${lf.head}")
    }

    RoutingTable(mappings, lf.headOption, globalFilter)
  }

}
