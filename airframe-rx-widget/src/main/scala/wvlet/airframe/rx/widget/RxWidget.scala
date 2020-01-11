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
import wvlet.airframe.rx.Cancelable
import wvlet.airframe.rx.widget.ui.Elem

import scala.xml.Node

trait RxWidget {
  protected var config: RxWidgetConfig = new RxWidgetConfig()

  private[widget] def getConfig = config

  protected def updateConfig(newConfig: RxWidgetConfig): this.type = {
    config = newConfig
    this
  }

  def withId(id: String): this.type = updateConfig(config.withId(id))

  /**
    * Set an extra CSS class
    */
  def addClass(className: String): this.type = updateConfig(config.addClass(className))
  def clearClass: this.type                  = updateConfig(config.removeAttribute("class"))
  def clearStyle: this.type                  = updateConfig(config.removeAttribute("reset"))

  /**
    * Add an extra CSS style
    */
  def addStyle(styleValue: String): this.type = updateConfig(config.addStyle(styleValue))
}

/**
  * Base trait of reactive component that can take a content value and produce a DOM element
  */
trait RxComponent extends RxWidget {
  def render(content: xml.Node): xml.Node

  def apply(elems: RxElement*): RxElement = Elem(() => render(xml.Group(elems.map(_.render))))
  def apply(elem: String): RxElement      = Elem(() => render(scala.xml.Text(elem)))
  def apply(elem: xml.Node): RxElement    = Elem(() => render(elem))
}

object RxComponent {

  def apply(f: xml.Node => xml.Node): RxComponent = new RxComponent {
    override def render(content: Node): Node = f(content)
  }

  def ofTag(tag: String): RxComponent = RxComponent { content =>
    val elem = scala.xml.Elem(
      prefix = null,
      label = tag,
      attributes1 = xml.Null,
      scope = scala.xml.TopScope,
      minimizeEmpty = true,
      child = content
    )
    elem
  }
}

/**
  * Base trait of reactive element that can produce a single DOM element
  */
trait RxElement extends RxWidget {
  def render: xml.Node

  /**
    * Return the final XML node of this Widget after setting all configurations
    */
  def mountTo(parent: dom.Node): Cancelable = {
    RxDOM.mountTo(parent, this)
  }
}

object RxElement {
  def apply(node: xml.Node): RxElement = new RxElement { override def render: Node = node }
}

/**
  *
  */
case class RxWidgetConfig(
    id: Option[String] = None,
    attributes: Map[String, Seq[String]] = Map.empty,
    onClickHandler: Option[dom.MouseEvent => Unit] = None,
    onEventHandler: Option[dom.Event => Unit] = None
) {

  // TODO: Add all event types https://www.w3schools.com/jsref/dom_obj_event.asp
  def onClick(handler: dom.MouseEvent => Unit) = this.copy(onClickHandler = Some(handler))
  def onEvent(handler: dom.Event => Unit)      = this.copy(onEventHandler = Some(handler))

  def withId(id: String): RxWidgetConfig = this.copy(id = Some(id))

  def setAttribute(attrName: String, attrValue: String): RxWidgetConfig = {
    this.copy(attributes = attributes + (attrName -> Seq(attrValue)))
  }

  def removeAttribute(attrName: String): RxWidgetConfig = {
    this.copy(attributes = attributes - attrName)
  }

  def appendAttribute(attrName: String, attrValue: String): RxWidgetConfig = {
    this.copy(attributes = attributes + (attrName -> (attributes.getOrElse(attrName, Seq.empty[String]) :+ attrValue)))
  }

  def addStyle(styleString: String): RxWidgetConfig = {
    appendAttribute("style", styleString)
  }

  def addClass(className: String): RxWidgetConfig = {
    appendAttribute("class", className)
  }
}
