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
package wvlet.airframe.http.rx.html

import org.scalajs.dom.raw.HTMLElement
import wvlet.airframe.rx.{Cancelable, Rx, RxStream}
import wvlet.airframe.rx.html.{DOMRenderer, Embedded, RxElement}
import wvlet.airspec.AirSpec
import wvlet.airframe.rx.html.all._

/**
  */
object RxRenderingTest extends AirSpec {

  private def render(v: Any): (HTMLElement, Cancelable) = {
    val (n, c) = v match {
      case rx: RxStream[RxElement] @unchecked =>
        DOMRenderer.render(div(rx))
      case other: RxElement =>
        DOMRenderer.render(other)
    }
    (n.asInstanceOf[HTMLElement], c)
  }

  test("rendering RxOptionVar") {
    val v = Rx.optionVariable[String](None)

    val (n, c) = render {
      v.transform {
        case Some(x) => div(x)
        case None    => span("N/A")
      }
    }

    n.outerHTML shouldBe """<div><span>N/A</span></div>"""
    v := Some("Hello")
    n.outerHTML shouldBe """<div><div>Hello</div></div>"""
  }

  test("rendering RxOption with map") {
    val title = Rx.optionVariable(Some("home"))

    val (n, c) = render {
      div(
        "menu:",
        title.map { x =>
          b(x)
        }
      )
    }

    n.outerHTML shouldBe """<div>menu:<b>home</b></div>"""
    title := None
    n.outerHTML shouldBe """<div>menu:</div>"""
  }

}
