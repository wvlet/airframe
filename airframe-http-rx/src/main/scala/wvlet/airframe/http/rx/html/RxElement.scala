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
package wvlet.airframe.http.rx.html

import wvlet.airframe.http.rx.{Cancelable, Rx}
import wvlet.log.LogSupport

/**
  *
  */
abstract class RxElement(val modifiers: List[Seq[HtmlNode]] = List.empty) extends HtmlNode { self =>

  /**
    * Render this element
    * @return
    */
  def render: RxElement

  def apply(xs: HtmlNode*): RxElement = {
    if (xs.isEmpty) {
      this
    } else {
      addModifier(xs)
    }
  }

  def add(xs: HtmlNode*): RxElement = new RxElement(xs :: modifiers) {
    override def render = self.render
  }
  def addModifier(xs: HtmlNode*): RxElement = add(xs: _*)

  private[html] def traverseModifiers(f: HtmlNode => Cancelable): Cancelable = {
    val cancelables = for (g <- modifiers.reverse; m <- g) yield {
      f(m)
    }
    Cancelable.merge(cancelables)
  }
}

object RxElement {

  def apply(a: RxElement): RxElement = new RxElement() {
    override def render: RxElement = a
  }
  def apply[A <: RxElement](a: Rx[A]): RxElement = new RxElement() {
    override def render: RxElement = LazyRxElement(() => a)
  }
}

case class LazyRxElement[A: EmbeddableNode](v: () => A) extends RxElement() with LogSupport {
  override def render: RxElement = Embedded(v())
}

case class HtmlElement(
    name: String,
    namespace: Namespace = Namespace.xhtml,
    override val modifiers: List[Seq[HtmlNode]] = List.empty
) extends RxElement(modifiers) {
  def render: RxElement = this

  override def addModifier(cs: HtmlNode*): HtmlElement = {
    HtmlElement(name, namespace, cs :: modifiers)
  }
}
