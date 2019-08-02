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
  *   - Router1 with Filter1
  *      - Router2: endpoints e1, e2
  *      - Router3: endpoints e3 with Filter2
  *   - Router4: endpoints e4
  *
  * From this router definition, the backend HTTP server specific implementation will build a mapping table like this:
  *   e1 -> Filter1 andThen process(e1)
  *   e2 -> Filter1 andThen process(e2)
  *   e3 -> Filter1 andThen Filter2 andThen process(e3)
  *   e4 -> process(e4)
  *
  */
case class Router(surface: Option[Surface] = None,
                  children: Seq[Router] = Seq.empty,
                  localRoutes: Seq[Route] = Seq.empty,
                  filterSurface: Option[Surface] = None) {
  def isEmpty = this eq Router.empty

  def routes: Seq[Route] = {
    localRoutes ++ children.flatMap(_.routes)
  }

  override def toString: String = printNode(0)

  private def printNode(indentLevel: Int): String = {
    val s = Seq.newBuilder[String]

    val ws = " " * (indentLevel * 2)
    s += s"${ws}- Router[${surface.orElse(filterSurface).getOrElse("")}]"

    for (r <- localRoutes) {
      s += s"${ws}  + ${r}"
    }
    for (c <- children) {
      s += c.printNode(indentLevel + 1)
    }
    s.result().mkString("\n")
  }

  /**
    * A request filter that will be applied before routing the request to the target method
    */
  private lazy val routeMatcher                                            = RouteMatcher.build(routes)
  def findRoute[Req: HttpRequestAdapter](request: Req): Option[RouteMatch] = routeMatcher.findRoute(request)

  /**
    * Add methods annotated with @Endpoint to the routing table
    */
  def add[Controller]: Router = macro RouterMacros.add[Controller]

  def andThen(next: Router): Router = {
    this.children.size match {
      case 0 =>
        this.addChild(next)
      case 1 =>
        new Router(surface, Seq(children(0).andThen(next)), localRoutes, filterSurface)
      case _ =>
        throw new IllegalStateException(s"The router ${this.toString} already has multiple child routers")
    }
  }

  def andThen[Controller]: Router = macro RouterMacros.andThen[Controller]

  /**
    * Add a child and and return a new Router with this child node
    *
    * @param childRouter
    * @return
    */
  def addChild(childRouter: Router): Router = {
    new Router(surface, children :+ childRouter, localRoutes, filterSurface)
  }

  def withFilter(newFilterSurface: Surface): Router = {
    new Router(surface, children, localRoutes, Some(newFilterSurface))
  }

  /**
    * Internal only method for adding the surface of the controller
    */
  def addInternal(controllerSurface: Surface, controllerMethodSurfaces: Seq[MethodSurface]): Router = {
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
            Route(controllerSurface, endPoint.method(), prefixPath + endPoint.path(), m)
        }

    val newRouter = new Router(surface = Some(controllerSurface), localRoutes = newRoutes)
    if (this.isEmpty) {
      newRouter
    } else {
      Router.apply(this, newRouter)
    }
  }
}

object Router extends LogSupport {
  val empty: Router   = new Router()
  def apply(): Router = empty

  def apply(children: Router*): Router = {
    if (children == null) {
      empty
    } else {
      children.fold(empty)((prev, child) => prev.addChild(child))
    }
  }

  def of[Controller]: Router = macro RouterMacros.of[Controller]
  def add[Controller]: Router = macro RouterMacros.of[Controller]

  @deprecated(message = "Use Router.add or Router.of instead", since = "19.8.0")
  def filter[Filter <: HttpFilterType]: Router = macro RouterMacros.of[Filter]

}
