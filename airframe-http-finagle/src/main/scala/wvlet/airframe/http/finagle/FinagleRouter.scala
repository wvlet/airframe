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

import com.twitter.finagle.{Service, SimpleFilter}
import com.twitter.finagle.http.{Request, Response, Status}
import com.twitter.util.Future
import wvlet.airframe.Session
import wvlet.airframe.http.{ControllerProvider, ResponseHandler, Router}
import wvlet.airframe._
import wvlet.airframe.codec.{JSONCodec, MessageCodec, ObjectCodec}
import wvlet.log.LogSupport
import wvlet.surface.Surface

import scala.util.Try

/**
  * A filter for dispatching http requests with Finagle
  */
class FinagleRouter(router: Router, serviceProvider: ControllerProvider, responseHandler: ResponseHandler[Response])
    extends SimpleFilter[Request, Response]
    with LogSupport {

  override def apply(request: Request, service: Service[Request, Response]): Future[Response] = {
    router.findRoute(request) match {
      case Some(route) =>
        route.call(serviceProvider, request) match {
          case None =>
            Future.exception(new IllegalStateException(s"${route.serviceSurface} is not found"))
          case Some(x) =>
            Future.value(responseHandler.toHttpResponse(route.methodSurface.returnType, x))
        }
      case None =>
        service(request)
    }
  }
}

trait FinagleControllerProvider extends ControllerProvider with LogSupport {
  private val session = bind[Session]

  override def find(controllerSurface: Surface): Option[Any] = {
    val v = session.getInstanceOf(controllerSurface)
    Some(v)
  }
}

trait FinagleResponseHandler extends ResponseHandler[Response] {
  def toHttpResponse[A](responseSurface: Surface, a: A): Response = {
    a match {
      case r: Response => r
      case s: String =>
        val r = Response()
        r.contentString = s
        r
      case _ =>
        val rs    = MessageCodec.default.of(responseSurface).asInstanceOf[ObjectCodec[A]]
        val bytes = rs.packAsMapBytes(a)
        val json  = JSONCodec.unpackBytes(bytes)
        json match {
          case Some(j) =>
            val res = Response(Status.Ok)
            res.setContentString(json.get)
            res
          case None =>
            Response(Status.InternalServerError)
        }
    }
  }
}
