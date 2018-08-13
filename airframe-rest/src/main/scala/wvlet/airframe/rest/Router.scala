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
package wvlet.airframe.rest

import javax.ws.rs.{DELETE, GET, POST, PUT}
import wvlet.surface
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

case class RequestRoute(method: HttpMethod, path: String, methodSurface: ReflectMethodSurface) {
  require(
    path.startsWith("/"),
    s"Invalid route path: ${path}. @Path must start with a slash (/) in ${methodSurface.owner.name}:${methodSurface.name}")

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
    val prefixPath =
      surface
        .of[A]
        .findAnnotationOf[javax.ws.rs.Path]
        .map { p =>
          p.value()
        }
        .getOrElse("")

    val newRoutes =
      surface
        .methodsOf[A]
        .map(m => (m, m.findAnnotationOf[javax.ws.rs.Path]))
        .collect {
          case (m: ReflectMethodSurface, Some(path)) =>
            val method =
              m match {
                case m if m.findAnnotationOf[GET].isDefined =>
                  HttpMethod.GET
                case m if m.findAnnotationOf[POST].isDefined =>
                  HttpMethod.POST
                case m if m.findAnnotationOf[DELETE].isDefined =>
                  HttpMethod.DELETE
                case m if m.findAnnotationOf[PUT].isDefined =>
                  HttpMethod.PUT
                case _ =>
                  HttpMethod.GET
              }
            RequestRoute(method, prefixPath + path.value(), m)
        }

    RouteBuilder(routes ++ newRoutes)
  }

}
