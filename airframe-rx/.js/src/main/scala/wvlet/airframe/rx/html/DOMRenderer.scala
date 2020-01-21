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
import wvlet.log.LogSupport

/**
  *
  */
object DOMRenderer extends LogSupport {

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

  private def newTextNode(s: String): dom.Text = dom.document.createTextNode(s)

  def renderTo(node: dom.Node, htmlNode: HtmlNode): Cancelable = {

    def traverse(v: Any, anchor: Option[dom.Node]): Cancelable = {
      v match {
        case HtmlNode.empty =>
          Cancelable.empty
        case e: HtmlElement =>
          // TODO renderer
          val (childDOM, c1) = render(e)
          node.mountHere(childDOM, anchor)
          c1
        case rx: Rx[_] =>
          val (start, end) = node.createMountSection()
          var c1           = Cancelable.empty
          val c2 = rx.subscribe { value =>
            // Remove the previous binding from the DOM
            node.clearMountSection(start, end)
            // Cancel the previous binding
            c1.cancel
            c1 = traverse(value, Some(start))
          }
          Cancelable.merge(c1, c2)
        case HtmlAttribute(name, value, ns) =>
          addAttribute(node, name, value)
        case n: dom.Node =>
          node.mountHere(n, anchor)
          Cancelable.empty
        case a: Embedded =>
          traverse(a.v, anchor)
        case s: String =>
          val textNode = newTextNode(s)
          node.mountHere(textNode, anchor)
          Cancelable.empty
        case v: Int =>
          val textNode = newTextNode(v.toString)
          node.mountHere(textNode, anchor)
          Cancelable.empty
        case v: Long =>
          val textNode = newTextNode(v.toString)
          node.mountHere(textNode, anchor)
          Cancelable.empty
        case v: Float =>
          val textNode = newTextNode(v.toString)
          node.mountHere(textNode, anchor)
          Cancelable.empty
        case v: Double =>
          val textNode = newTextNode(v.toString)
          node.mountHere(textNode, anchor)
          Cancelable.empty
        case v: Char =>
          val textNode = newTextNode(v.toString)
          node.mountHere(textNode, anchor)
          Cancelable.empty
        case b: Boolean =>
          val textNode = newTextNode(b.toString)
          node.mountHere(textNode, anchor)
          Cancelable.empty
        case None =>
          Cancelable.empty
        case Some(x) =>
          traverse(x, anchor)
        case s: Seq[_] =>
          val cancelables = for (el <- s) yield {
            traverse(el, anchor)
          }
          Cancelable.merge(cancelables)
        case other =>
          throw new IllegalArgumentException(s"unsupported: ${other}")
      }
    }

    traverse(htmlNode, anchor = None)
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
            // Cancel the previous binding
            c1.cancel
            c1 = traverse(value)
          }
          Cancelable.merge(c1, c2)
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

    /**
      * Create a two text nodes for embedding an Rx element.
      *
      * This is a workaround for that DOM API only expose `.insertBefore`
      */
    def createMountSection(): (dom.Node, dom.Node) = {
      val start = newTextNode("")
      val end   = newTextNode("")
      node.appendChild(end)
      node.appendChild(start)
      (start, end)
    }

    def mountHere(child: dom.Node, start: Option[dom.Node]): Unit = {
      start.fold(node.appendChild(child))(point => node.insertBefore(child, point)); ()
    }

    def clearMountSection(start: dom.Node, end: dom.Node): Unit = {
      val next = start.previousSibling
      if (next != end) {
        node.removeChild(next)
        clearMountSection(start, end)
      }
    }
  }
}
