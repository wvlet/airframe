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
package wvlet.airframe.http

import wvlet.airframe.codec.{MessageCodec, ParamListCodec}
import wvlet.log.LogSupport
import wvlet.surface
import wvlet.surface.Surface
import wvlet.surface.reflect._

import scala.reflect.runtime.{universe => ru}

class Router(routes: Seq[RequestRoute]) {

  def findRoute(request: HttpRequest): Option[RequestRoute] = {
    routes
      .find { r =>
        r.method == request.method &&
        r.pathComponents.length == request.pathComponents.length &&
        request.path.startsWith(r.pathPrefix)
      }
  }
}

case class RequestRoute(serviceSurface: Surface, method: HttpMethod, path: String, methodSurface: ReflectMethodSurface)
    extends LogSupport {
  require(
    path.startsWith("/"),
    s"Invalid route path: ${path}. EndPoint path must start with a slash (/) in ${methodSurface.owner.name}:${methodSurface.name}")

  lazy val pathComponents: IndexedSeq[String] = {
    path
      .substring(1)
      .split("/")
      .toIndexedSeq
  }

  lazy val pathPrefix: String = {
    "/" +
      pathComponents
        .takeWhile(!_.startsWith(":"))
        .mkString("/")
  }

  def call(controllerProvider: ControllerProvider, request: HttpRequest): Option[Any] = {
    // Resolving path parameter values
    // For example, /user/:id with /user/1 gives { id -> 1 }
    val pathParams = (for ((elem, actual) <- pathComponents.zip(request.pathComponents) if elem.startsWith(":")) yield {
      elem.substring(1) -> actual
    }).toMap[String, String]

    val methodParams = request.query ++ pathParams

    // TODO use Airframe session for find bindings
    val emptyValueFinder = { s: Surface =>
      s.rawType match {
        case c if c == classOf[HttpRequest] =>
          // Bind HttpRequest in the function argument
          request
        case _ =>
          val codec = MessageCodec.default.of(s)
          JSONCodec.request.contentString

          ParamListCodec.defaultEmptyParamBinder(s)
      }
    }

    // TODO initialize MethodCaller outside this function for reuse
    val methodCallBuilder = MethodCaller.of(methodSurface, emptyValueFinder)
    val methodCall        = methodCallBuilder.prepare(methodParams)
    debug(methodCall)

    controllerProvider.find(serviceSurface).map { serviceObj =>
      methodSurface.call(serviceObj, methodCall.paramArgs: _*)
    }
  }
}

case class RouteBuilder(routes: Seq[RequestRoute] = Seq.empty) {

  def build: Router = {
    new Router(routes)
  }

  /**
    * Find methods annotated with [javax.ws.rs.Path]
    */
  def add[A: ru.TypeTag]: RouteBuilder = {
    // Check prefix
    val serviceSurface = surface.of[A]
    val prefixPath =
      serviceSurface
        .findAnnotationOf[Endpoint]
        .map { p =>
          p.path()
        }
        .getOrElse("")

    val newRoutes =
      surface
        .methodsOf[A]
        .map(m => (m, m.findAnnotationOf[Endpoint]))
        .collect {
          case (m: ReflectMethodSurface, Some(endPoint)) =>
            RequestRoute(serviceSurface, endPoint.method(), prefixPath + endPoint.path(), m)
        }

    RouteBuilder(routes ++ newRoutes)
  }

}
