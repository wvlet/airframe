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

import java.lang.reflect.InvocationTargetException

import com.twitter.finagle.http.{Request, Response}
import com.twitter.finagle.{Service, SimpleFilter}
import com.twitter.util.Future
import wvlet.airframe.http._
import wvlet.airframe.http.finagle.FinagleRouter.RouteFilter
import wvlet.log.LogSupport

/**
  * A router for dispatching http requests to the predefined routes.
  */
class FinagleRouter(
    config: FinagleServerConfig,
    controllerProvider: ControllerProvider,
    responseHandler: ResponseHandler[Request, Response]
) extends SimpleFilter[Request, Response]
    with LogSupport {

  // A table for Route -> matching HttpFilter
  private val filterTable: Map[Route, RouteFilter] =
    FinagleRouter.buildRouteFilters(config.router, FinagleFilter.Identity, controllerProvider)

  override def apply(request: Request, service: Service[Request, Response]): Future[Response] = {
    // Find a route matching to the request
    config.router.findRoute(request) match {
      case Some(routeMatch) =>
        val routeFilter    = filterTable(routeMatch.route)
        val context        = new FinagleRouter.FinagleHttpContext(routeMatch, routeFilter.controller, responseHandler)
        val currentService = routeFilter.filter.andThen(context)
        currentService(request)
      case None =>
        // No route is found
        service(request)
    }
  }
}

object FinagleRouter {

  case class RouteFilter(filter: FinagleFilter, controller: Any)

  /**
    * Traverse the Router tree and build HttpFilter for each local Route
    */
  def buildRouteFilters(
      router: Router,
      parentFilter: FinagleFilter,
      controllerProvider: ControllerProvider
  ): Map[Route, RouteFilter] = {

    // TODO Extract this logic into airframe-http
    val localFilterOpt: Option[FinagleFilter] =
      router.filterSurface
        .map(fs => controllerProvider.findController(fs))
        .filter(_.isDefined)
        // TODO convert generic http filter to FinagleFilter
        .map(_.get.asInstanceOf[FinagleFilter])

    val currentFilter: FinagleFilter =
      localFilterOpt
        .map { l =>
          parentFilter.andThen(l)
        }
        .getOrElse(parentFilter)

    val m = Map.newBuilder[Route, RouteFilter]
    for (route <- router.localRoutes) {
      val controller = controllerProvider.findController(route.controllerSurface)
      if (controller.isEmpty) {
        throw new IllegalStateException(s"Missing controller. Add ${route.controllerSurface} to the design")
      }
      m += (route -> RouteFilter(currentFilter, controller.get))
    }
    for (c <- router.children) {
      m ++= buildRouteFilters(c, currentFilter, controllerProvider)
    }
    m.result()
  }

  /**
    *  Call a controller method by mapping the request parameters to the method arguments.
    *  This will be the last context after applying preceding filters
    */
  class FinagleHttpContext(routeMatch: RouteMatch, controller: Any, responseHandler: ResponseHandler[Request, Response])
      extends HttpContext[Request, Response, Future] {
    override def apply(request: Request): Future[Response] = {
      val route = routeMatch.route
      // Call the method in this controller
      val args = route.buildControllerMethodArgs[Request](controller, request, routeMatch.params)
      val result = try {
        route.call(controller, args)
      } catch {
        case e: InvocationTargetException =>
          // Return the exception from the target method
          throw e.getTargetException
      }

      route.returnTypeSurface.rawType match {
        // When a return type is Future[X]
        case f if classOf[Future[_]].isAssignableFrom(f) =>
          // Check the type of X
          val futureValueSurface = route.returnTypeSurface.typeArgs(0)
          futureValueSurface.rawType match {
            // If X is Response type, return as is
            case vc if classOf[Response].isAssignableFrom(vc) =>
              result.asInstanceOf[Future[Response]]
            case other =>
              // If X is other type, convert X into an HttpResponse
              result.asInstanceOf[Future[_]].map { r =>
                responseHandler.toHttpResponse(request, futureValueSurface, r)
              }
          }
        case _ =>
          // If the route returns non future value, convert it into Future response
          Future.value(responseHandler.toHttpResponse(request, route.returnTypeSurface, result))
      }
    }
  }
}
