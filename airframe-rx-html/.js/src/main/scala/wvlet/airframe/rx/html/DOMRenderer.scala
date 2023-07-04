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
import wvlet.airframe.rx.{Cancelable, Rx, RxOps}
import wvlet.log.LogSupport

import scala.scalajs.js

/**
  * Convert HtmlNodes into DOM elements for Scala.js.
  *
  * An important functionality of this renderer is composing Cancelable objects so that resources allocated along with
  * the rendered DOM objects will be properly discarded.
  *
  * Resources include event listeners, Rx subscribers, etc.
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

  private def createNode(e: HtmlElement): dom.Node = {
    val elem = e.namespace match {
      case Namespace.xhtml => dom.document.createElement(e.name)
      case _               => dom.document.createElementNS(e.namespace.uri, e.name)
    }
    elem
  }

  def render(e: RxElement): (dom.Node, Cancelable) = {
    def traverse(v: Any): (dom.Node, Cancelable) = {
      v match {
        case h: HtmlElement =>
          val node: dom.Node = createNode(h)
          val cancelable     = h.traverseModifiers(m => renderTo(node, m))
          (node, cancelable)
        case l: LazyRxElement[_] =>
          traverse(l.render)
        case Embedded(v) =>
          traverse(v)
        case r: RxElement =>
          r.beforeRender
          val (n, c) = traverse(r.render)
          r.afterRender
          (n, Cancelable.merge(Cancelable(() => r.beforeUnmount), c))
        case d: dom.Node =>
          (d, Cancelable.empty)
        case other =>
          throw new IllegalArgumentException(s"unsupported top level element: ${other}. Use renderTo")
      }
    }

    traverse(e)
  }

  private def newTextNode(s: String): dom.Text = dom.document.createTextNode(s)

  def renderTo(node: dom.Node, htmlNode: HtmlNode, modifier: dom.Node => dom.Node = identity): Cancelable = {

    def traverse(v: Any, anchor: Option[dom.Node]): Cancelable = {
      v match {
        case HtmlNode.empty =>
          Cancelable.empty
        case e: HtmlElement =>
          val elem = createNode(e)
          val c    = e.traverseModifiers(m => renderTo(elem, m))
          node.mountHere(elem, anchor)
          c
        case rx: RxOps[_] =>
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
        case a: HtmlAttribute =>
          addAttribute(node, a)
        case n: dom.Node =>
          node.mountHere(n, anchor)
          Cancelable.empty
        case e: Embedded =>
          traverse(e.v, anchor)
        case rx: RxElement =>
          rx.beforeRender
          val c1   = renderTo(node, rx.render)
          val elem = node.lastChild
          val c2   = rx.traverseModifiers(m => renderTo(elem, m))
          rx.afterRender
          node.mountHere(elem, anchor)
          Cancelable.merge(Cancelable(() => rx.beforeUnmount), Cancelable.merge(c1, c2))
        case s: String =>
          val textNode = newTextNode(s)
          node.mountHere(textNode, anchor)
          Cancelable.empty
        case EntityRef(entityName) =>
          // Wrap entity ref with a span tag.
          // This is a workaround if the text is inserted in the middle of text element.
          val domNode = dom.document.createElement("span");
          val entity = {
            var x = entityName.trim
            if (!x.startsWith("&")) {
              x = s"&${x}"
            }
            if (!x.endsWith(";")) {
              x = s"${x};"
            }
            x
          }
          domNode.innerHTML = entity
          node.mountHere(domNode, anchor)
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
        case s: Iterable[_] =>
          val cancellables = for (el <- s) yield {
            traverse(el, anchor)
          }
          Cancelable.merge(cancellables)
        case other =>
          throw new IllegalArgumentException(s"unsupported class ${other}")
      }
    }

    traverse(htmlNode, anchor = None)
  }

  private def removeStringFromAttributeValue(
      attrValue: String,
      toRemove: String
  ): String = {
    if (attrValue == null) {
      ""
    } else {
      attrValue
        .split("""\s+""")
        .filter(x => x != toRemove)
        .mkString(" ")
    }
  }

  private def removeStyleValue(
      styleValue: String,
      toRemove: String
  ): String = {
    if (styleValue == null) {
      ""
    } else {
      val targetValueToRemove = toRemove.trim

      import scala.util.chaining._
      styleValue.trim
        .split(""";\s*""")
        .map(x => s"${x};")
        .filter(x => x != targetValueToRemove)
        .mkString("; ")
        .pipe { x =>
          if (x.nonEmpty) s"${x};" else x
        }
    }
  }

  private def addAttribute(node: dom.Node, a: HtmlAttribute): Cancelable = {
    val htmlNode = node.asInstanceOf[dom.html.Html]

    def traverse(v: Any): Cancelable = {
      v match {
        case null | None | false =>
          htmlNode.removeAttribute(a.name)
          Cancelable.empty
        case Some(x) =>
          traverse(x)
        case rx: RxOps[_] =>
          var c1 = Cancelable.empty
          val c2 = rx.run { value =>
            // Cancel the previous binding
            c1.cancel
            c1 = traverse(value)
          }
          Cancelable.merge(c1, c2)
        // TODO: Add RxElement rendering
        // case r: RxElement =>
        //
        case f: Function0[_] =>
          node.setEventListener(a.name, { (_: dom.Event) => f() })
        case f: Function1[dom.Node @unchecked, _] =>
          node.setEventListener(a.name, f)
        case _ =>
          val value = v match {
            case true => ""
            case _    => v.toString
          }
          a.name match {
            case "style" =>
              val prev = htmlNode.style.cssText
              if (prev.nonEmpty && a.append && value.nonEmpty) {
                htmlNode.style.cssText = s"${prev} ${value}"
              } else {
                htmlNode.style.cssText = value
              }
              Cancelable { () =>
                if (htmlNode != null && value.nonEmpty) {
                  val newAttributeValue = removeStyleValue(htmlNode.style.cssText, value)
                  htmlNode.style.cssText = newAttributeValue
                }
              }
            case _ =>
              def removeAttribute(): Unit = {
                a.ns match {
                  case Namespace.xhtml =>
                    htmlNode.removeAttribute(a.name)
                  case ns =>
                    htmlNode.removeAttributeNS(ns.uri, a.name)
                }
              }

              def setAttribute(newAttrValue: String): Unit = {
                a.ns match {
                  case Namespace.xhtml =>
                    htmlNode.setAttribute(a.name, newAttrValue)
                  case ns =>
                    // e.g., SVG attributes
                    htmlNode.setAttributeNS(ns.uri, a.name, newAttrValue)
                }
              }

              val newAttrValue = if (a.append && htmlNode.hasAttribute(a.name)) {
                s"${htmlNode.getAttribute(a.name)} ${value}"
              } else {
                value
              }
              setAttribute(newAttrValue)

              Cancelable { () =>
                if (htmlNode != null && htmlNode.hasAttribute(a.name)) {
                  val v = htmlNode.getAttribute(a.name)
                  if (v != null) {
                    // remove the appended value
                    val newAttrValue = removeStringFromAttributeValue(v, value)

                    // Replace the attribute value with the new one
                    removeAttribute()
                    if (newAttrValue.nonEmpty) {
                      setAttribute(newAttrValue)
                    }
                  }
                }
              }
          }
      }
    }

    traverse(a.v)
  }

  private implicit class RichDomNode(node: dom.Node) {

    /**
      * Evaluate an event listener return value
      */
    private def eval(v: Any): Cancelable = {
      v match {
        case rx: RxOps[_] =>
          rx.run { _ => }
        case Some(v) =>
          eval(v)
        case s: Iterable[_] =>
          val cancellables = for (x <- s) yield {
            eval(x)
          }
          Cancelable.merge(cancellables)
        case _ =>
          Cancelable.empty
      }
    }

    def setEventListener[A, U](key: String, listener: A => U): Cancelable = {
      val dyn = node.asInstanceOf[js.Dynamic]
      var c1  = Cancelable.empty
      val newListener = { (e: A) =>
        c1.cancel
        c1 = eval(listener(e))
      }
      dyn.updateDynamic(key)(newListener)
      Cancelable { () =>
        c1.cancel
        dyn.updateDynamic(key)(null)
      }
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
