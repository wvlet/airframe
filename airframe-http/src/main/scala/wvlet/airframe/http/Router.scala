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

import wvlet.airframe.surface.{MethodSurface, Surface}
import wvlet.log.LogSupport

import scala.language.experimental.macros

/**
  * Provides mapping from HTTP requests to controller methods (= Route)
  *
  * @param routes
  */
class Router(val routes: Seq[Route]) {

  /**
    * Add methods annotated with @Endpoint to the routing table
    */
  def add[Controller]: Router = macro RouterMacros.add[Controller]

  protected lazy val routeFinder: RouteFinder                  = RouteFinder.build(routes)
  def findRoute[Req](request: HttpRequest[Req]): Option[Route] = routeFinder.findRoute(request)
}

object Router extends LogSupport {
  def empty: Router = Router()
  def of[Controller]: Router = macro RouterMacros.of[Controller]
  def add[Controller]: Router = macro RouterMacros.of[Controller]
  def apply(): Router = new Router(Seq.empty)

  def add(r: Router, controllerSurface: Surface, controllerMethodSurfaces: Seq[MethodSurface]): Router = {
    // Import ReflectSurface to find method annotations (Endpoint)
    import wvlet.airframe.surface.reflect._

    val prefixPath =
      controllerSurface
        .findAnnotationOf[Endpoint]
        .map(_.path())
        .getOrElse("")

    val newRoutes =
      controllerMethodSurfaces
        .map(m => (m, m.findAnnotationOf[Endpoint]))
        .collect {
          case (m: ReflectMethodSurface, Some(endPoint)) =>
            Route(controllerSurface, endPoint.method(), prefixPath + endPoint.path(), m)
        }

    new Router(r.routes ++ newRoutes)
  }
}
