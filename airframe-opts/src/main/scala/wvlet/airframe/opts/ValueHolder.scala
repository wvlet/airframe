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
package wvlet.airframe.opts

import wvlet.log.LogSupport
import wvlet.airframe.surface.reflect.Path

/**
  * Holder of structured data consisting of named values. ValueHolder is immutable, so
  * the set operations in this class return another ValueHolder and never modify the original ValueHolder.
  *
  * <pre>
  * A(a, B(b, c))
  *
  * { a: apple, B:{b:book, c:car} }
  *
  * val n1 = Empty.set("a", apple)  =>  Node(a -> Leaf(apple))
  * val n2 = n1.set("B.b", "book")
  * => Node(a -> Leaf(apple), B -> Empty.set("b", "book"))
  * => Node(a -> apple, B->Node(b -> Leaf(book)))
  * val n3 = n2.set("B.c", "car") => Node(a ->apple, B->Node(b -> Leaf(book), c->Leaf(car)))
  *
  * </pre>
  *
  */
sealed trait ValueHolder[+A] {

  def +[B >: A](e: (Path, B)): ValueHolder[B] = set(e._1, e._2)
  def ++[B >: A](it: Iterable[(Path, B)]): ValueHolder[B] = it.foldLeft[ValueHolder[B]](this) { (h, e) =>
    h.set(e._1, e._2)
  }

  /**
    * Set a value at the specified path
    *
    * @param path string representation of Path
    * @param value
    * @return updated value holder
    */
  def set[B >: A](path: String, value: B): ValueHolder[B] = set(Path(path), value)

  /**
    * Set a value at the specified path
    *
    * @param path  path
    * @param value String value to set
    * @return updated value holder
    */
  def set[B >: A](path: Path, value: B): ValueHolder[B]

  /**
    * Extract a part of the value holder under the path
    *
    * @param path
    * @return value holder under the path
    */
  def get(path: String): ValueHolder[A] = get(Path(path))

  /**
    * Extract a part of the value holder under the path
    *
    * @param path
    * @return value holder under the path
    */
  def get(path: Path): ValueHolder[A]

  /**
    * Depth first search iterator
    *
    * @return
    */
  def dfs: Iterator[(Path, A)] = dfs(Path.current)

  /**
    * Depth first search iterator under the specified path
    *
    * @param path
    * @return
    */
  def dfs(path: Path): Iterator[(Path, A)]

  /**
    * Depth first search iterator under the specified path
    *
    * @param path
    * @return
    */
  def dfs(path: String): Iterator[(Path, A)] = dfs(Path(path))

  def isEmpty = false

}

object ValueHolder extends LogSupport {

  import collection.immutable.{Map => IMap}

  def apply[A](elems: Iterable[(Path, A)]): ValueHolder[A] = Empty.++[A](elems)

  val empty: ValueHolder[Nothing] = Empty

  private[opts] case object Empty extends ValueHolder[Nothing] {
    def set[B >: Nothing](path: Path, value: B) = {
      if (path.isEmpty) {
        Leaf(value)
      } else {
        Node(IMap.empty[String, ValueHolder[B]]).set(path, value)
      }
    }
    def get(path: Path)     = Empty
    def extract(path: Path) = Empty

    def dfs(path: Path)  = Iterator.empty
    override def isEmpty = true
  }

  private[opts] case class Node[A](child: IMap[String, ValueHolder[A]]) extends ValueHolder[A] {
    override def toString: String =
      "{%s}".format(
        child
          .map { e =>
            "%s:%s".format(e._1, e._2)
          }
          .mkString(", "))

    def set[B >: A](path: Path, value: B): ValueHolder[B] = {
      if (path.isEmpty) {
        throw new IllegalStateException("path cannot be empty")
      } else {
        val p = child.getOrElse(path.head, Empty).set(path.tailPath, value)
        Node(child + (path.head -> p))
      }
    }

    def get(path: Path) = {
      if (path.isEmpty) {
        this
      } else {
        child.get(path.head) map { _.get(path.tailPath) } getOrElse Empty
      }
    }

    def dfs(path: Path) = (for ((name, h) <- child) yield h.dfs(path / name)).reduce(_ ++ _)
  }

  private[opts] case class Leaf[A](value: A) extends ValueHolder[A] {
    override def toString = value.toString
    def set[B >: A](path: Path, value: B) = {
      SeqLeaf(Seq(this, Empty.set[B](path, value)))
    }

    def get(path: Path) = {
      if (path.isEmpty) {
        this
      } else {
        Empty
      }
    }

    def dfs(path: Path) = Iterator.single(path -> value)
  }

  private[opts] case class SeqLeaf[A](elems: Seq[ValueHolder[A]]) extends ValueHolder[A] {
    override def toString = "[%s]".format(elems.mkString(", "))

    def set[B >: A](path: Path, value: B) = {
      SeqLeaf(elems :+ Empty.set(path, value))
    }

    def get(path: Path) =
      if (path.isEmpty) {
        this
      } else {
        Empty
      }

    def dfs(path: Path) = elems.map(e => e.dfs(path)).reduce(_ ++ _)

  }

}
