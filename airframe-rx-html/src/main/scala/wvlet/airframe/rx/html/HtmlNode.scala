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
package wvlet.airframe.rx.html
import wvlet.airframe.rx.html.RxEmbedding.*

trait HtmlNode extends HtmlNodeBase {
  @deprecated("Use html.when(cond, node)", since = "23.7.0")
  def when(cond: => Boolean): HtmlNode = {
    if (cond) this else HtmlNode.empty
  }

  @deprecated("Use html.when(!cond, node)", since = "23.7.0")
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

// Used for properly embedding namespaces to DOM. For example, for rendering SVG elements,
// Namespace.svg must be set to DOM elements. If not, the SVG elements will not be rendered in the web browsers.
case class Namespace(uri: String)

object Namespace {
  val xhtml: Namespace = Namespace("http://www.w3.org/1999/xhtml")
  val svg: Namespace   = Namespace("http://www.w3.org/2000/svg")
  val svgXLink         = Namespace("http://www.w3.org/1999/xlink")
}
