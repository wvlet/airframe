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

import java.lang.reflect.InvocationTargetException

import wvlet.airframe.Session
import wvlet.airframe.http.Router.RouteFilter
import wvlet.airframe.http._
import wvlet.log.LogSupport

import scala.concurrent.ExecutionContext
import scala.language.higherKinds

/**
  *
  */
object HttpRequestDispatcher {
  def newDispatcher[Req: HttpRequestAdapter, Resp, F[_]](
      session: Session,
      router: Router,
      controllerProvider: ControllerProvider,
      backend: HttpBackend[Req, Resp, F],
      responseHandler: ResponseHandler[Req, Resp]
  ): HttpFilter[Req, Resp, F] = {
    // A table for Route -> matching HttpFilter
    val filterTable: Map[Route, RouteFilter[Req, Resp, F]] = {
      HttpRequestDispatcher.buildRouteFilters(session, router, backend.Identity, controllerProvider)
    }

    backend.newFilter { (request: Req, context: HttpContext[Req, Resp, F]) =>
      router.findRoute(request) match {
        case Some(routeMatch) =>
          // Find a filter for the matched route
          val routeFilter = filterTable(routeMatch.route)
          val context =
            new HttpEndpointExecutionContext(backend, routeMatch, routeFilter.controller, responseHandler)
          val currentService = routeFilter.filter.andThen(context)
          currentService(request)
        case None =>
          // No matching route is found
          context.apply(request)
      }
    }
  }

  /**
    * Traverse the Router tree and build HttpFilter for each local Route
    */
  private[http] def buildRouteFilters[Req, Resp, F[_]](
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
      m ++= buildRouteFilters(session, c, currentFilter, controllerProvider)
    }
    m.result()
  }

  /**
    *  Call a controller method by mapping the request parameters to the method arguments.
    *  This will be the last context after applying preceding filters
    */
  private class HttpEndpointExecutionContext[Req: HttpRequestAdapter, Resp, F[_]](
      backend: HttpBackend[Req, Resp, F],
      routeMatch: RouteMatch,
      controller: Any,
      responseHandler: ResponseHandler[Req, Resp]
  ) extends HttpContext[Req, Resp, F]
      with LogSupport {
    override def apply(request: Req): F[Resp] = {
      val route = routeMatch.route
      val result = {
        // Call the method in this controller
        try {
          route.call(controller, request, routeMatch.params)
        } catch {
          case e: InvocationTargetException =>
            // Return the exception from the target method
            throw e.getTargetException
        }
      }

      route.returnTypeSurface.rawType match {
        // When a return type is Future[X]
        case cl: Class[_] if backend.isFutureType(cl) =>
          // Check the type of X
          val futureValueSurface = route.returnTypeSurface.typeArgs(0)
          futureValueSurface.rawType match {
            // If X is the backend Response type, return as is:
            case valueCls if backend.isRawResponseType(valueCls) =>
              // Use Finagle Future
              result.asInstanceOf[F[Resp]]
            case other =>
              // If X is other type, convert X into an HttpResponse
              backend.mapF(
                result.asInstanceOf[F[_]], { x: Any =>
                  responseHandler.toHttpResponse(request, futureValueSurface, x)
                }
              )
          }
        case cl: Class[_] if backend.isScalaFutureType(cl) =>
          // Check the type of X
          val futureValueSurface = route.returnTypeSurface.typeArgs(0)

          // TODO: Is using global execution a right choice?
          val ex = ExecutionContext.global
          futureValueSurface.rawType match {
            // If X is the backend Response type, return as is:
            case valueCls if backend.isRawResponseType(valueCls) =>
              // Convert Scala Future to Finagle Future
              backend.toFuture(result.asInstanceOf[scala.concurrent.Future[Resp]], ex)
            case other =>
              // If X is other type, convert X into an HttpResponse
              val scalaFuture = result
                .asInstanceOf[scala.concurrent.Future[_]]
                .map { x =>
                  responseHandler.toHttpResponse(request, futureValueSurface, x)
                }(ex)
              backend.toFuture(scalaFuture, ex)
          }
        case _ =>
          // If the route returns non future value, convert it into Future response
          backend.toFuture(responseHandler.toHttpResponse(request, route.returnTypeSurface, result))
      }
    }
  }
}
