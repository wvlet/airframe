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

import wvlet.airframe.http.{HttpBackend, HttpContext, HttpRequestAdapter}
import wvlet.log.LogSupport

import scala.concurrent.ExecutionContext
import scala.language.higherKinds

/**
  *  Create the terminal request handler for processing a method with @EndPoint annotation.
  *
  *  This handler will call a controller method with the request parameters build from the method arguments.
  */
class HttpEndpointExecutionContext[Req: HttpRequestAdapter, Resp, F[_]](
    protected val backend: HttpBackend[Req, Resp, F],
    routeMatch: RouteMatch,
    responseHandler: ResponseHandler[Req, Resp],
    controller: Any
) extends HttpContext[Req, Resp, F]
    with LogSupport {

  override def apply(request: Req): F[Resp] = {
    val route = routeMatch.route
    val result = {
      // Call the method in this controller
      try {
        route.call(controller, request, routeMatch.params, this)
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
