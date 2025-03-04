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
import wvlet.airframe.rx.{Cancelable, Rx}
import wvlet.log.LogSupport

/**
  */
abstract class RxElement(val modifiers: List[Seq[HtmlNode]] = List.empty) extends HtmlNode with LogSupport { self =>

  /**
    * Render this element
    * @return
    */
  def render: RxElement

  /**
    * Called right before rendering this RxElement begins.
    *
    * Override this method to define a custom event hook before rendering.
    */
  def beforeRender: Unit = {}

  /**
    * Called right after mounting this RxElement to the document. Override this method to define a custom event hook
    * after rendering.
    *
    * @param node
    *   the mounted DOM node (org.scalajs.dom.Node in Scala.js)
    */
  def onMount(node: Any): Unit = RxElement.NoOp

  /**
    * Called right before unmounting (deleting) this RxElement from DOM.
    *
    * This is a good place to remove any background process or manually added event listeners.
    */
  def beforeUnmount: Unit = {}

  /**
    * Add child elements or attributes to this element and return this element
    */
  def apply(xs: HtmlNode*): RxElement = {
    if (xs.isEmpty) {
      this
    } else {
      addModifier(xs)
    }
  }

  /**
    * Add child nodes (elements or attributes) to this element
    * @param xs
    * @return
    */
  def addModifier(xs: HtmlNode*): RxElement = add(xs: _*)

  def add(xs: HtmlNode*): RxElement = {
    new RxElement(xs :: modifiers) {
      override def render = self.render
    }
  }

  private[html] def traverseModifiers(f: HtmlNode => Cancelable): Cancelable = {
    val cancelables = for (g <- modifiers.reverse; m <- g) yield {
      f(m)
    }
    Cancelable.merge(cancelables)
  }
}

object RxElement {
  def apply(a: RxElement): RxElement =
    new RxElement() {
      override def render: RxElement = a
    }
  def apply[A <: RxElement](a: Rx[A]): RxElement =
    new RxElement() {
      override def render: RxElement = LazyRxElement(() => a)
    }

  private[html] val NoOp = { (n: Any) => }

}

case class LazyRxElement[A: EmbeddableNode](v: () => A) extends RxElement() with LogSupport {
  override def render: RxElement = Embedded(v())
}
