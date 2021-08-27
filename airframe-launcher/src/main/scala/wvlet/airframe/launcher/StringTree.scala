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
package wvlet.airframe.launcher

import wvlet.airframe.launcher.StringTree.Leaf
import wvlet.log.LogSupport
import wvlet.airframe.surface.reflect.Path

/**
  * Holder of structured data consisting of named values. ValueHolder is immutable, so the set operations in this class
  * return another ValueHolder and never modify the original ValueHolder.
  *
  * <pre> A(a, B(b, c))
  *
  * { a: apple, B:{b:book, c:car} }
  *
  * val n1 = EmptyNode.set("a", apple) => Node(a -> Leaf(apple)) val n2 = n1.set("B.b", "book")
  * => Node(a -> Leaf(apple), B -> EmptyNode.set("b", "book"))
  * => Node(a -> apple, B->Node(b -> Leaf(book))) val n3 = n2.set("B.c", "car") => Node(a ->apple, B->Node(b ->
  * Leaf(book), c->Leaf(car)))
  *
  * </pre>
  */
sealed trait StringTree {
  def +(e: (Path, StringTree)): StringTree = setNode(e._1, e._2)
  def ++(it: Iterable[(Path, StringTree)]): StringTree =
    it.foldLeft[StringTree](this) { (h, e) =>
      h.setNode(e._1, e._2)
    }

  /**
    * Set a value at the specified path
    *
    * @param path
    *   string representation of Path
    * @param value
    * @return
    *   updated value holder
    */
  def set(path: String, value: String): StringTree = setNode(Path(path), Leaf(value))

  def set(path: Path, value: String): StringTree = setNode(path, Leaf(value))

  /**
    * Set a value at the specified path
    *
    * @param path
    *   path
    * @param value
    *   String value to set
    * @return
    *   updated value holder
    */
  def setNode(path: Path, node: StringTree): StringTree

  /**
    * Extract a part of the value holder under the path
    *
    * @param path
    * @return
    *   value holder under the path
    */
  def get(path: String): StringTree = get(Path(path))

  /**
    * Extract a part of the value holder under the path
    *
    * @param path
    * @return
    *   value holder under the path
    */
  def get(path: Path): StringTree

  /**
    * Depth first search iterator
    *
    * @return
    */
  def dfs: Iterator[(Path, String)] = dfs(Path.current)

  /**
    * Depth first search iterator under the specified path
    *
    * @param path
    * @return
    */
  def dfs(path: Path): Iterator[(Path, String)]

  /**
    * Depth first search iterator under the specified path
    *
    * @param path
    * @return
    */
  def dfs(path: String): Iterator[(Path, String)] = dfs(Path(path))

  def isEmpty = false

  def toMsgPack: Array[Byte] = {
    StringTreeCodec.toMsgPack(this)
  }
}

object StringTree extends LogSupport {
  import collection.immutable.{Map => IMap}

  def apply(elems: Iterable[(Path, StringTree)]): StringTree = EmptyNode.++(elems)

  val empty: StringTree = EmptyNode

  private[launcher] case object EmptyNode extends StringTree {
    def setNode(path: Path, value: StringTree) = {
      if (path.isEmpty) {
        value
      } else {
        Node(IMap.empty[String, StringTree]).setNode(path, value)
      }
    }
    def get(path: Path)     = EmptyNode
    def extract(path: Path) = EmptyNode

    def dfs(path: Path)  = Iterator.empty
    override def isEmpty = true
  }

  private[launcher] case class Node(child: IMap[String, StringTree]) extends StringTree {
    override def toString: String =
      s"{${child.map(e => s"${e._1}:${e._2}").mkString(", ")}}"

    def setNode(path: Path, value: StringTree): StringTree = {
      if (path.isEmpty) {
        throw new IllegalStateException("path cannot be empty")
      } else {
        val p = child.getOrElse(path.head, EmptyNode).setNode(path.tailPath, value)
        Node(child + (path.head -> p))
      }
    }

    def get(path: Path) = {
      if (path.isEmpty) {
        this
      } else {
        child.get(path.head) map { _.get(path.tailPath) } getOrElse EmptyNode
      }
    }

    def dfs(path: Path) = (for ((name, h) <- child) yield h.dfs(path / name)).reduce(_ ++ _)
  }

  private[launcher] case class Leaf(value: String) extends StringTree {
    override def toString = value.toString
    def setNode(path: Path, value: StringTree) = {
      SeqLeaf(Seq(this, EmptyNode.setNode(path, value)))
    }

    def get(path: Path) = {
      if (path.isEmpty) {
        this
      } else {
        EmptyNode
      }
    }

    def dfs(path: Path) = Iterator.single(path -> value)
  }

  /**
    * Node containing multiple elements
    * @param elems
    */
  private[launcher] case class SeqLeaf(elems: Seq[StringTree]) extends StringTree {
    override def toString = s"[${elems.mkString(", ")}]"

    def setNode(path: Path, value: StringTree) = {
      SeqLeaf(elems :+ EmptyNode.setNode(path, value))
    }

    def get(path: Path) =
      if (path.isEmpty) {
        this
      } else {
        EmptyNode
      }

    def dfs(path: Path) = elems.map(e => e.dfs(path)).reduce(_ ++ _)
  }
}
