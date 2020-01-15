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
import org.scalajs.dom.{Node => DomNode}
import wvlet.airframe.rx.{Cancelable, Rx}
import wvlet.log.LogSupport

import scala.scalajs.js
import scala.xml._

/**
  * Building reactive DOM from xml.Node.
  *
  * It will also set event listener to the mounting DOM node (parent) if
  * XML attributes contain a Scala function
  *
  * This code is based on monadic-html:
  * https://github.com/OlivierBlanvillain/monadic-html/blob/master/monadic-html/src/main/scala/mhtml/mount.scala
  */
private[widget] object RxDOM extends LogSupport {

  def mount(elem: RxElement): Cancelable = {
    val node = dom.document.createElement("div")
    mountTo(node, elem)
  }

  def mountTo(parent: dom.Node, elem: RxElement): Cancelable = {
    mount(parent, Some(elem.getConfig), elem.render)
  }

  private def applyConfig(domNode: dom.Node, config: RxWidgetConfig): dom.Node = {
    // Enrich attributes using the config of RxElement
    val htmlNode = domNode.asInstanceOf[dom.html.Html]
    for ((attrName, value) <- config.attributes) {
      attrName match {
        case "style" =>
          val css = htmlNode.style.cssText
          htmlNode.style = s"${css} ${value.mkString(" ")}"
        case "class" =>
          value.foreach { cl =>
            htmlNode.classList.add(cl)
          }
        case attr if htmlNode.hasAttribute(attrName) =>
          val attrNode = htmlNode.getAttributeNode(attrName)
          htmlNode.setAttribute(attrName, s"${attrNode.value} ${value.mkString(" ")}")
        case _ =>
          htmlNode.setAttribute(attrName, value.mkString(" "))
      }
    }
    htmlNode
  }

  private def mount(
      parent: dom.Node,
      config: Option[RxWidgetConfig],
      currentNode: xml.Node,
      startPoint: Option[dom.Node] = None
  ): Cancelable = {
    currentNode match {
      case e @ Elem(_, label, metadata, scope, _, _*) =>
        val domNode = Option(e.namespace) match {
          case Some(ns) => dom.document.createElementNS(ns, label)
          case None     => dom.document.createElement(label)
        }
        val cancelMetadata = metadata.map { m =>
          addAttribute(domNode, scope, m)
        }
        config.foreach { x =>
          applyConfig(domNode, x)
        }

        val cancelChild = e.child.map { c =>
          mount(domNode, None, c)
        }
        parent.mountHere(domNode, startPoint)
        Cancelable { () =>
          cancelMetadata.foreach(_.cancel); cancelChild.foreach(_.cancel)
        }
      case EntityRef(entityName) =>
        val domNode = dom.document.createTextNode("").asInstanceOf[dom.Element]
        domNode.innerHTML = s"&$entityName;"
        parent.mountHere(domNode, startPoint)
        Cancelable.empty
      case Comment(text) =>
        parent.mountHere(dom.document.createComment(text), startPoint)
        Cancelable.empty
      case Group(nodes) =>
        val cancels = nodes.map(n => mount(parent, None, n, startPoint))
        Cancelable(() => cancels.foreach(_.cancel))
      case a: Atom[_] =>
        a.data match {
          case n: xml.Node =>
            mount(parent, None, n, startPoint)
          case node: dom.Node =>
            config
              .map(x => applyConfig(node, x))
            parent.mountHere(node, startPoint)
            Cancelable.empty
          case rx: Rx[_] =>
            val (start, end) = parent.createMountSection()
            var c1           = Cancelable.empty
            val c2 = rx.subscribe { v =>
              // Remove the previous node from the DOM
              parent.cleanMountSection(start, end)
              c1.cancel
              c1 = mount(parent, None, new Atom(v), Some(start))
            }
            Cancelable { () =>
              c1.cancel; c2.cancel
            }
          case elem: RxElement => {
            mount(parent, Some(elem.getConfig), elem.render, startPoint)
          }
          case Some(x) =>
            mount(parent, config, new Atom(x), startPoint)
          case None =>
            Cancelable.empty
          case LazyNode(node) =>
            mount(parent, config, node, startPoint)
          case LazyElement(elem) =>
            mount(parent, Some(elem.getConfig), elem.render, startPoint)
          case LazyElementSeq(elems) =>
            val lst = elems.map { elem =>
              mount(parent, Some(elem.getConfig), elem.render, startPoint)
            }
            Cancelable.merge(lst)
          case seq: Seq[_] =>
            mount(parent, None, Group(seq.map(new Atom(_))), startPoint)
          case primitive =>
            val content = primitive.toString
            if (!content.isEmpty)
              parent.mountHere(dom.document.createTextNode(content), startPoint)
            Cancelable.empty
        }
    }
  }

  private def addAttribute(
      parent: dom.Node,
      scope: NamespaceBinding,
      m: MetaData
  ): Cancelable = {
    def traverse(v: Any): Cancelable = {
      v match {
        case a: Atom[_] => traverse(a.data)
        case Some(x)    => traverse(x)
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
          parent.setEventListener(m.key, (_: dom.Event) => f())
        case f: Function1[DomNode @unchecked, Unit @unchecked] =>
          parent.setEventListener(m.key, f)
        case _ =>
          parent.setMetadata(scope, m, v)
          Cancelable.empty
      }
    }

    traverse(m.value)
  }

  private implicit class RichDomNode(node: dom.Node) {
    def setEventListener[A](key: String, listener: A => Unit): Cancelable = {
      val dyn = node.asInstanceOf[js.Dynamic]
      dyn.updateDynamic(key)(listener)
      Cancelable(() => dyn.updateDynamic(key)(null))
    }

    def setMetadata(scope: NamespaceBinding, m: MetaData, v: Any): Unit = {
      val htmlNode = node.asInstanceOf[dom.html.Html]
      def set(key: String, prefix: Option[String]): Unit = {
        v match {
          case null | None | false =>
            htmlNode.removeAttribute(key)
          case _ =>
            val value = v match {
              case true => ""
              case _    => v.toString
            }
            key match {
              case "style" =>
                htmlNode.style.cssText = value
              case _ =>
                prefix.map(p => scope.getURI(p)) match {
                  case Some(ns) => htmlNode.setAttributeNS(ns, key, value)
                  case None     => htmlNode.setAttribute(key, value)
                }
            }
        }
      }
      m match {
        case m: PrefixedAttribute[_] => set(s"${m.pre}:${m.key}", Some(m.pre))
        case _                       => set(m.key, None)
      }
    }

    // Creates and inserts two empty text nodes into the DOM, which delimitate
    // a mounting region between them point. Because the DOM API only exposes
    // `.insertBefore` things are reversed: at the position of the `}`
    // character in our binding example, we insert the start point, and at `{`
    // goes the end.
    def createMountSection(): (dom.Node, dom.Node) = {
      val start = dom.document.createTextNode("")
      val end   = dom.document.createTextNode("")
      node.appendChild(end)
      node.appendChild(start)
      (start, end)
    }

    // Elements are then "inserted before" the start point, such that
    // inserting List(a, b) looks as follows: `}` → `a}` → `ab}`. Note that a
    // reference to the start point is sufficient here. */
    def mountHere(child: dom.Node, start: Option[dom.Node]): Unit = {
      start.fold(node.appendChild(child))(point => node.insertBefore(child, point)); ()
    }

    // Cleaning stuff is equally simple, `cleanMountSection` takes a references
    // to start and end point, and (tail recursively) deletes nodes at the
    // left of the start point until it reaches end of the mounting section. */
    def cleanMountSection(start: dom.Node, end: dom.Node): Unit = {
      val next = start.previousSibling
      if (next != end) {
        node.removeChild(next)
        cleanMountSection(start, end)
      }
    }
  }

}
