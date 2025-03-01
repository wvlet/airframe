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
import wvlet.airframe.codec.{MessageCodec, MessageCodecFactory}
import wvlet.airframe.control.ThreadUtil
import wvlet.airframe.http.{Http, HttpBackend, HttpContext, HttpMethod, HttpRequestAdapter, HttpStatus, ServerSentEvent}
import wvlet.log.LogSupport

import java.util.concurrent.Executors
import scala.concurrent.ExecutionContext
import scala.language.higherKinds

/**
  * Create the terminal request handler for processing a method with @EndPoint annotation.
  *
  * This handler will call a controller method with the request parameters build from the method arguments.
  */
class HttpEndpointExecutionContext[Req: HttpRequestAdapter, Resp, F[_]](
    protected val backend: HttpBackend[Req, Resp, F],
    routeMatch: RouteMatch,
    responseHandler: ResponseHandler[Req, Resp],
    controller: Any,
    codecFactory: MessageCodecFactory,
    executionContext: ExecutionContext
) extends HttpContext[Req, Resp, F]
    with LogSupport {

  override def apply(request: Req): F[Resp] = {
    val route = routeMatch.route
    val result = {
      // Call the method in this controller
      try {
        route.call(controller, request, routeMatch.params, this, codecFactory)
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
            // Use Backend Future (e.g., Finagle Future or Rx)
            result.asInstanceOf[F[Resp]]
          case valueCls if valueCls == classOf[ServerSentEvent] =>
            // Rx[ServerSentEvent]
            backend.toFuture(responseHandler.toHttpResponse(route, request, route.returnTypeSurface, result))
          case other =>
            // If X is other type, convert X into an HttpResponse
            backend.mapF(
              result.asInstanceOf[F[Resp]],
              { (x: Any) => responseHandler.toHttpResponse(route, request, futureValueSurface, x) }
            )
        }
      case cl: Class[_] if backend.isScalaFutureType(cl) =>
        // Check the type of X
        val futureValueSurface = route.returnTypeSurface.typeArgs(0)

        futureValueSurface.rawType match {
          // If X is the backend Response type, return as is:
          case valueCls if backend.isRawResponseType(valueCls) =>
            // Convert Scala Future to the backend-specific Future
            backend.toFuture(result.asInstanceOf[scala.concurrent.Future[Resp]], executionContext)
          case other =>
            // If X is other type, convert X into an HttpResponse
            val scalaFuture = result
              .asInstanceOf[scala.concurrent.Future[_]]
              .map { x => responseHandler.toHttpResponse(route, request, futureValueSurface, x) }(executionContext)
            backend.toFuture(scalaFuture, executionContext)
        }
      case _ =>
        // If the route returns non future value, convert it into Future response
        backend.toFuture(responseHandler.toHttpResponse(route, request, route.returnTypeSurface, result))
    }
  }
}
