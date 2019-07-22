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
class Router(private var _parent: Option[Router] = None,
             val surface: Option[Surface] = None,
             val children: Seq[Router] = Seq.empty,
             val localRoutes: Seq[Route] = Seq.empty,
             val filterSurface: Option[Surface] = None) {

  def parent: Option[Router] = _parent
  def setParent(p: Router): Unit = {
    _parent = Some(p)
  }

  override def toString: String = print(0)

  private def print(indentLevel: Int): String = {
    val s = Seq.newBuilder[String]

    val ws = " " * (indentLevel * 2)
    s += s"${ws}- Router[${surface.orElse(filterSurface).getOrElse("")}]"

    for (r <- localRoutes) {
      s += s"${ws}  + ${r}"
    }
    for (c <- children) {
      s += c.print(indentLevel + 1)
    }
    s.result().mkString("\n")
  }

  def routes: Seq[Route] = {
    localRoutes ++ children.flatMap(_.routes)
  }

  def descendantsAndSelf: Seq[Router] = {
    val lst = Seq.newBuilder[Router]
    lst += this
    for (c <- children) {
      lst ++= c.descendantsAndSelf
    }
    lst.result()
  }

  def ancestorsAndSelf: Seq[Router] = {
    val lst = Seq.newBuilder[Router]
    for (p <- parent) {
      lst ++= p.ancestorsAndSelf
    }
    lst += this
    lst.result()
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
    if (this.children.nonEmpty) {
      throw new IllegalStateException(s"The router ${this.toString} already has a child")
    }
    this.addChild(next)
  }
  def andThen[Controller]: Router = macro RouterMacros.andThen[Controller]

  /**
    * Add a child and and return a new Router with this child node
    *
    * @param childRouter
    * @return
    */
  def addChild(childRouter: Router): Router = {
    val newRoute = new Router(parent, surface, children :+ childRouter, localRoutes, filterSurface)
    newRoute.children.foreach(_.setParent(newRoute))
    newRoute
  }

  def withFilter(newFilterSurface: Surface): Router = {
    val newRoute = new Router(parent, surface, children, localRoutes, Some(newFilterSurface))
    newRoute.children.foreach(_.setParent(newRoute))
    newRoute
  }

  def isEmpty = this eq Router.empty
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
            Route(controllerSurface, endPoint.method(), prefixPath + endPoint.path(), m)
        }

    val newRouter = new Router(surface = Some(controllerSurface), localRoutes = newRoutes)
    if (r.isEmpty) {
      newRouter
    } else {
      r.parent match {
        case Some(p) =>
          p.addChild(newRouter)
        case None =>
          Router.apply(r, newRouter)
      }
    }
  }
}
