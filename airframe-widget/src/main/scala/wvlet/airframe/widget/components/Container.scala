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
package wvlet.airframe.widget.components
import wvlet.airframe.widget.{RxComponent, RxElement}

import scala.xml.Node

/**
  *
  */
case class Container(style: String = "container") extends RxComponent {
  override def body(content: xml.Node*): xml.Node = <div class={style}>
    {content}
  </div>
}

case class Elem(elem: xml.Node) extends RxElement {
  override def body: Node = elem
}

object Container {
  def of(elems: RxElement*): RxElement = {
    new Container().apply(elems: _*)
  }

  def ofList(elems: Seq[RxElement]): RxElement = of(elems: _*)

  def apply(nodes: xml.Node*): RxElement = {
    Elem(new Container().body(nodes: _*))
  }
}
