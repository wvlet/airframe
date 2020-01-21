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
import wvlet.airframe.rx.{Cancelable, Rx}

import scala.scalajs.js
import org.scalajs.dom

/**
  *
  */
object DOMRenderer {

  def renderToHtml(node: dom.Node): String = {
    node match {
      case e: dom.Element =>
        e.outerHTML
      case _ =>
        node.innerText
    }
  }

  def render(e: HtmlElement): (dom.Node, Cancelable) = {
    val node: dom.Node = dom.document.createElement(e.name)
    val cancelables = for (g <- e.modifiers.reverse; m <- g) yield {
      renderTo(node, m)
    }
    (node, Cancelable.merge(cancelables))
  }

  def renderTo(node: dom.Node, mod: HtmlNode): Cancelable = {

    def traverse(v: Any): Cancelable = {
      v match {
        case HtmlNode.empty =>
          Cancelable.empty
        case e: HtmlElement =>
          // TODO renderer
          val (childDOM, c1) = render(e)
          node.appendChild(childDOM)
          c1
        case rx: Rx[_] =>
          var c1 = Cancelable.empty
          val c2 = rx.run { value =>
            c1.cancel
            c1 = traverse(value)
          }
          Cancelable { () =>
            c1.cancel; c2.cancel
          }
        case HtmlAttribute(name, value) =>
          addAttribute(node, name, value)
        case a: Embed =>
          a.v match {
            case i: Int =>
              val textNode = dom.document.createTextNode(i.toString)
              node.appendChild(textNode)
              Cancelable.empty
            case s: String =>
              val textNode = dom.document.createTextNode(s)
              node.appendChild(textNode)
              Cancelable.empty
            case s: Seq[_] =>
              val cancelables = for (el <- s) yield {
                traverse(el)
              }
              Cancelable.merge(cancelables)
            case other =>
              throw new IllegalArgumentException(s"unsupported: ${other}")
          }
        case _ =>
          throw new IllegalArgumentException(s"unsupported: ${mod}")
      }
    }

    traverse(mod)
  }

  private def addAttribute(node: dom.Node, name: String, value: Any): Cancelable = {
    val htmlNode = node.asInstanceOf[dom.html.Html]

    def traverse(v: Any): Cancelable = {
      v match {
        case null | None | false =>
          htmlNode.removeAttribute(name)
          Cancelable.empty
        case Some(x) =>
          traverse(x)
        case rx: Rx[_] =>
          var c1 = Cancelable.empty
          val c2 = rx.run { value =>
            c1.cancel
            c1 = traverse(value)
          }
          Cancelable { () =>
            c1.cancel; c2.cancel
          }
        case f: Function0[Unit @unchecked] =>
          node.setEventListener(name, (_: dom.Event) => f())
        case f: Function1[dom.Node @unchecked, Unit @unchecked] =>
          node.setEventListener(name, f)
        case _ =>
          val value = v match {
            case true => ""
            case _    => v.toString
          }
          name match {
            case "style" =>
              val prev = htmlNode.style.cssText
              if (prev.isEmpty) {
                htmlNode.style.cssText = s"${prev} ${v}"
              } else {
                htmlNode.style.cssText = value
              }
              Cancelable.empty
            case _ =>
              htmlNode.setAttribute(name, value)
              Cancelable.empty
          }
      }
    }

    traverse(value)
  }

  private implicit class RichDomNode(node: dom.Node) {
    def setEventListener[A](key: String, listener: A => Unit): Cancelable = {
      val dyn = node.asInstanceOf[js.Dynamic]
      dyn.updateDynamic(key)(listener)
      Cancelable(() => dyn.updateDynamic(key)(null))
    }
  }
}
