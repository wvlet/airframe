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

import wvlet.airframe.http.Router.RouterSeq
import wvlet.airframe.surface.{MethodSurface, Surface}
import wvlet.log.LogSupport

import scala.language.experimental.macros

/**
  * Router defines mappings from HTTP requests to Routes.
  *
  * Router can be nested
  *   - Router1 (parent filter)
  *      - Router2
  *      - Router3
  *   - Router4
  *
  *  When the request is routed to Route2, it will apply the method in each Router:
  *    - Router1.apply
  *    - Router2.apply
  */
trait Router {

  def routes: Seq[Route]

  /**
    * We usually need to set a parent Router later after creating its child Router instance
    */
  private var _parent: Option[Router] = None

  /**
    * A parent router if exists
    */
  def parent: Option[Router] = _parent
  def setParent(r: Router): Unit = {
    _parent = Some(r)
  }

  /**
    * Add methods annotated with @Endpoint to the routing table
    */
  def add[Controller]: Router = macro RouterMacros.add[Controller]
  def add(r: Router): Router = new RouterSeq(Seq(this, r))

  private[http] def getFilterSurface: Option[Surface] = None

  def andThen(r: Router): Router  = ???
  def andThen[Controller]: Router = ???

  /**
    * A request filter that will be applied before routing the request to the target method
    */
  private lazy val routeMatcher                                            = RouteMatcher.build(routes)
  def findRoute[Req: HttpRequestAdapter](request: Req): Option[RouteMatch] = routeMatcher.findRoute(request)
}

object Router extends LogSupport {
  def empty: Router   = Router()
  def apply(): Router = new RouterLeaf(Seq.empty)

  def of[Controller]: Router = macro RouterMacros.of[Controller]
  def add[Controller]: Router = macro RouterMacros.of[Controller]
  def filter[Filter <: HttpFilter]: Router = macro RouterMacros.newFilter[Filter]

  def addInternal(r: Router, controllerSurface: Surface, controllerMethodSurfaces: Seq[MethodSurface]): Router = {
    // Import ReflectSurface to find method annotations (Endpoint)
    import wvlet.airframe.surface.reflect._

    // Get the common prefix of Endpoints
    val prefixPath =
      controllerSurface
        .findAnnotationOf[Endpoint]
        .map(_.path())
        .getOrElse("")

    // Add methods annotated with @Endpoint
    val newRoutes =
      controllerMethodSurfaces
        .map(m => (m, m.findAnnotationOf[Endpoint]))
        .collect {
          case (m: ReflectMethodSurface, Some(endPoint)) =>
            Route(None, controllerSurface, endPoint.method(), prefixPath + endPoint.path(), m)
        }

    new RouterSeq(Seq(r, new RouterLeaf(newRoutes)))
  }

  //def addFilterInternal(r: Router, filterSurface: Surface): Router = {}

  /**
    * A sequence of multiple Routers
    */
  class RouterSeq(routers: Seq[Router]) extends Router {
    def routes: Seq[Route] = routers.flatMap(_.routes)
  }

  class RouterWithFilter(child: Option[Router], filterSurface: Surface) extends Router {
    child.map(_.setParent(this))
    def routes: Seq[Route] = child.map(_.routes).getOrElse(Seq.empty)

    override def getFilterSurface: Option[Surface] = Some(filterSurface)
  }

  /**
    * A leaf router for providing mappings from HTTP requests to controller methods.
    *
    * @param routes
    */
  class RouterLeaf(val routes: Seq[Route]) extends Router {
    routes.foreach(_.setRouter(this))
  }

}
