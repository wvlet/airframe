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

import scala.xml.{Attribute, Node}

/**
  *
  */
trait RxComponent {
  def apply(elems: RxElement*): RxElement = Elem(body(elems.map(_.body): _*))
  def apply(elem: String): RxElement      = Elem(body(scala.xml.Text(elem)))

  def body(content: xml.Node*): xml.Node
}

/**
  *
  */
trait RxElement {
  def body: xml.Node

  def appendTo(parent: dom.Element): dom.Element = {
    RxDOM.mount(parent, body)
    parent
  }

  def render: dom.Element = {
    val node = dom.document.createElement("div")
    RxDOM.mount(node, body)
    node
  }
}

case class RxComponentBuilder(tag: String, primaryClass: Option[String] = None, otherClasses: Seq[String] = Seq.empty) {
  def withClasses(classes: String*) = this.copy(otherClasses = otherClasses ++ classes)

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
  def withSticyTop    = withClasses("sticky-top")

  def apply(content: String): RxElement = apply(Text(content))

  def apply(elems: RxElement*): RxElement =
    new RxComponent {
      override def body(content: Node*): Node = {
        val classes = Seq.newBuilder[String]
        primaryClass.foreach(classes += _)
        classes ++= otherClasses

        val cls = Attribute.apply(null, "class", classes.result().mkString(" "), scala.xml.Null)
        val elem = scala.xml
          .Elem(
            prefix = null,
            label = tag,
            attributes = cls,
            scope = scala.xml.TopScope,
            minimizeEmpty = true,
            child = elems.map(_.body): _*
          )
        elem
      }
    }.apply(elems: _*)
}
