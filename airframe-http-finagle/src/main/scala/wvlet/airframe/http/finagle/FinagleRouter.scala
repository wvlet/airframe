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
import com.twitter.util.Future
import wvlet.airframe.codec.{JSONCodec, MessageCodec, MessageCodecFactory, ObjectCodec}
import wvlet.airframe.http.{ControllerProvider, ResponseHandler, Router}
import wvlet.log.LogSupport
import wvlet.surface.Surface

/**
  * A filter for dispatching http requests to the predefined routes with Finagle
  */
class FinagleRouter(router: Router,
                    controllerProvider: ControllerProvider,
                    responseHandler: ResponseHandler[Request, Response])
    extends SimpleFilter[Request, Response]
    with LogSupport {

  override def apply(request: Request, service: Service[Request, Response]): Future[Response] = {
    // Find a route matching to the request
    router.findRoute(request) match {
      case Some(route) =>
        // Find the corresponding controller
        controllerProvider.findController(route.controllerSurface) match {
          case Some(controller) =>
            // Call the method in this controller
            val args   = route.buildControllerMethodArgs(request)
            val result = route.call(controller, args)
            Future.value(responseHandler.toHttpResponse(request, route.returnTypeSurface, result))
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
    MessageCodec.defautlFactory.withObjectMapCodec

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
        val bytes: Array[Byte] = rs match {
          case m: MessageCodec[_] =>
            m.asInstanceOf[MessageCodec[A]].toMsgPack(a)
          case _ =>
            throw new IllegalArgumentException(s"Unknown codec: ${rs}")
        }

        // TODO return application/msgpack content type
        val json = JSONCodec.unpackMsgPack(bytes)
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
