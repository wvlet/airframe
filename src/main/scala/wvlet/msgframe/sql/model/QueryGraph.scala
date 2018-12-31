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

package wvlet.msgframe.sql.model

/**
  * Graph for representing table input and output dependencies
  */
object QueryGraph {

  trait Node {
    def name: String
  }

  case object Terminal extends Node {
    override def toString = name
    def name              = "#"
  }
  case class Alias(name: String) extends Node {
    override def toString = s"&${name}"
  }
  case class SourceTable(name: String) extends Node {
    override def toString = name
  }
  case class TargetTable(name: String) extends Node {
    override def toString = s"!${name}"
  }

  case class Edge(src: Node, dest: Node) {
    override def toString = s"${src}->${dest}"
  }

  case class Graph(nodes: Set[Node], edges: Set[Edge]) {
    def +(n: Node): Graph = Graph(nodes + n, edges)
    def +(e: Edge): Graph = {
      val s = findNode(e.src)
      val d = findNode(e.dest)
      Graph(nodes + s + d, edges + Edge(s, d))
    }

    def findNode(n: Node): Node = {
      nodes.find(_.name == n.name).getOrElse(n)
    }

    override def toString = {
      s"""nodes: ${nodes.mkString(", ")}
         |edges: ${edges.mkString(", ")}""".stripMargin
    }
  }

  case object EdgeOrdering extends Ordering[Edge] {
    override def compare(x: Edge, y: Edge): Int = {
      val diff = x.src.name.compareTo(y.src.name)
      if (diff != 0) {
        diff
      } else {
        x.dest.name.compareTo(y.dest.name)
      }
    }
  }

  val emptyGraph = Graph(Set.empty, Set.empty)
}
