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

/**
  */
package object html extends HtmlCompat with RxEmbeddingSupport {

  object tags       extends Tags
  object tags_extra extends TagsExtra
  object attrs      extends Attrs

  object all extends Tags with Attrs with RxEmbeddingSupport

  object svgTags  extends SvgTags
  object svgAttrs extends SvgAttrs

  trait HtmlNode {
    def when(cond: => Boolean): HtmlNode = {
      if (cond) this else HtmlNode.empty
    }
    def unless(cond: => Boolean): HtmlNode = {
      if (cond) HtmlNode.empty else this
    }
  }

  // HtmlNode -> Element -> HtmlElement
  //          -> HtmlAttribute

  object HtmlNode {
    object empty extends HtmlNode
  }

  case class HtmlAttribute(name: String, v: Any, ns: Namespace = Namespace.xhtml, append: Boolean = false)
      extends HtmlNode

  class HtmlAttributeOf(name: String, namespace: Namespace = Namespace.xhtml) {
    def apply[V: EmbeddableAttribute](v: V): HtmlNode = HtmlAttribute(name, v, namespace)
    def ->[V: EmbeddableAttribute](v: V): HtmlNode    = apply(v)
    def add[V: EmbeddableAttribute](v: V): HtmlNode   = HtmlAttribute(name, v, namespace, append = true)
    def +=[V: EmbeddableAttribute](v: V): HtmlNode    = add(v)
    def noValue: HtmlNode                             = HtmlAttribute(name, true, namespace)
  }

  class HtmlEventHandlerOf[E](name: String, namespace: Namespace = Namespace.xhtml) {
    def apply[U](v: E => U): HtmlNode  = HtmlAttribute(name, v, namespace)
    def ->[U](v: E => U): HtmlNode     = apply(v)
    def apply[U](v: () => U): HtmlNode = HtmlAttribute(name, v, namespace)
    def ->[U](v: () => U): HtmlNode    = apply(v)
    def noValue: HtmlNode              = HtmlAttribute(name, false, namespace)
  }

  case class EntityRef(ref: String) extends HtmlNode

  // TODO embed namespace properly to DOM
  case class Namespace(uri: String)

  object Namespace {
    val xhtml: Namespace = Namespace("http://www.w3.org/1999/xhtml")
    val svg: Namespace   = Namespace("http://www.w3.org/2000/svg")
    val svgXLink         = Namespace("http://www.w3.org/1999/xlink")
  }

  def tag(name: String): HtmlElement            = new HtmlElement(name)
  def tagOf(name: String, namespace: Namespace) = new HtmlElement(name, namespace)

  def attr(name: String): HtmlAttributeOf                       = new HtmlAttributeOf(name)
  def attr(name: String, namespace: Namespace): HtmlAttributeOf = new HtmlAttributeOf(name, namespace)
  def attributeOf(name: String): HtmlAttributeOf                = attr(name)

  def svgTag(name: String) = new HtmlElement(name, Namespace.svg)

  def handler[T](name: String): HtmlEventHandlerOf[T]                       = new HtmlEventHandlerOf[T](name)
  def handler[T](name: String, namespace: Namespace): HtmlEventHandlerOf[T] = new HtmlEventHandlerOf[T](name, namespace)

  private[rx] case class RxCode(rxElements: Seq[RxElement], sourceCode: String)

}
