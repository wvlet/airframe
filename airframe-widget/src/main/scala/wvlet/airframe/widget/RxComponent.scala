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
package wvlet.airframe.widget

import org.scalajs.dom
import wvlet.airframe.widget.components.{Elem, Text}

import scala.xml.{MetaData, Node, UnprefixedAttribute}

/**
  *
  */
trait RxComponent {
  def apply(elems: RxElement*): RxElement = Elem(render(elems.map(_.render): _*))
  def apply(elem: String): RxElement      = Elem(render(scala.xml.Text(elem)))

  def render(content: xml.Node*): xml.Node
}

/**
  *
  */
trait RxElement {
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

  def withBorder: RxComponentBuilder        = withClasses("border")
  def withRoundedCorner: RxComponentBuilder = withClasses("rounded")
  def withShadow: RxComponentBuilder        = withClasses("shadow-sm")

  def withOverflowAuto   = withClasses("overflow-auto")
  def withOverflowHidden = withClasses("overflow-hidden")

  def withPositionStatic   = withClasses("position-static")
  def withPositionRelative = withClasses("position-relative")
  def withPositionAbsolute = withClasses("position-absolute")
  def withPositionFixed    = withClasses("position-fixed")
  def withPositionSticky   = withClasses("position-sticky")

  def withFixedTop    = withClasses("fixed-top")
  def withFixedBottom = withClasses("fixed-bottom")
  def withStickyTop   = withClasses("sticky-top")

  def withAlertLink = withClasses("alert-link")

  def apply(content: String): RxElement = apply(Text(content))

  def apply(elems: RxElement*): RxElement =
    new RxComponent {
      override def render(content: Node*): Node = {
        var attrs: MetaData = scala.xml.Null
        attributes.foreach {
          case (key, values) =>
            attrs = attrs.append(UnprefixedAttribute(key, values.mkString(" "), scala.xml.Null))
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
    }.apply(elems: _*)
}
