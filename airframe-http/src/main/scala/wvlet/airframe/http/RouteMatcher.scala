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
import wvlet.airframe.http.Automaton.{DFA, NextNode}
import wvlet.log.LogSupport

case class RouteMatch(route: Route, params: Map[String, String]) {
  def call[Req: HttpRequestAdapter](controllerProvider: ControllerProvider, request: Req): Option[Any] = {
    route.call(controllerProvider, request, params)
  }
}

/**
  * Find a matching route (RouteMatch) from a given HttpRequest
  */
trait RouteMatcher {
  def findRoute[Req: HttpRequestAdapter](request: Req): Option[RouteMatch]
}

object RouteMatcher extends LogSupport {

  def build(routes: Seq[Route]): RouteMatcher = {
    new RouteMatcherByHttpMethodTypes(routes)
  }

  /**
    * A set of RouteMatchers for different HTTP method types
    */
  class RouteMatcherByHttpMethodTypes(routes: Seq[Route]) extends RouteMatcher {
    private val routesByMethod: Map[HttpMethod, RouteMatcher] = {
      for ((method, lst) <- routes.groupBy(_.method)) yield {
        method -> new FastRouteMatcher(method, lst)
      }
    }

    def findRoute[Req](request: Req)(implicit tp: HttpRequestAdapter[Req]): Option[RouteMatch] = {
      routesByMethod.get(tp.methodOf(request)).flatMap { nextRouter =>
        nextRouter.findRoute(request)(tp)
      }
    }
  }

  /**
    * DFA-based RouterMatcher
    */
  class FastRouteMatcher(targetMethod: HttpMethod, routes: Seq[Route]) extends RouteMatcher with LogSupport {
    private val dfa = buildPathDFA(routes)
    trace(s"DFA for ${routes.size} ${targetMethod} requests:\n${dfa}")

    dfa.nodeTable
      .map(_._1).foreach(state =>
        if (state.size > 1 && state.forall(_.isTerminal)) {
          throw new IllegalArgumentException(
            s"Found multiple matching routes: ${state.map(_.route).flatten.map(p => s"${p.path}").mkString(", ")} ")
      })

    def findRoute[Req](request: Req)(implicit tp: HttpRequestAdapter[Req]): Option[RouteMatch] = {
      var currentState = dfa.initStateId
      var pathIndex    = 0
      val pc           = tp.pathComponentsOf(request)

      var foundRoute: Option[Route] = None
      var toContinue                = true

      var params = Map.empty[String, String]

      // Traverse the path components and transit the DFA state
      while (toContinue && pathIndex < pc.length) {
        val token = pc(pathIndex)
        pathIndex += 1
        dfa.nextNode(currentState, token) match {
          case Some(NextNode(actions, nextStateId)) =>
            trace(s"path index:${pathIndex}/${pc.length}, transition: ${currentState} -> ${token} -> ${nextStateId}")
            currentState = nextStateId
            // Update variable bindings here
            actions.foreach { action =>
              params = action.updateMatch(params, token)
            }

            // Try to find a match at the last path component
            if (pathIndex == pc.length) {
              actions.find(_.isTerminal).map { matchedAction =>
                foundRoute = matchedAction.route
                // Continue the matching for PathSequenceMapping
                toContinue = false
              }
            }
          case None =>
            // Dead-end in the DFA
            toContinue = false
        }
      }

      foundRoute.map { r =>
        trace(s"Found a matching route: ${r.path} <= {${params.mkString(", ")}}")
        RouteMatch(r, params.toMap)
      }
    }
  }

  /**
    * Define an operation when matching path component is found (e.g., binding path components to matching path variables)
    */
  sealed trait PathMapping {
    // Matched route
    def route: Option[Route]
    def isTerminal: Boolean                                                             = route.isDefined
    def isRepeat: Boolean                                                               = false
    def updateMatch(m: Map[String, String], pathComponent: String): Map[String, String] = m
  }
  case object Init extends PathMapping {
    override def route: Option[Route] = None
  }
  case class VariableMapping(index: Int, varName: String, route: Option[Route]) extends PathMapping {
    override def toString: String = {
      val t = s"[${index}]/$$${varName}"
      if (isTerminal) s"!${t}" else t
    }
    override def updateMatch(m: Map[String, String], pathComponent: String): Map[String, String] = {
      m + (varName -> pathComponent)
    }
  }
  case class ConstantPathMapping(index: Int, name: String, route: Option[Route]) extends PathMapping {
    override def toString: String = {
      val t = s"[${index}]/${name}"
      if (isTerminal) s"!${t}" else t
    }
  }

  /**
    * Matching the tail of path components to a single variable
    */
  case class PathSequenceMapping(index: Int, varName: String, route: Option[Route]) extends PathMapping {
    override def toString: String    = s"![${index}]/*${varName}"
    override def isTerminal: Boolean = true
    override def isRepeat: Boolean   = true
    override def updateMatch(m: Map[String, String], pathComponent: String): Map[String, String] = {
      val paramValue = m.get(varName) match {
        case Some(x) => s"${x}/${pathComponent}"
        case None    => pathComponent
      }
      m + (varName -> paramValue)
    }
  }

  private val anyToken: String = "<*>"

  private[http] def buildPathDFA(routes: Seq[Route]): DFA[Set[PathMapping], String] = {
    // Convert http path pattens (Route) to mapping operations (List[PathMapping])
    def toPathMapping(r: Route, pathIndex: Int): List[PathMapping] = {
      if (pathIndex >= r.pathComponents.length) {
        Nil
      } else {
        val isTerminal = pathIndex == r.pathComponents.length - 1
        r.pathComponents(pathIndex) match {
          case x if x.startsWith(":") =>
            VariableMapping(pathIndex, x.substring(1), if (isTerminal) Some(r) else None) :: toPathMapping(
              r,
              pathIndex + 1)
          case x if x.startsWith("*") =>
            if (!isTerminal) {
              throw new IllegalArgumentException(s"${r.path} cannot have '*' in the middle of the path")
            }
            PathSequenceMapping(pathIndex, x.substring(1), Some(r)) :: toPathMapping(r, pathIndex + 1)
          case x =>
            ConstantPathMapping(pathIndex, x, if (isTerminal) Some(r) else None) :: toPathMapping(r, pathIndex + 1)
        }
      }
    }

    // Build an NFA of path patterns
    var g = Automaton.empty[PathMapping, String]
    for (r <- routes) {
      val pathMappings = Init :: toPathMapping(r, 0)
      for (it <- pathMappings.sliding(2)) {
        val pair   = it.toIndexedSeq
        val (a, b) = (pair(0), pair(1))
        b match {
          case ConstantPathMapping(_, token, _) =>
            g = g.addEdge(a, token, b)
          case PathSequenceMapping(_, _, _) =>
            g = g.addEdge(a, anyToken, b)
            // Add self-cycle edge for keep reading as sequence of paths
            g = g.addEdge(b, anyToken, b)
          case _ =>
            g = g.addEdge(a, anyToken, b)
        }
      }
    }
    // Convert the NFA into DFA to uniquely determine the next state in the automation.
    g.toDFA(Init, defaultToken = anyToken)
  }

}
