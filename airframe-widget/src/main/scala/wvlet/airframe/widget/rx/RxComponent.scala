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
package wvlet.airframe.widget.rx

import org.scalajs.dom
import wvlet.airframe.widget.dom.VirtualDOM
import wvlet.airframe.widget.ui.components.Elem

/**
  *
  */
trait RxComponent {
  def apply(elems: RxElement*): RxElement = Elem(body(elems.map(_.body): _*))
  def apply(elem: String): RxElement      = Elem(body(scala.xml.Text(elem)))

  def body(content: xml.Node*): xml.Node
}

trait RxNode {
  def appendTo(parent: dom.Element): dom.Element
}

/**
  *
  */
trait RxElement extends RxNode {
  def body: xml.Node

  override def appendTo(parent: dom.Element): dom.Element = {
    val node = body
    VirtualDOM.mount(parent, node)
    parent
  }
}
