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

import com.twitter.finagle.http.{Request, Response, Status}
import com.twitter.finagle.{Service, SimpleFilter}
import com.twitter.io.Buf.ByteArray
import com.twitter.util.Future
import wvlet.airframe.codec.{JSONCodec, MessageCodec, MessageCodecFactory}
import wvlet.airframe.http.{ControllerProvider, ResponseHandler}
import wvlet.airframe.surface.Surface
import wvlet.log.LogSupport

/**
  * A filter for dispatching http requests to the predefined routes with Finagle
  */
class FinagleRouter(config: FinagleServerConfig,
                    controllerProvider: ControllerProvider,
                    responseHandler: ResponseHandler[Request, Response])
    extends SimpleFilter[Request, Response]
    with LogSupport {

  override def apply(request: Request, service: Service[Request, Response]): Future[Response] = {
    // Find a route matching to the request
    config.router.findRouteMatch(request) match {
      case Some(routeMatch) =>
        val route = routeMatch.route
        // Find a corresponding controller
        controllerProvider.findController(route.controllerSurface) match {
          case Some(controller) =>
            // Call the method in this controller
            val args   = route.buildControllerMethodArgs(controller, request, routeMatch.params)
            val result = route.call(controller, args)

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
          case None =>
            Future.exception(new IllegalStateException(s"Controller ${route.controllerSurface} is not found"))
        }
      case None =>
        // No route is found
        service(request)
    }
  }
}

/**
  * Converting controller results into finagle http responses.
  */
trait FinagleResponseHandler extends ResponseHandler[Request, Response] {

  // Use Map codecs to create natural JSON responses
  private[this] val mapCodecFactory =
    MessageCodecFactory.defaultFactory.withObjectMapCodec

  def toHttpResponse[A](request: Request, responseSurface: Surface, a: A): Response = {
    a match {
      case r: Response =>
        // Return the response as is
        r
      case s: String =>
        val r = Response()
        r.contentString = s
        r
      case _ =>
        // Convert the response object into JSON
        val rs = mapCodecFactory.of(responseSurface)
        val msgpack: Array[Byte] = rs match {
          case m: MessageCodec[_] =>
            m.asInstanceOf[MessageCodec[A]].toMsgPack(a)
          case _ =>
            throw new IllegalArgumentException(s"Unknown codec: ${rs}")
        }

        // TODO return application/msgpack content type
        if (request.accept.contains("application/x-msgpack")) {
          val res = Response(Status.Ok)
          res.contentType = "application/x-msgpack"
          res.content = ByteArray.Owned(msgpack)
          res
        } else {
          val json = JSONCodec.unpackMsgPack(msgpack)
          json match {
            case Some(j) =>
              val res = Response(Status.Ok)
              res.setContentTypeJson()
              res.setContentString(json.get)
              res
            case None =>
              Response(Status.InternalServerError)
          }
        }
    }
  }
}
