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
import wvlet.log.LogSupport

import scala.collection.mutable

/**
  * HttpRequest -> Route finder
  */
trait RouteFinder {
  def findRoute[Req](request: HttpRequest[Req]): Option[Route]
}

object RouteFinder extends LogSupport {

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

  class FastRouteFinder(routes: Seq[Route]) extends RouteFinder with LogSupport {

    val g = buildNFA(routes)
    info(g)
    val dfa = g.toDFA
    info(dfa)

    def findRoute[Req](request: HttpRequest[Req]): Option[Route] = {
      RouteFinder.defaultRouteFinder(request, routes)
    }
  }

  sealed trait PathMapping {
    def isTerminal: Boolean = false
  }
  case object Init extends PathMapping
  case class VariableMapping(route: Route, varName: String, override val isTerminal: Boolean) extends PathMapping {
    override def toString: String = {
      val t = s"/$$${varName}"
      if (isTerminal) s"[[${t}]]" else t
    }
  }
  case class ConstantPathMapping(route: Route, name: String, override val isTerminal: Boolean) extends PathMapping {
    override def toString: String = {
      val t = s"/${name}"
      if (isTerminal) s"[[${t}]]" else t
    }
  }
  case class PathSequenceMapping(route: Route, varName: String) extends PathMapping {
    override def toString: String    = s"[[*${varName}]]"
    override def isTerminal: Boolean = true
  }

  private val anyToken: String = "<*>"

  class PathGraphDFA() {}

  type State = Set[PathMapping]

  class PathGraph(edgeTable: Map[PathMapping, Map[String, Seq[PathMapping]]]) {
    override def toString(): String = {
      val s = Seq.newBuilder[String]
      for (src <- edgeTable.keys) {
        for ((token, dest) <- edgeTable(src)) {
          s += s"[${src}]: ${token} -> ${dest.mkString(", ")}"
        }
      }
      s.result().mkString("\n")
    }

    def possibleTokensAt(state: PathMapping): Seq[String] = {
      edgeTable.getOrElse(state, Map.empty).keys.toSeq
    }

    def nextStates(current: PathMapping, token: String): Seq[PathMapping] = {
      edgeTable.get(current).flatMap(_.get(token)).getOrElse(Seq.empty)
    }

    def toDFA: PathGraphDFA = {
      // Convert NFA to DFA
      val initState: State          = Set(Init)
      var knownStates: List[State]  = initState :: Nil
      var knownTokens: List[String] = anyToken :: Nil
      val stateTransitionTable      = mutable.Map.empty[State, Map[String, State]]

      var remaining: List[State] = initState :: Nil
      while (remaining.nonEmpty) {
        val current = remaining.head
        remaining = remaining.tail
        val tokenToNextState = for (state <- current; token <- possibleTokensAt(state)) yield {
          if (!knownTokens.contains(token)) {
            knownTokens = token :: knownTokens
          }
          token -> nextStates(state, token)
        }
        for ((token, nextStateList) <- tokenToNextState.groupBy(_._1)) {
          val m         = stateTransitionTable.getOrElse(current, Map.empty)
          val nextState = nextStateList.map(_._2).flatten.toSet
          if (!knownStates.contains(nextState)) {
            remaining = nextState :: remaining
            knownStates = nextState :: knownStates
          }
          stateTransitionTable.put(current, m + (token -> nextState))
        }
      }

      // Build a state table. Reversing the list here to make Set(Init) to 0th state
      val stateTable = knownStates.reverse.zipWithIndex.toMap
      val tokenTable = knownTokens.reverse.zipWithIndex.toMap
      logger.info(tokenTable.mkString(", "))
      logger.info(stateTable.mkString("\n"))
      val transitions = for ((state, edges) <- stateTransitionTable) yield {
        val stateId = stateTable(state)
        for ((token, nextState) <- edges) yield {
          val nextStateId = stateTable(nextState)
          val tokenId     = tokenTable(token)
          logger.info(s"${stateId} - ${token}(${tokenId}) -> ${nextStateId}")
          (stateId, tokenId, nextStateId)
        }
      }

      new PathGraphDFA()
    }
  }

  class PathGraphBuilder {
    private val edgeTable: mutable.Map[PathMapping, Map[String, Seq[PathMapping]]] = mutable.Map.empty

    def addEdge(current: PathMapping, token: String, next: PathMapping): Unit = {
      val transitionTable: Map[String, Seq[PathMapping]] = edgeTable.getOrElse(current, Map.empty)
      val nextStates                                     = transitionTable.getOrElse(token, Seq.empty) :+ next
      edgeTable.put(current, transitionTable + (token -> nextStates))
    }

    def addDefaultEdge(current: PathMapping, next: PathMapping): Unit = {
      addEdge(current, anyToken, next)
    }

    def build: PathGraph = {
      new PathGraph(edgeTable.toMap)
    }
  }

  private def buildNFA(routes: Seq[Route]): PathGraph = {
    // Convert http path pattens (Route) to mapping operations (List[PathMapping])
    def toPathMapping(r: Route, pathIndex: Int): List[PathMapping] = {
      if (pathIndex >= r.pathComponents.length) {
        Nil
      } else {
        val isTerminal = pathIndex == r.pathComponents.length - 1
        r.pathComponents(pathIndex) match {
          case x if x.startsWith(":") =>
            VariableMapping(r, x.substring(1), isTerminal) :: toPathMapping(r, pathIndex + 1)
          case x if x.startsWith("*") =>
            if (!isTerminal) {
              throw new IllegalArgumentException(s"${r.path} cannot have '*' in the middle of the path")
            }
            PathSequenceMapping(r, x.substring(1)) :: toPathMapping(r, pathIndex + 1)
          case x =>
            ConstantPathMapping(r, x, isTerminal) :: toPathMapping(r, pathIndex + 1)
        }
      }
    }

    // Build NFA of path patterns
    val g = new PathGraphBuilder
    for (r <- routes) {
      val pathMappings = Init :: toPathMapping(r, 0)
      for (it <- pathMappings.sliding(2)) {
        val pair   = it.toIndexedSeq
        val (a, b) = (pair(0), pair(1))
        b match {
          case ConstantPathMapping(_, token, _) =>
            g.addEdge(a, token, b)
          case PathSequenceMapping(_, _) =>
            g.addDefaultEdge(a, b)
            // Add self-cycle edge for keep reading as sequence of paths
            g.addDefaultEdge(b, b)
          case _ =>
            g.addDefaultEdge(a, b)
        }
      }
    }
    g.build
  }

}
