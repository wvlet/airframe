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
import org.scalajs.dom.{MutationObserver, MutationObserverInit}
import wvlet.airframe.rx.{Cancelable, OnError, OnNext, Rx, RxOps, RxRunner}
import wvlet.log.LogSupport

import scala.scalajs.js
import scala.util.{Failure, Success, Try}

/**
  * Convert HtmlNodes into DOM elements for Scala.js.
  *
  * An important functionality of this renderer is composing Cancelable objects so that resources allocated along with
  * the rendered DOM objects will be properly discarded.
  *
  * Resources include event listeners, Rx subscribers, etc.
  */
object DOMRenderer extends LogSupport {

  /**
    * Render HtmlNode to a div element with the given id in the document. If the node doesn't exist, this method will
    * create a new div element with the nodeId.
    *
    * @param nodeId
    * @param htmlNode
    * @return
    *   A pair of the rendered DOM node and a Cancelable object to clean up the rendered elements
    */
  def renderToNode(nodeId: String, htmlNode: HtmlNode): (dom.Node, Cancelable) = {
    // Insert a DOM node if it doesn't exists
    val node = dom.document.getElementById(nodeId) match {
      case null =>
        val elem = dom.document.createElement("div")
        elem.setAttribute("id", nodeId)
        dom.document.body.appendChild(elem)
      case other => other
    }
    (node, renderTo(node, htmlNode))
  }

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

  /**
    * Create a new DOM node from the given RxElement
    * @param e
    * @return
    *   A pair of the rendered DOM node and a Cancelable object to clean up the rendered elements
    */
  def createNode(e: RxElement): (dom.Node, Cancelable) = {

    def render(rx: RxElement): (dom.Node, Cancelable) = {
      Try(rx.render) match {
        case Success(r) =>
          traverse(r)
        case Failure(e) =>
          warn(s"Failed to render ${rx}: ${e.getMessage}", e)
          // Embed an empty node
          (dom.document.createElement("span"), Cancelable.empty)
      }
    }

    def traverse(v: Any): (dom.Node, Cancelable) = {
      v match {
        case h: HtmlElement =>
          val node: dom.Node = createNode(h)
          val cancelable     = h.traverseModifiers(m => renderTo(node, m))
          (node, cancelable)
        case l: LazyRxElement[_] => render(l)
        case Embedded(v) =>
          traverse(v)
        case r: RxElement =>
          r.beforeRender
          val (n, c) = render(r)
          r.onMount(n)
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
    val context = new RenderingContext()
    val c       = renderToInternal(context, node, htmlNode, modifier)
    context.onFinish()
    c
  }

  /**
    * A class for collecting onRender hooks so that we can call onRender hooks after DOM nodes are mounted on the
    * document.
    */
  private class RenderingContext() {
    private var onRenderHooks = List.empty[() => Unit]

    def onFinish(): Unit = {
      onRenderHooks.reverse.foreach { f => f() }
    }

    def addOnRenderHook(f: () => Unit): RenderingContext = {
      onRenderHooks = f :: onRenderHooks
      this
    }
  }

  private def renderToInternal(
      context: RenderingContext,
      node: dom.Node,
      htmlNode: HtmlNode,
      modifier: dom.Node => dom.Node = identity
  ): Cancelable = {
    def traverse(v: Any, anchor: Option[dom.Node], localContext: RenderingContext): Cancelable = {
      v match {
        case HtmlNode.empty =>
          Cancelable.empty
        case e: HtmlElement =>
          val elem = createNode(e)
          val c    = e.traverseModifiers(m => renderToInternal(localContext, elem, m))
          node.mountHere(elem, anchor)
          c
        case rx: RxOps[_] =>
          val (start, end) = node.createMountSection()
          var c1           = Cancelable.empty
          val c2 = RxRunner.runContinuously(rx) { ev =>
            // Remove the previous binding from the DOM
            node.clearMountSection(start, end)
            // Cancel the previous binding
            c1.cancel
            ev match {
              case OnNext(value) =>
                // When updating a local DOM node, creata a localized context for collecting onRender hooks
                val ctx = new RenderingContext()
                c1 = traverse(value, Some(start), ctx)
                ctx.onFinish()
              case OnError(e) =>
                warn(s"An unhandled error occurred while rendering ${rx}: ${e.getMessage}", e)
                c1 = Cancelable.empty
              case other =>
                c1 = Cancelable.empty
            }
          }
          Cancelable.merge(c1, c2)
        case a: HtmlAttribute =>
          addAttribute(node, a)
        case n: dom.Node =>
          node.mountHere(n, anchor)
          Cancelable.empty
        case e: Embedded =>
          traverse(e.v, anchor, localContext)
        case rx: RxElement =>
          rx.beforeRender
          Try(rx.render) match {
            case Success(r) =>
              val c1   = renderToInternal(localContext, node, r)
              val elem = node.lastChild
              val c2   = rx.traverseModifiers(m => renderToInternal(localContext, elem, m))
              if ((rx.onMount _) ne RxElement.NoOp) {
                val observer: MutationObserver = new MutationObserver({ (mut, obs) =>
                  mut.foreach { m =>
                    m.addedNodes.find(_ eq elem).foreach { n =>
                      // Check if element has an ID and ensure it's available before calling onMount
                      val hasId = elem match {
                        case htmlElement: dom.HTMLElement => Option(htmlElement.id).filter(_.nonEmpty).isDefined
                        case _                            => false
                      }

                      if (hasId) {
                        // For elements with ID, verify they're available via getElementById before calling onMount
                        val elementId = elem.asInstanceOf[dom.HTMLElement].id
                        Option(dom.document.getElementById(elementId)) match {
                          case Some(_) => rx.onMount(elem)
                          case None    =>
                            // Element not yet available, try once more in next tick
                            dom.window.setTimeout(() => {
                              if (Option(dom.document.getElementById(elementId)).isDefined) {
                                rx.onMount(elem)
                              }
                            }, 0)
                        }
                      } else {
                        // For elements without ID, call onMount immediately
                        rx.onMount(elem)
                      }
                    }
                  }
                  obs.disconnect()
                })
                observer.observe(
                  node,
                  new MutationObserverInit {
                    attributes = node.nodeType == dom.Node.ATTRIBUTE_NODE
                    childList = node.nodeType != dom.Node.ATTRIBUTE_NODE
                  }
                )
              }
              node.mountHere(elem, anchor)
              Cancelable.merge(Cancelable(() => rx.beforeUnmount), Cancelable.merge(c1, c2))
            case Failure(e) =>
              warn(s"Failed to render ${rx}: ${e.getMessage}", e)
              Cancelable(() => rx.beforeUnmount)
          }
        case s: String =>
          val textNode = newTextNode(s)
          node.mountHere(textNode, anchor)
          Cancelable.empty
        case r: RawHtml =>
          val domNode = dom.document.createElement("span")
          domNode.innerHTML = r.html
          // Extract the inner node
          domNode.childNodes.headOption.foreach { n =>
            node.mountHere(n, anchor)
          }
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
          traverse(x, anchor, localContext)
        case s: Iterable[_] =>
          val cancelables = for (el <- s) yield {
            traverse(el, anchor, localContext)
          }
          Cancelable.merge(cancelables)
        case other =>
          throw new IllegalArgumentException(s"unsupported class ${other}")
      }
    }

    traverse(htmlNode, anchor = None, context)
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

      import scala.util.chaining.*
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
          val c2 = RxRunner.runContinuously(rx) { ev =>
            // Cancel the previous binding
            c1.cancel
            ev match {
              case OnNext(value) =>
                c1 = traverse(value)
              case OnError(e) =>
                warn(s"An unhandled error occurred while rendering ${rx}: ${e.getMessage}", e)
                c1 = Cancelable.empty
              case other =>
                c1 = Cancelable.empty
            }
          }
          Cancelable.merge(c1, c2)
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
                if (htmlNode != null && a.append && value.nonEmpty) {
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
                if (htmlNode != null && a.append && htmlNode.hasAttribute(a.name)) {
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
