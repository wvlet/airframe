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

  /**
    * We usually need to set a parent Router later after creating its child Router instance
    */
  //private var _parent: Option[Router] = None
  //private var _children: Seq[Router]  = Seq.empty

  def routes: Seq[Route]

  /**
    * A parent router if exists
    */
  def parent: Option[Router]
  def children: Seq[Router]

  def withParent(r: Router): Router
  def withFilter(filterSurface: Surface): Router

  /**
    * Add methods annotated with @Endpoint to the routing table
    */
  def add[Controller]: Router = macro RouterMacros.add[Controller]

  /**
    * Add a child and and return a new Router with this child node
    *
    * @param childRouter
    * @return
    */
  def addChild(childRouter: Router): Router

  def filterSurface: Option[Surface]

  def andThen(next: Router): Router = {
    if (this.children.nonEmpty) {
      throw new IllegalStateException(s"The router ${this.toString} already has a child")
    }
    this.addChild(next)
  }
  def andThen[Controller]: Router = macro RouterMacros.andThen[Controller]

  /**
    * A request filter that will be applied before routing the request to the target method
    */
  private lazy val routeMatcher                                            = RouteMatcher.build(routes)
  def findRoute[Req: HttpRequestAdapter](request: Req): Option[RouteMatch] = routeMatcher.findRoute(request)
}

object Router extends LogSupport {
  def empty: Router   = Router()
  def apply(): Router = RouterNode()

  def apply(children: Router*): Router = {
    val n = new RouterNode()
    children.fold(n)((prev, child) => prev.addChild(child))
  }

  def of[Controller]: Router = macro RouterMacros.of[Controller]
  def add[Controller]: Router = macro RouterMacros.of[Controller]

  def filter[Filter <: HttpFilter]: Router = macro RouterMacros.newFilter[Filter]

  def addInternal(r: Router, controllerSurface: Surface, controllerMethodSurfaces: Seq[MethodSurface]): Router = {
    // Import ReflectSurface to find method annotations (Endpoint)
    import wvlet.airframe.surface.reflect._

    // Get a common prefix of Endpoints if exists
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

    Router(r, new RouterNode(surface = Some(controllerSurface), routes = newRoutes))
  }

  case class RouterNode(parent: Option[Router] = None,
                        surface: Option[Surface] = None,
                        children: Seq[Router] = Seq.empty,
                        routes: Seq[Route] = Seq.empty,
                        override val filterSurface: Option[Surface] = None)
      extends Router {

    override def toString: String = s"Router[${surface.getOrElse("")}]"

    override def addChild(childRouter: Router): Router = {
      new RouterNode(parent, surface, children :+ childRouter.withParent(this), routes, filterSurface)
    }

    def withParent(newParent: Router): Router = {
      new RouterNode(Some(newParent), surface, children, routes, filterSurface)
    }

    def withFilter(newFilterSurface: Surface): Router = {
      new RouterNode(parent, surface, children, routes, Some(newFilterSurface))
    }

  }
}
