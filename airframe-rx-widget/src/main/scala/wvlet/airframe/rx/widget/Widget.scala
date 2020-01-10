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
package wvlet.airframe.rx.widget

import org.scalajs.dom
import wvlet.airframe.rx.widget.ui.Elem

import scala.xml

/**
  *
  */
case class WidgetConfig(idOpt: Option[String] = None, attributes: Map[String, Seq[String]] = Map.empty) {

  def withId(id: String): WidgetConfig = this.copy(idOpt = Some(id))

  def setAttribute(attrName: String, attrValue: String): WidgetConfig = {
    this.copy(attributes = attributes + (attrName -> Seq(attrValue)))
  }

  def addAttribute(attrName: String, attrValue: String): WidgetConfig = {
    this.copy(attributes = attributes + (attrName -> (attributes.getOrElse(attrName, Seq.empty[String]) :+ attrValue)))
  }

  def addStyle(styleName: String, value: String): WidgetConfig = {
    addAttribute("style", s"${styleName}: ${value};")
  }

  def addClass(className: String): WidgetConfig = {
    addAttribute("class", className)
  }
}

trait Widget {
  protected val config: WidgetConfig

  protected def withConfig(newConfig: WidgetConfig): this.type

  def withId(id: String): this.type = withConfig(config.withId(id))

}

trait WidgetComponent extends Widget {
  def render(content: xml.Node*): xml.Node

  def apply(elems: RxElement*): RxElement = Elem(render(elems.map(_.render): _*))
  def apply(elem: String): RxElement      = Elem(render(scala.xml.Text(elem)))
}

trait WidgetElement extends Widget {
  def render: xml.Node

  def appendTo(parent: dom.Element): dom.Element = {
    RxDOM.mount(parent, render)
    parent
  }

  def toDOM: dom.Element = {
    val node = dom.document.createElement("div")
    RxDOM.mount(node, render)
    node
  }
}

object Widget {}
