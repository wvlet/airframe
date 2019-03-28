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

    def findRoute[Req](request: HttpRequest[Req]): Option[Route] = {
      RouteFinder.defaultRouteFinder(request, routes)
    }
  }

  private def process(lst: Seq[Route], pathIndex: Int): Seq[List[PathMapping]] = {
    lst.map { r =>
      toPathMapping(r, 0, 1)
    }

  }

  sealed trait PathMapping {
    def id: Int
  }
  case class VariableMapping(id: Int, route: Route, varName: String)     extends PathMapping
  case class PathSequenceMapping(id: Int, route: Route, varName: String) extends PathMapping
  case class ConstantPathMapping(id: Int, route: Route, name: String)    extends PathMapping
  case object Terminal extends PathMapping {
    def id = 0
  }

  private def toPathMapping(r: Route, pathIndex: Int, numStates: Int): List[PathMapping] = {
    if (r.pathComponents.length >= pathIndex) {
      Nil
    } else {
      r.pathComponents(pathIndex) match {
        case x if x.startsWith(":") =>
          VariableMapping(numStates, r, x.substring(1)) :: toPathMapping(r, pathIndex + 1, numStates + 1)
        case x if x.startsWith("*") =>
          if (r.pathComponents.length != pathIndex - 1) {
            throw new IllegalArgumentException(s"${r.path} cannot have '*' in the middle of the path")
          }
          PathSequenceMapping(numStates, r, x.substring(1)) :: toPathMapping(r, pathIndex + 1, numStates + 1)
        case x =>
          ConstantPathMapping(numStates, r, x) :: toPathMapping(r, pathIndex + 1, numStates + 1)
      }
    }
  }

  private def buildNFA(routes: Seq[Route]): NFA = {
    val pathMapping = process(routes, 0)
    val tokenTable: Map[String, Int] = pathMapping.flatten
      .collect {
        case ConstantPathMapping(_, _, name) => name
      }
      .zipWithIndex
      .map {
        case (s, i) => s -> (i + 1)
      }.toMap[String, Int]

    val states = pathMapping.flatten.map(_.id).max
    val numStates = pathMapping.
    val numTokens = tokenTable.size + 1


    for (t <- 0 until numTokens) yield {}

    val numStates = pathMapping.map(_.length).sum

    val matrix = Array.fill(tokenTable.size + 1, numStates)(0)
    new NFA(tokenTable, matrix)
  }

  /**
    * r1: /v1/config
    *
    * -1: match to r1
    * 0: N/A
    * 1: init
    *
    * token  | 0 | 1 | 2
    * -------------------
    *   *    | 0 | 0 | 0
    *   v1   | 0 | 2 | 0
    *  config| 0 | 0 |-1
    *
    */
  class NFA(tokenTable: Map[String, Int], transitionMatrix: Array[Array[Int]]) {
    def tokenId(s: String): Int = {
      tokenTable.getOrElse(s, 0)
    }

    def nextStates(currentState: Int, input: String): Seq[Int] = {}
  }

  class NFAState(nfa: NFA) {
    private var states = Set(0)

    def transit(s: String): Unit = {
      nfa.tokenId(s)
    }
  }

}
