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

import org.scalajs.dom
import wvlet.airframe.rx.{Cancelable, Rx}

case class Elem(body: () => Embedded) extends RxElement {
  override def render: HtmlNode = body()
}

/**
  * A placeholder for rendering elements lazily
  */
private[html] case class LazyElement(elem: RxElement)
private[html] case class LazyElementSeq(elems: Seq[RxElement])

/**
  *
  */
trait RxComponent {
  def render(content: HtmlNode): HtmlNode

  def apply(elems: HtmlNode*): RxElement = {
    elems.size match {
      case 1     => Elem(() => Embedded(elems.head))
      case other => Elem(() => Embedded(elems.toSeq))
    }
  }
  def apply[A: EmbeddableNode](elem: A): HtmlNode = {
    Elem(() => Embedded(elem))
  }
}

object RxComponent {
  def ofTag(name: String): RxComponent = new RxComponent { content =>
    override def render(content: HtmlNode): HtmlNode = {
      tag(name)(content)
    }
  }

  def apply(f: HtmlNode => HtmlNode): RxComponent = new RxComponent {
    override def render(content: HtmlNode): HtmlNode = {
      f(content)
    }
  }
}

trait RxElement extends HtmlNode {
  def render: HtmlNode

  def mountTo(parent: dom.Node): Cancelable = {
    DOMRenderer.renderTo(parent, render)
  }
}

object RxElement {
  def apply(a: HtmlElement): RxElement = new RxElement {
    override def render: HtmlNode = a
  }
  def apply(a: RxElement): RxElement = new RxElement {
    override def render: HtmlNode = Embedded(a)
  }
  def apply[A](a: Rx[A]): RxElement = new RxElement {
    override def render: HtmlNode = Embedded(a)
  }
}
