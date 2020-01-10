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
package scala.xml

import wvlet.airframe.rx.Rx
import wvlet.airframe.rx.widget.{RxElement, RxWidget}

import scala.collection.mutable
import scala.annotation.{implicitNotFound, tailrec}
import scala.language.higherKinds

// XML Nodes ------------------------------------------------------------------

// Nodes are now implemented as an idiomatic sealed hierarchy of case classes.
// `Atom` was kept as a class for source compatibility (even if it's not used
// directly in scalac paser). `NoteSeq` disappeared, `Node <:!< Seq[Node]`...

/** Trait representing XML tree. */
sealed trait Node {
  def scope: NamespaceBinding           = TopScope
  def prefix: String                    = null
  def namespace: String                 = getNamespace(this.prefix)
  def getNamespace(pre: String): String = if (scope eq null) null else scope.getURI(pre)
  def attributes: MetaData              = Null
}

/** A hack to group XML nodes in one node. */
final case class Group(nodes: Seq[Node]) extends Node

/** XML element. */
final case class Elem(
    override val prefix: String,
    label: String,
    attributes1: MetaData,
    override val scope: NamespaceBinding,
    minimizeEmpty: Boolean,
    child: Node*
) extends Node {

  override val attributes = MetaData.normalize(attributes1, scope)

}

/** XML leaf for comments. */
final case class Comment(commentText: String) extends Node

/**
  * `SpecialNode` is a special XML node which represents either text
  *  `(PCDATA)`, a comment, a `PI`, or an entity ref.
  *
  *  @author Burak Emir
  */
sealed abstract class SpecialNode extends Node {

  /** always empty */
  final override def attributes = Null

  /** always Node.EmptyNamespace */
  final override def namespace = null

  /** always empty */
  final def child = Nil

  /** Append string representation to the given string buffer argument. */
  def buildString(sb: StringBuilder): StringBuilder
}

/** XML leaf for entity references. */
final case class EntityRef(entityName: String) extends SpecialNode {
  def label = "#ENTITY"

  def text = entityName match {
    case "lt"   => "<"
    case "gt"   => ">"
    case "amp"  => "&"
    case "apos" => "'"
    case "quot" => "\""
    case _      => Utility.sbToString(buildString)
  }

  /**
    * Appends `"&amp; entityName;"` to this string buffer.
    *
    *  @param  sb the string buffer.
    *  @return the modified string buffer `sb`.
    */
  override def buildString(sb: StringBuilder) =
    sb.append("&").append(entityName).append(";")
}

object Utility {
  private[xml] def sbToString(f: (StringBuilder) => Unit): String = {
    val sb = new StringBuilder
    f(sb)
    sb.toString
  }

}

/** XML leaf for text. */
final case class Text(text: String) extends Atom[String](text)

/** XML leaf container for any data of type `A`. */
class Atom[+A](val data: A) extends Node

// Scopes ---------------------------------------------------------------------

sealed trait Scope {
  def namespaceURI(prefix: String): Option[String]
}

// Used by scalac for xmlns prefixed attributes
case class NamespaceBinding(prefix: String, uri: String, parent: NamespaceBinding) {
  def getURI(_prefix: String): String =
    if (prefix == _prefix) uri else parent getURI _prefix

  def getPrefix(_uri: String): String =
    if (_uri == uri) prefix else parent getPrefix _uri
}

object NamespaceBinding {
  def unapply(s: NamespaceBinding): NamespaceBinding = s
}

object TopScope extends NamespaceBinding(null, null, null) {
  override def getURI(prefix1: String): String =
    if (prefix1 == "xml") "http://www.w3.org/XML/1998/namespace" else null

  override def getPrefix(uri1: String): String =
    if (uri1 == "http://www.w3.org/XML/1998/namespace") "xml" else null
}

// XML Metadata ---------------------------------------------------------------

// `Attibutes`, the trait which used to be in between Metadata and {Prefixed,
// Unprefixed}Attribute was removed. Iterable[MetaData] <: Metadata still holds,
// but this should never be user facing.

/**
  * This class represents an attribute and at the same time a linked list of
  *  attributes. Every instance of this class is either
  *  - an instance of `UnprefixedAttribute key,value` or
  *  - an instance of `PrefixedAttribute namespace_prefix,key,value` or
  *  - `Null`, the empty attribute list.
  */
sealed trait MetaData {
  def hasNext = (Null != next)
  def key: String
  def value: Node
  def next: MetaData
  def map[T](f: MetaData => T): List[T] = {
    def map0(acc: List[T], m: MetaData): List[T] =
      m match {
        case Null => acc
        case any  => map0(f(any) :: acc, m.next)
      }
    map0(Nil, this)
  }

  /**
    * Updates this MetaData with the MetaData given as argument. All attributes that occur in updates
    *  are part of the resulting MetaData. If an attribute occurs in both this instance and
    *  updates, only the one in updates is part of the result (avoiding duplicates). For prefixed
    *  attributes, namespaces are resolved using the given scope, which defaults to TopScope.
    *
    *  @param updates MetaData with new and updated attributes
    *  @return a new MetaData instance that contains old, new and updated attributes
    */
  def append(updates: MetaData, scope: NamespaceBinding = TopScope): MetaData =
    MetaData.update(this, scope, updates)

  /**
    * returns a copy of this MetaData item with next field set to argument.
    */
  def newCopy(next: MetaData): MetaData
}

object MetaData {

  /**
    * appends all attributes from new_tail to attribs, without attempting to
    * detect or remove duplicates. The method guarantees that all attributes
    * from attribs come before the attributes in new_tail, but does not
    * guarantee to preserve the relative order of attribs.
    *
    * Duplicates can be removed with `normalize`.
    */
  @tailrec
  def concatenate(attribs: MetaData, new_tail: MetaData): MetaData =
    if (attribs eq Null) new_tail
    else concatenate(attribs.next, attribs newCopy new_tail)

  /**
    * returns normalized MetaData, with all duplicates removed and namespace prefixes resolved to
    *  namespace URIs via the given scope.
    */
  def normalize(attribs: MetaData, scope: NamespaceBinding): MetaData = {
    def iterate(md: MetaData, normalized_attribs: MetaData, set: Set[String]): MetaData = {
      lazy val key = getUniversalKey(md, scope)
      if (md eq Null) normalized_attribs
      else if ((md.value eq null) || set(key)) iterate(md.next, normalized_attribs, set)
      else md newCopy iterate(md.next, normalized_attribs, set + key)
    }
    iterate(attribs, Null, Set())
  }

  /**
    * returns key if md is unprefixed, pre+key is md is prefixed
    */
  def getUniversalKey(attrib: MetaData, scope: NamespaceBinding) = attrib match {
    case prefixed: PrefixedAttribute[_]     => scope.getURI(prefixed.pre) + prefixed.key
    case unprefixed: UnprefixedAttribute[_] => unprefixed.key
    case other                              => other.key
  }

  /**
    *  returns MetaData with attributes updated from given MetaData
    */
  def update(attribs: MetaData, scope: NamespaceBinding, updates: MetaData): MetaData =
    normalize(concatenate(updates, attribs), scope)

}

case object Null extends MetaData {
  def next  = null
  def key   = null
  def value = null

  def newCopy(next: MetaData) = next
}

final case class PrefixedAttribute[T: XmlAttributeEmbeddable](
    pre: String,
    key: String,
    expr: T,
    next: MetaData
) extends MetaData {

  val value: Node = expr match {
    case n: Node => n
    case _       => new Atom(expr)
  }

  def newCopy(newNext: MetaData) = this.copy(next = newNext)

}

final case class UnprefixedAttribute[T: XmlAttributeEmbeddable](
    key: String,
    expr: T,
    next: MetaData
) extends MetaData {
  val value: Node =
    expr match {
      case n: Node => n
      case _       => new Atom(expr)
    }

  def newCopy(newNext: MetaData) = this.copy(next = newNext)
}

/** Evidence that T can be embedded in xml attribute position. */
@implicitNotFound(
  msg = """Cannot embed value of type ${T} in xml attribute, implicit XmlAttributeEmbeddable[${T}] not found.
The following types are supported:
- String
- Boolean (false → remove attribute, true → empty attribute)
- () => Unit, T => Unit event handler. Note: The return type needs to be Unit!
- Var[T], Rx[T] where T is XmlAttributeEmbeddable
- Option[T] where T is XmlAttributeEmbeddable (None → remove from the DOM)
"""
)
trait XmlAttributeEmbeddable[T]
object XmlAttributeEmbeddable {
  type XA[T] = XmlAttributeEmbeddable[T]
  @inline implicit def noneAttributeEmbeddable: XA[None.type]                        = null
  @inline implicit def booleanAttributeEmbeddable: XA[Boolean]                       = null
  @inline implicit def stringAttributeEmbeddable: XA[String]                         = null
  @inline implicit def textNodeAttributeEmbeddable: XA[Text]                         = null
  @inline implicit def function0AttributeEmbeddable: XA[() => Unit]                  = null
  @inline implicit def function1AttributeEmbeddable[T]: XA[T => Unit]                = null
  @inline implicit def optionAttributeEmbeddable[C[x] <: Option[x], T: XA]: XA[C[T]] = null
  @inline implicit def rxAttributeEmbeddable[C[x] <: Rx[x], T: XA]: XA[C[T]]         = null
}

/** Evidence that T can be embedded in xml element position. */
@implicitNotFound(
  msg = """Cannot embed value of type ${T} in xml element, implicit XmlElementEmbeddable[${T}] not found.
The following types are supported:
- String, Int, Long, Double, Float, Char (converted with .toString)
- xml.Node, Seq[xml.Node]
- Var[T], Rx[T] where T is XmlElementEmbeddable
- Option[T] where T is XmlElementEmbeddable (None → remove from the DOM)
"""
)
trait XmlElementEmbeddable[T]
object XmlElementEmbeddable {
  type XE[T] = XmlElementEmbeddable[T]

  @inline implicit def nilElementEmbeddable: XE[Nil.type]                          = null
  @inline implicit def noneElementEmbeddable: XE[None.type]                        = null
  @inline implicit def intElementEmbeddable: XE[Int]                               = null
  @inline implicit def floatElementEmbeddable: XE[Float]                           = null
  @inline implicit def doubleElementEmbeddable: XE[Double]                         = null
  @inline implicit def longElementEmbeddable: XE[Long]                             = null
  @inline implicit def charElementEmbeddable: XE[Char]                             = null
  @inline implicit def stringElementEmbeddable: XE[String]                         = null
  @inline implicit def nodeElementEmbeddable[T <: Node]: XE[T]                     = null
  @inline implicit def optionElementEmbeddable[C[x] <: Option[x], T: XE]: XE[C[T]] = null
  @inline implicit def seqElementEmbeddable[C[x] <: Seq[x], T <: Node]: XE[C[T]]   = null
  @inline implicit def rxElementEmbeddable[C[x] <: Rx[x], T: XE]: XE[C[T]]         = null
  @inline implicit def rxElementEmbeddable: XE[RxElement]                          = null
}

/** Internal structure used by scalac to create literals */
class NodeBuffer extends Seq[Node] {
  private val underlying: mutable.ArrayBuffer[Node] = mutable.ArrayBuffer.empty
  def iterator: Iterator[Node]                      = underlying.iterator
  def apply(i: Int): Node                           = underlying.apply(i)
  def length: Int                                   = underlying.length
  override def toString: String                     = underlying.toString

  def &+(e: Node): NodeBuffer                       = { underlying.+=(e); this }
  def &+[A: XmlElementEmbeddable](e: A): NodeBuffer = { underlying.+=(new Atom(e)); this }
}
