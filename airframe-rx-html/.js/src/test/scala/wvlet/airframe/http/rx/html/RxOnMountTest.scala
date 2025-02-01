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

import org.scalajs.dom
import org.scalajs.dom.HTMLElement
import wvlet.airframe.rx.{Cancelable, Rx}
import wvlet.airframe.rx.html.{DOMRenderer, RxElement}
import wvlet.airframe.rx.html.all.*
import wvlet.airspec.AirSpec

class RxOnMountTest extends AirSpec {

  private def render(v: Any): (HTMLElement, Cancelable) = {
    val (n, c) = v match {
      case rx: Rx[RxElement] @unchecked =>
        DOMRenderer.createNode(div(rx))
      case other: RxElement =>
        DOMRenderer.createNode(other)
    }
    (n.asInstanceOf[HTMLElement], c)
  }

  test("beforeRender/beforeUnmount") {
    var a                = 0
    var b                = 0
    var afterRenderCount = 0

    val v = Rx.variable(1)
    val r = new RxElement {
      override def beforeRender: Unit = {
        a += 1
      }

      override def onMount(n: dom.Node) = {
        afterRenderCount += 1
      }

      override def beforeUnmount: Unit = {
        b += 1
        afterRenderCount shouldBe 1
      }

      override def render: RxElement = span(v.map { x => s"hello ${x}" })
    }

    a shouldBe 0
    afterRenderCount shouldBe 0
    val (n, c) = render(r)
    n.outerHTML shouldBe "<span>hello 1</span>"
    a shouldBe 1
    b shouldBe 0

    // Updating inner element should not trigger on render
    v := 2
    a shouldBe 1
    afterRenderCount shouldBe 1
    b shouldBe 0
    n.outerHTML shouldBe "<span>hello 2</span>"

    // unmounting
    c.cancel
    a shouldBe 1
    b shouldBe 1
  }

  test("nested beforeRender/beforeUnmount") {
    var a               = false
    var b               = false
    var afterRenderFlag = false

    var a1               = false
    var b1               = false
    var afterRenderFlag1 = false

    var rendered = Rx.variable(false)

    val nested = new RxElement {
      override def beforeRender: Unit = {
        a1 = true
      }

      override def onMount(n: dom.Node) = {
        debug("afterMount: nested")
        afterRenderFlag = true
        rendered := true
        rendered.stop()
      }

      override def beforeUnmount: Unit = {
        debug(s"beforeUnmount: nested")
        b1 = true
      }

      override def render: RxElement = span("initial")
    }

    val r = new RxElement {
      override def beforeRender: Unit = {
        a = true
      }

      override def onMount(n: dom.Node) = {
        debug(s"afterMount: r")
        afterRenderFlag1 = true
      }

      override def beforeUnmount: Unit = {
        debug(s"beforeUnmount: r")
        b = true
      }

      override def render: RxElement = span(nested)
    }

    afterRenderFlag shouldBe false
    afterRenderFlag1 shouldBe false
    val (n, c) = render(r)

    rendered.lastOption.map { isFullyRendered =>
      isFullyRendered shouldBe true
      afterRenderFlag shouldBe true
      afterRenderFlag1 shouldBe true
      a shouldBe true
      b shouldBe false
      a1 shouldBe true
      b1 shouldBe false
      c.cancel
      b shouldBe true
      b1 shouldBe true
    }
  }
}
