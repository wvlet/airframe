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
package wvlet.airframe.rx.ml

import org.scalajs.dom
import wvlet.log.LogSupport

import scala.annotation.implicitNotFound
import scala.language.{higherKinds, implicitConversions}

/**
  *
  */
object html {

  class Namespace(val uri: String) extends AnyVal
  object Namespace {
    val xhtml: Namespace = new Namespace("http://www.w3.org/1999/xhtml")
    val svg: Namespace   = new Namespace("http://www.w3.org/2000/svg")
  }

  trait HtmlNode

  trait ElementModifier {
    def applyTo(elem: dom.Node): dom.Node
    def when(cond: => Boolean): ElementModifier = {
      if (cond) this else ElementModifier.empty
    }
    def unless(cond: => Boolean): ElementModifier = {
      if (cond) ElementModifier.empty else this
    }
  }

  object ElementModifier {
    object empty extends ElementModifier {
      def applyTo(elem: dom.Node) = elem
    }

  }

  class HtmlElement(name: String, modifiers: List[Seq[ElementModifier]] = List.empty) extends HtmlNode {
    def apply(xs: ElementModifier*): HtmlElement = {
      if (xs.isEmpty) {
        this
      } else {
        new HtmlElement(name = name, modifiers = xs :: modifiers)
      }
    }

    def toDOM: dom.Node = {
      var elem: dom.Node = dom.document.createElement(name)
      for (g <- modifiers.reverse; m <- g) {
        elem = m.applyTo(elem)
      }
      elem
    }
  }

  private def elementFilter(f: dom.Node => dom.Node): ElementModifier = new ElementModifier {
    def applyTo(elem: dom.Node): dom.Node = f(elem)
  }

  class HtmlAttribute(name: String) extends LogSupport {
    def apply[V](v: V): ElementModifier = elementFilter { x =>
      x match {
        case e: dom.raw.HTMLElement =>
          name match {
            case "style" =>
              val prev = e.style.cssText
              if (prev.isEmpty) {
                e.style.cssText = s"${prev} ${v}"
              }
            case _ =>
              warn(s"here: ${e}")
              // TODO check v type
              e.setAttribute(name, v.toString)
          }
        case _ =>
          warn(x)
      }
      x
    }
  }

  def tag(name: String): HtmlElement           = new HtmlElement(name)
  def attr(name: String): HtmlAttribute        = new HtmlAttribute(name)
  def attributeOf(name: String): HtmlAttribute = new HtmlAttribute(name)

  def div: HtmlElement  = tag("div")
  def img: HtmlElement  = tag("img")
  def th: HtmlElement   = tag("th")
  def td: HtmlElement   = tag("td")
  def tr: HtmlElement   = tag("tr")
  def a: HtmlElement    = tag("a")
  def p: HtmlElement    = tag("p")
  def code: HtmlElement = tag("code")
  def pre: HtmlElement  = tag("pre")
  def svg: HtmlElement  = tag("svg")

  def _src: HtmlAttribute   = attributeOf("src")
  def _href: HtmlAttribute  = attributeOf("href")
  def _class: HtmlAttribute = attributeOf("class")
  def _style: HtmlAttribute = attributeOf("style")
  def _id: HtmlAttribute    = attributeOf("id")
  //def onClick[U](handler: => U) =

  @implicitNotFound(msg = "unsupported type")
  trait Embeddable[X]
  object Embeddable {
    type EE[A] = Embeddable[A]
    @inline implicit def embedNil: EE[Nil.type]                    = null
    @inline implicit def embedString: EE[String]                   = null
    @inline implicit def embedElem: EE[HtmlElement]                = null
    @inline implicit def embedMod: EE[ElementModifier]             = null
    @inline implicit def embedSeq[C[x] <: Seq[x], T: EE]: EE[C[T]] = null
  }

  class Atom(v: Any) extends ElementModifier {
    def applyTo(elem: dom.Node): dom.Node = {
      // TODO
      v match {
        case s: String =>
          val textNode = dom.document.createTextNode(s)
          elem.appendChild(textNode)
          elem
        case other =>
          throw new IllegalArgumentException(s"unsupported: ${other}")
      }
    }
  }

  class ChildElementAdder(child: HtmlElement) extends ElementModifier {
    def applyTo(elem: dom.Node): dom.Node = {
      // TODO
      val childDOM = child.toDOM
      elem.appendChild(childDOM)
      elem
    }
  }

  implicit def convertToHtmlElement[A: Embeddable](v: A): ElementModifier = {
    v match {
      case e: HtmlElement =>
        new ChildElementAdder(e)
      case other =>
        new Atom(v)
    }
  }

}
