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
import wvlet.airframe.http.HttpRequestContext.{RedirectTo, Respond}
import wvlet.airframe.http._
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

  private val filterMap = {
    val m = Map.newBuilder[Route, HttpFilter]
    for (router <- config.router.descendantsAndSelf; route <- router.localRoutes) {
      val parentFilters = router.ancestorsAndSelf.map(_.filterSurface).filter(_.isDefined).map(_.get)

      warn(s"filters for ${router.surface}: ${parentFilters.mkString(" -> ")}")

      val concreteFilters =
        parentFilters
          .map(controllerProvider.findController)
          .filter(_.isDefined)
          .map(_.get.asInstanceOf[HttpFilter])

      if (concreteFilters.nonEmpty) {
        for (r <- router.localRoutes) {
          m += r -> concreteFilters.reduce((a, b) => a.andThen(b))
        }
      }
    }
    m.result()
  }

  override def apply(request: Request, service: Service[Request, Response]): Future[Response] = {
    // TODO Extract this logic into airframe-http

    // Find a route matching to the request
    config.router.findRoute(request) match {
      case Some(routeMatch) =>
        // Process filter
        val requestContext = new HttpRequestContext()
        val filter         = filterMap.get(routeMatch.route)
        val dispatchResult = filter.map(_.beforeFilter(request.toHttpRequest, requestContext))

        val resp = dispatchResult match {
          case Some(RedirectTo(newPath)) =>
            val resp = Response(request)
            resp.statusCode = HttpStatus.TemporaryRedirect_307.code
            resp.location = newPath
            Future.value(resp)
          case Some(Respond(response)) =>
            Future.value(responseHandler.toHttpResponse(request, routeMatch.route.returnTypeSurface, response))
          case other =>
            processRoute(routeMatch, request, service)
        }

        filter match {
          case Some(f) =>
            resp.map { x =>
              f.afterFilter(request, x, requestContext) match {
                case Respond(newResponse) =>
                  // TODO more safe cast
                  newResponse.asInstanceOf[HttpResponse[Response]].toRaw
                case RedirectTo(newPath) =>
                  val newResp = Response(request)
                  newResp.statusCode = HttpStatus.TemporaryRedirect_307.code
                  newResp.location = newPath
                  newResp
                case other =>
                  x
              }
            }
          case None =>
            resp
        }
      case None =>
        // No route is found
        service(request)
    }

  }

  private def processRoute(routeMatch: RouteMatch,
                           request: Request,
                           service: Service[Request, Response]): Future[Response] = {
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
  }

}

/**
  * Converting controller results into finagle http responses.
  */
trait FinagleResponseHandler extends ResponseHandler[Request, Response] {

  // Use Map codecs to create natural JSON responses
  private[this] val mapCodecFactory =
    MessageCodecFactory.defaultFactory.withObjectMapCodec

  // TODO: Extract this logic into airframe-http
  def toHttpResponse[A](request: Request, responseSurface: Surface, a: A): Response = {
    a match {
      case r: Response =>
        // Return the response as is
        r
      case r: SimpleHttpResponse =>
        val resp = Response(request)
        resp.statusCode = r.statusCode
        resp.contentString = r.contentString
        r.contentType.map { c =>
          resp.contentType = c
        }
        resp
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
