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
import wvlet.airframe.rx.Rx
import wvlet.airframe.rx.html.all._
import wvlet.airspec._

class HtmlTest extends AirSpec {

  def render(node: RxElement): String = {
    val txt = DOMRenderer.render(node) match {
      case (elem: dom.Element, c) =>
        c.cancel
        elem.outerHTML
      case (other, c) =>
        c.cancel
        other.innerText
    }
    info(txt)
    txt
  }

  test("create div") {
    val d = div(cls("link"), a(src("hello")), "hello html!")
    render(d)
  }
  test("create div with attrs") {
    val d = div(cls -> "main", a(src -> "hello"))
    render(d)
  }

  test("create component") {
    def d(content: RxElement*): RxElement = {
      div(cls("main"), div(cls("container"), content))
    }

    val x = d(id("c1"), "hello RxComponent")
    render(x)
  }

  test("table") {
    val t = table(
      th(
        Seq("col1", "col2").map { x => td(cls("col"), x) }
      ),
      for (i <- 0 to 2) yield {
        tr(
          td(1),
          td(2)
        )
      }
    )

    render(t)
  }

  test("embed values") {
    val d = div(
      1,
      10L,
      1.toShort,
      'a',
      true,
      false,
      None,
      Option("hello"),
      1.234f,
      1.234,
      Rx.variable("rx_var")
    )

    render(d)
  }

  test("add onclick") {
    val d = button("hello", onclick { e: dom.MouseEvent => println("clicked") })
    render(d)
  }

  test("add onclick without arg") {
    val d = button("hello", onclick { () => println("clicked") })
    render(d)
  }

}
