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
package wvlet.airframe.rx
import scala.annotation.implicitNotFound

/**
  *
  */
package object html {

  trait HtmlNode {
    def when(cond: => Boolean): HtmlNode = {
      if (cond) this else HtmlNode.empty
    }
    def unless(cond: => Boolean): HtmlNode = {
      if (cond) HtmlNode.empty else this
    }
  }

  object HtmlNode {
    object empty extends HtmlNode
  }

  case class HtmlAttribute(name: String, v: Any) extends HtmlNode

  class HtmlAttributeOf(name: String) {
    def apply[V](v: V): HtmlNode = HtmlAttribute(name, v)
  }

  class HtmlElement(val name: String, val modifiers: List[Seq[HtmlNode]] = List.empty) extends HtmlNode {
    def apply(xs: HtmlNode*): HtmlElement = {
      if (xs.isEmpty) {
        this
      } else {
        new HtmlElement(name = name, modifiers = xs :: modifiers)
      }
    }
  }

  case class Namespace(uri: String)

  object Namespace {
    val xhtml: Namespace = Namespace("http://www.w3.org/1999/xhtml")
    val svg: Namespace   = Namespace("http://www.w3.org/2000/svg")
  }

  def tag(name: String): HtmlElement             = new HtmlElement(name)
  def attr(name: String): HtmlAttributeOf        = new HtmlAttributeOf(name)
  def attributeOf(name: String): HtmlAttributeOf = new HtmlAttributeOf(name)

  @implicitNotFound(msg = "Unsupported type as an attribute value")
  trait EmbeddableAttribute[X]
  object EmbeddableAttribute {
    type EA[A] = EmbeddableAttribute[A]
    @inline implicit def embedNone: EA[None.type]                        = null
    @inline implicit def embedBoolean: EA[Boolean]                       = null
    @inline implicit def embedString: EA[String]                         = null
    @inline implicit def embedF0: EA[() => Unit]                         = null
    @inline implicit def embedF1[I]: EA[I => Unit]                       = null
    @inline implicit def embedOption[C[x] <: Option[x], A: EA]: EA[C[A]] = null
    @inline implicit def embedRx[C[x] <: Rx[x], A: EA]: EA[C[A]]         = null
  }

  @implicitNotFound(msg = "Unsupported type as an HtmlNode")
  trait EmbeddableNode[A]
  object EmbeddableNode {
    type EN[A] = EmbeddableNode[A]
    @inline implicit def embedNil: EN[Nil.type]                          = null
    @inline implicit def embedNone: EN[None.type]                        = null
    @inline implicit def embedBoolean: EN[Boolean]                       = null
    @inline implicit def embedInt: EN[Int]                               = null
    @inline implicit def embedChar: EN[Char]                             = null
    @inline implicit def embedShort: EN[Short]                           = null
    @inline implicit def embedByte: EN[Byte]                             = null
    @inline implicit def embedLong: EN[Long]                             = null
    @inline implicit def embedFloat: EN[Float]                           = null
    @inline implicit def embedDouble: EN[Double]                         = null
    @inline implicit def embedString: EN[String]                         = null
    @inline implicit def embedHtmlNode[A <: HtmlNode]: EN[A]             = null
    @inline implicit def embedRx[C[x] <: Rx[x], A: EN]: EN[C[A]]         = null
    @inline implicit def embedSeq[C[x] <: Seq[x], A: EN]: EN[C[A]]       = null
    @inline implicit def embedOption[C[x] <: Option[x], A: EN]: EN[C[A]] = null
  }

  /**
    * Holder for embedding various types as tag contents
    * @param v
    */
  case class Embed(v: Any) extends HtmlNode

  implicit def embedAsNode[A: EmbeddableNode](v: A): HtmlNode = Embed(v)
}
