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
package wvlet.airframe.rx.ml

/**
  *
  */
object html {

  class Namespace(val uri: String) extends AnyVal
  object Namespace {
    val xhtml: Namespace = new Namespace("http://www.w3.org/1999/xhtml")
    val svg: Namespace   = new Namespace("http://www.w3.org/2000/svg")
  }

  type MarkupElement
  //type Element
  //type Attribute

  trait HtmlNode

  trait ElementModifier

  class HtmlElement(name: String, modifiers: List[Seq[ElementModifier]]) extends HtmlNode {
    def apply(xs: HtmlElement*): HtmlElement = {
      new HtmlElement(name = name, modifiers = xs :: modifiers)
    }
  }

  class HtmlAttribute(name: String) {
    def :=[V](v: V): HtmlElement = {
      null
    }
  }

  def div: HtmlElement = new HtmlElement("div")
}
