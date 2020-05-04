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
package wvlet.airframe.surface.reflect

import wvlet.log.LogSupport

/**
  *
  */
object Path extends LogSupport {
  def root: Path    = Root
  def current: Path = Current

  def apply(s: String): Path = {
    if (s.startsWith("""/""")) {
      val c = s.substring(1).split("""\/""")
      c.foldLeft[Path](Root) { (parent, component) => parent / component }
    } else {
      val c = s.split("""\/""")
      c.length match {
        case 0 => Current
        case _ =>
          c.foldLeft[Path](Current) { (parent, component) => parent / component }
      }
    }
  }

  private class AbsolutePath(val parent: Option[Path], val name: String) extends Path {
    def fullPath = "/" + this.mkString("/")

    def /(child: String) = new AbsolutePath(Some(this), child)

    def isRelative = false
  }

  private class RelativePath(val parent: Option[Path], val name: String) extends Path {
    def fullPath         = this.mkString("/")
    def /(child: String) = new RelativePath(Some(this), child)
    def isRelative       = true
    def getParent        = parent
  }

  private case object Root extends AbsolutePath(None, "") {
    override def iterator         = Iterator.empty
    override def /(child: String) = new AbsolutePath(None, child)
  }
  private[Path] case object Current extends RelativePath(None, "") {
    override def iterator         = Iterator.empty
    override def /(child: String) = new RelativePath(None, child)
  }
}

/**
  * Representing paths separated by slashes
  *
  * @author leo
  */
trait Path extends Iterable[String] {
  override def toString = fullPath

  /**
    * leaf name
    */
  def name: String
  def fullPath: String
  def /(child: String): Path
  def isRelative: Boolean
  def isAbsolute: Boolean = !isRelative
  def parent: Option[Path]
  def isLeaf = size == 1
  def tailPath: Path =
    if (isEmpty) {
      Path.Current
    } else {
      drop(1).foldLeft[Path](Path.Current) { (p, c) => p / c }
    }

  def iterator: Iterator[String] =
    parent match {
      case Some(p) => p.iterator ++ Iterator.single(name)
      case None    => Iterator.single(name)
    }

  override def hashCode = fullPath.hashCode
  override def equals(other: Any) = {
    val o = other.asInstanceOf[AnyRef]
    if (this eq o) {
      true
    } else if (classOf[Path].isAssignableFrom(o.getClass)) {
      this.fullPath == other.asInstanceOf[Path].fullPath
    } else {
      false
    }
  }
}
