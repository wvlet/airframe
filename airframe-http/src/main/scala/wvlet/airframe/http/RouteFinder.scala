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

/**
  * HttpRequest -> Route finder
  */
trait RouteFinder {
  def findRoute[Req](request: HttpRequest[Req]): Option[Route]
}

object RouteFinder {

  def defaultRouteFinder[Req](request: HttpRequest[Req], routes: Seq[Route]) = {
    routes
      .find { r =>
        r.method == request.method &&
        checkPath(request.pathComponents, r.pathComponents)
      }
  }

  private[http] def checkPath(requestPathComponents: Seq[String], routePathComponents: Seq[String]): Boolean = {
    if (requestPathComponents.length == routePathComponents.length) {
      requestPathComponents.zip(routePathComponents).forall {
        case (requestPathComponent, routePathComponent) =>
          routePathComponent.startsWith(":") || routePathComponent == requestPathComponent
      }
    } else {
      false
    }
  }

  def build(routes: Seq[Route]): RouteFinder = {
    new RouteFinderGroups(routes)
  }

  /**
    * RouteFinders grouped by HTTP method types
    */
  class RouteFinderGroups(routes: Seq[Route]) extends RouteFinder {
    private val routesByMethod: Map[HttpMethod, RouteFinder] = {
      for ((method, lst) <- routes.groupBy(_.method)) yield {
        method -> new FastRouteFinder(lst)
      }
    }

    def findRoute[Req](request: HttpRequest[Req]): Option[Route] = {
      routesByMethod.get(request.method).flatMap { nextRouter =>
        nextRouter.findRoute(request)
      }
    }
  }

  class FastRouteFinder(routes: Seq[Route]) extends RouteFinder {

    process(routes, 0)

    def findRoute[Req](request: HttpRequest[Req]): Option[Route] = {
      RouteFinder.defaultRouteFinder(request, routes)
    }
  }

  private def process(lst: Seq[Route], pathIndex: Int): Unit = {
    lst.map { r =>
      toPathMapping(r, 0)
    }
  }

  class NFA {
    private var currentStates: Set[Int] = Set(0)

    def transit(x: String): Unit = {}
  }

  sealed trait PathMapping
  case class VariableMapping(route: Route, varName: String)     extends PathMapping
  case class PathSequenceMapping(route: Route, varName: String) extends PathMapping
  case class ConstantPathMapping(route: Route, name: String)    extends PathMapping

  private def toPathMapping(r: Route, pathIndex: Int): List[PathMapping] = {
    if (r.pathComponents.length >= pathIndex) {
      Nil
    } else {
      r.pathComponents(pathIndex) match {
        case x if x.startsWith(":") =>
          VariableMapping(r, x.substring(1)) :: toPathMapping(r, pathIndex + 1)
        case x if x.startsWith("*") =>
          if (r.pathComponents.length != pathIndex - 1) {
            throw new IllegalArgumentException(s"${r.path} cannot have '*' in the middle of the path")
          }
          PathSequenceMapping(r, x.substring(1)) :: toPathMapping(r, pathIndex + 1)
        case x =>
          ConstantPathMapping(r, x) :: toPathMapping(r, pathIndex + 1)
      }
    }
  }

}
