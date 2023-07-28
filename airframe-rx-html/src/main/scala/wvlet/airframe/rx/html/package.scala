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

import wvlet.log.LogSupport

/**
  */
package object html extends HtmlCompat with RxEmbedding {

  object tags       extends Tags
  object tags_extra extends TagsExtra
  object attrs      extends Attrs

  object all extends Tags with Attrs with RxEmbedding

  // Note: SVG tags and attributes are defined separately to resolve naming conflicts with regular HTML tags and attributes
  object svgTags  extends SvgTags
  object svgAttrs extends SvgAttrs

  def tag(name: String): HtmlElement            = new HtmlElement(name)
  def tagOf(name: String, namespace: Namespace) = new HtmlElement(name, namespace)

  def attr(name: String): HtmlAttributeOf                       = new HtmlAttributeOf(name)
  def attr(name: String, namespace: Namespace): HtmlAttributeOf = new HtmlAttributeOf(name, namespace)
  def attributeOf(name: String): HtmlAttributeOf                = attr(name)

  def svgTag(name: String) = new HtmlElement(name, Namespace.svg)

  def handler[T](name: String): HtmlEventHandlerOf[T]                       = new HtmlEventHandlerOf[T](name)
  def handler[T](name: String, namespace: Namespace): HtmlEventHandlerOf[T] = new HtmlEventHandlerOf[T](name, namespace)

  private[rx] case class RxCode(rxElements: Seq[RxElement], sourceCode: String)

  /**
    * Render an element only when the condition is true
    *
    * @param cond
    * @param body
    * @return
    *   rendered element or empty
    */
  def when(cond: Boolean, body: => HtmlNode): HtmlNode = {
    if (cond) {
      body
    } else {
      HtmlNode.empty
    }
  }

  /**
    * Holder for embedding various types as tag contents
    *
    * @param v
    */
  private[html] case class Embedded(v: Any) extends RxElement with LogSupport {
    override def render: RxElement = {
      warn(s"render is called for ${v}")
      ???
    }
  }
}
