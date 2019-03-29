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

object Automaton {

  def emptyNFA[Node, Token]: Automaton[Node, Token] = new Automaton(Set.empty, Set.empty)

  case class Edge[Node, Token](src: Node, token: Token, dest: Node)

  class Automaton[Node, Token](nodes: Set[Node], edges: Set[Edge[Node, Token]]) {
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

    def toNFA(init: Node): Automaton[NodeSet, Token] = {
      val initState: Set[Node] = Set(init)

      var knownNodeSets: List[NodeSet] = initState :: Nil
      var nfa                          = Automaton.emptyNFA[NodeSet, Token]

      var remaining: List[NodeSet] = initState :: Nil
      while (remaining.nonEmpty) {
        val currentNodeSet = remaining.head
        remaining = remaining.tail
        val tokenToNextNodes = for (node <- currentNodeSet; edge <- outEdgesFrom(node)) yield {
          // token -> {s_2, s_5, ...}
          edge.token -> nextNodes(node, edge.token)
        }

        // Grouping next nodes by label
        for ((token, nextNodeSets) <- tokenToNextNodes.groupBy(_._1)) {
          val nextNodeSet = nextNodeSets.map(_._2).flatten
          if (!knownNodeSets.contains(nextNodeSet)) {
            remaining = nextNodeSet :: remaining
            knownNodeSets = nextNodeSet :: knownNodeSets
          }
          nfa = nfa.addEdge(currentNodeSet, token, nextNodeSet)
        }
      }

      nfa
    }
  }

}
