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
