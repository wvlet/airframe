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
import com.sun.org.apache.xpath.internal.axes.PathComponent
import wvlet.airframe.http.Automaton.Graph
import wvlet.log.LogSupport

import scala.collection.mutable

/**
  * HttpRequest -> Route finder
  */
trait RouteFinder {
  def findRoute[Req](request: HttpRequest[Req]): Option[RouteMatch]
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

    def findRoute[Req](request: HttpRequest[Req]): Option[RouteMatch] = {
      routesByMethod.get(request.method).flatMap { nextRouter =>
        nextRouter.findRoute(request)
      }
    }
  }

  class FastRouteFinder(routes: Seq[Route]) extends RouteFinder with LogSupport {

    private val dfa = {
      val g = buildNFA(routes)
      trace(g)
      g.toDFA
    }
    debug(dfa)

    def findRoute[Req](request: HttpRequest[Req]): Option[RouteMatch] = {
      var currentState = 0
      var pathIndex    = 0
      val pc           = request.pathComponents

      var foundRoute: Option[Route] = None
      var toContinue                = true

      var params = Map.empty[String, String]

      // Traverse the path components and transit the DFA state
      while ((toContinue || foundRoute.isEmpty) && pathIndex < pc.length) {
        val token = pc(pathIndex)
        pathIndex += 1
        dfa.nextActions(currentState, token) match {
          case Some((actions, nextStateId)) =>
            trace(s"${currentState} -> ${token} -> ${nextStateId}")
            currentState = nextStateId
            actions.foreach { action =>
              params = action.updateMatch(params, token)
            }
            if (actions.size == 1 && actions.head.isTerminal) {
              foundRoute = actions.head.route
              // Continue matching for PathSequenceMapping
              toContinue = actions.head.isRepeat
            }
          case None =>
            toContinue = false
        }
      }
      debug(params.mkString(", "))
      foundRoute.map(r => RouteMatch(r, params.toMap))
    }
  }

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

  type State = Set[PathMapping]

  // DFA
  class PathGraphDFA(stateTable: Map[State, Int],
                     tokenTable: Map[String, Int],
                     transitions: Seq[(State, String, State)]) {

    // (currentStateId, tokenId) -> (nextState, nextStateId)
    // TODO: Use Array for lookup
    private val transitionTable: Map[(Int, Int), (State, Int)] = {
      transitions.map { x =>
        val stateId     = stateTable(x._1)
        val tokenId     = tokenTable(x._2)
        val nextStateId = stateTable(x._3)
        (stateId, tokenId) -> (x._3, nextStateId)
      }.toMap
    }

    override def toString: String = {
      transitionTable.mkString("\n")
    }

    // Return (next state, next state id)
    def nextActions(current: Int, token: String): Option[(State, Int)] = {
      val tokenId = tokenTable.getOrElse(token, 0)
      transitionTable.get((current, tokenId))
    }
  }

  // NFA of state transition
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

    // Convert NFA to DFA
    def toDFA: PathGraphDFA = {
      val initState: State = Set(Init)
      // knownStates, knownTokens will be used for assigning integer IDs.
      var knownStates: List[State]  = initState :: Nil
      var knownTokens: List[String] = anyToken :: Nil
      val stateTransitionTable      = mutable.Map.empty[State, Map[String, State]]

      // NFA: (PathMapping, token) -> Seq[PathMapping]
      // DFA: (State, token) -> State where State is Set[PathMapping]

      // This code is following a standard procedure for converting NFA into DFA.
      // Starting from an initial state {s0}, then traverse all possible next states in NFA {s_a, s_b, ...},
      // then make this set a new state of DFA.
      //
      // initial state {s0} -> s_i: {all possible next states in NFA}
      // s_i -> s_{i+1} {s_x, s_y, ....}
      // ...
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
      val transitions = (for ((state, edges) <- stateTransitionTable) yield {
        {
          for ((token, nextState) <- edges) yield {
            val nextStateId = stateTable(nextState)
            val tokenId     = tokenTable(token)
            (state, token, nextState)
          }
        }.toSeq
      }).flatten.toSeq

      stateTable.foreach {
        case (states, stateId) =>
          if (states.size > 1 && states.forall(_.isTerminal)) {
            throw new IllegalStateException(
              s"Ambiguous HTTP routes are found:\n${states.flatMap(_.route).map(x => s"- ${x.path}").mkString("\n")}")
          }
      }

      new PathGraphDFA(stateTable, tokenTable, transitions)
    }
  }

  // Build a graph for (state: PathMapping, token:String) -> nextStates:Seq[PathMapping]
  private[http] class PathGraphBuilder {
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

  private def buildPathGraph(routes: Seq[Route]): Graph[Set[PathMapping], String] = {
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

    // Build NFA of path patterns
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
    g.toNFA(Init).toGraph
  }

}
