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
import wvlet.airframe.rx.widget.ui.{Elem, Text}

import scala.xml
import scala.xml.{MetaData, Node}

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

trait RxComponent extends RxWidget {
  def render(content: xml.Node): xml.Node

  def apply(elems: RxElement*): RxElement = Elem(() => render(xml.Group(elems.map(_.render))))
  def apply(elem: String): RxElement      = Elem(() => render(scala.xml.Text(elem)))
  def apply(elem: xml.Node): RxElement    = Elem(() => render(elem))
}

trait RxElement extends RxWidget {
  def render: xml.Node

  /**
    * Return the final XML node of this Widget after setting all configurations
    */
  def mountTo(parent: dom.Node): Cancelable = {
    RxDOM.mountTo(parent, this)
  }
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

object RxComponentBuilder {
  def apply(tag: String)                       = new RxComponentBuilder(tag)
  def apply(tag: String, primaryClass: String) = new RxComponentBuilder(tag, Map("class" -> Seq(primaryClass)))
}

case class RxComponentBuilder(
    tag: String,
    attributes: Map[String, Seq[String]] = Map.empty,
    roles: Seq[String] = Seq.empty
) {

  def addAttribute(attrName: String, value: String*) = this.copy(
    attributes = attributes + (attrName -> (attributes.getOrElse("class", Seq.empty) ++ value))
  )

  def withClasses(classes: String*) = addAttribute("class", classes: _*)
  def withRoles(newRoles: String*)  = addAttribute("role", newRoles: _*)

  def apply(content: String): RxElement = apply(ui.Text(content))

  def apply(elems: RxElement*): RxElement = new RxElement {
    override def render: Node = {
      var attrs: MetaData = scala.xml.Null
      attributes.foreach {
        case (key, values) =>
          attrs = attrs.append(xml.UnprefixedAttribute(key, values.mkString(" "), scala.xml.Null))
      }

      val elem = scala.xml
        .Elem(
          prefix = null,
          label = tag,
          attributes1 = attrs,
          scope = scala.xml.TopScope,
          minimizeEmpty = true,
          child = elems.map(_.render): _*
        )
      elem
    }
  }
}
