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
package wvlet.airframe.http.router

/**
  *
  */
object Automaton {
  def empty[Node, Token]: Automaton[Node, Token] = new Automaton(Set.empty, Set.empty)

  case class Edge[Node, Token](src: Node, token: Token, dest: Node)

  /**
    * Immutable Automaton implementation. Adding nodes or edges will create a new Automaton instance
    */
  class Automaton[Node, Token](val nodes: Set[Node], val edges: Set[Edge[Node, Token]]) {
    type NodeSet = Set[Node]

    def addNode(n: Node): Automaton[Node, Token] = {
      new Automaton(nodes + n, edges)
    }

    def addEdge(src: Node, token: Token, dest: Node): Automaton[Node, Token] = {
      new Automaton(nodes ++ Seq(src, dest), edges + (Edge(src, token, dest)))
    }

    def outEdgesFrom(n: Node): Set[Edge[Node, Token]] = {
      edges.filter(x => x.src == n)
    }

    def nextNodes(current: Node, token: Token): Set[Node] = {
      edges
        .filter(x => x.src == current && x.token == token)
        .map(e => e.dest)
    }

    /**
      *  Converting NFA to DFA
      *
      *   NFA: (Node, Token) -> Seq[Node]  (multiple next nodes can be found for a given token)
      *   DFA: State = Seq[Node]
      *     (State, Token) -> State        (only a single state can be found for a given token)
      *
      * @param init initial node to start
      */
    def toDFA(init: Node, defaultToken: Token): DFA[NodeSet, Token] = {
      // This code is following a standard procedure for converting NFA into DFA:
      //  1. Add the initial node set {s} (= current node set) to the queue
      //  2. Pick a node set from the queue.
      //  3. Traverse all possible next states (= the next node set) from the current node set.
      //  4. If the next node set is appeared for the first time, add this to the queue.
      //  5. Repeat 2. until the queue becomes empty.

      val initState: Set[Node] = Set(init)

      var knownNodeSets: List[NodeSet] = initState :: Nil
      var dfa                          = Automaton.empty[NodeSet, Token]

      // Register the initState to the DFA
      dfa = dfa.addNode(initState)

      // Traverse all possible states from the initState
      var remaining: List[NodeSet] = initState :: Nil
      while (remaining.nonEmpty) {
        val currentNodeSet = remaining.head
        remaining = remaining.tail
        val tokenToNextNodes = for (node <- currentNodeSet; edge <- outEdgesFrom(node)) yield {
          // token -> { next possible states, ... }
          edge.token -> nextNodes(node, edge.token)
        }

        // Grouping next nodes by label
        for ((token, nextNodeSets) <- tokenToNextNodes.groupBy(_._1)) {
          val nextNodeSet = nextNodeSets.map(_._2).flatten
          if (!knownNodeSets.contains(nextNodeSet)) {
            remaining = nextNodeSet :: remaining
            knownNodeSets = nextNodeSet :: knownNodeSets
          }
          // Add: {current node sets} -> token -> {next possible node sets}
          dfa = dfa.addEdge(currentNodeSet, token, nextNodeSet)
        }
      }

      // Build DFA
      val nodeTable  = dfa.nodes.zipWithIndex.toMap
      val tokenTable = (dfa.edges.map(_.token) + defaultToken).zipWithIndex.toMap
      new DFA(nodeTable, tokenTable, dfa.edges, initState, defaultToken)
    }
  }

  case class NextNode[Node](node: Node, nodeId: Int)

  case class DFA[Node, Token](
      nodeTable: Map[Node, Int],
      tokenTable: Map[Token, Int],
      edges: Set[Edge[Node, Token]],
      init: Node,
      defaultToken: Token
  ) {
    val initStateId = nodeTable(init)

    // (currentStateId, tokenId) -> (nextState, nextStateId)
    // TODO: Use array-lookup for faster lookup
    private val transitionTable: Map[(Int, Int), NextNode[Node]] = {
      edges.map { x => (nodeTable(x.src), tokenTable(x.token)) -> NextNode[Node](x.dest, nodeTable(x.dest)) }.toMap
    }
    private val tokenIdTable = tokenTable.map(x => x._2 -> x._1).toMap

    override def toString: String = {
      val s = Seq.newBuilder[String]
      s += "[nodes]"
      s += nodeTable.map(x => s"${x._2}: ${x._1}").mkString("\n")
      s += "\n[tokens]"
      s += tokenTable.map(x => s"${x._2}: ${x._1}").mkString("\n")
      s += "\n[edges]"
      s += transitionTable
        .map { case ((state, token), next) => s"${state}: ${tokenIdTable(token)} -> ${next.nodeId}: ${next.node}" }.mkString(
          "\n"
        )
      s.result().mkString("\n")
    }

    def nextNode(currentState: Int, token: Token): Option[NextNode[Node]] = {
      tokenTable
        .get(token).flatMap { tokenId =>
          // Check the exact match token
          transitionTable.get((currentState, tokenId))
        }.orElse {
          // Check the wildcard token
          transitionTable.get((currentState, tokenTable(defaultToken)))
        }
    }
  }
}
