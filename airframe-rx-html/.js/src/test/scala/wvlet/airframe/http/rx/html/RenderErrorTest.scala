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

import org.scalajs.dom.HTMLElement
import wvlet.airframe.rx.Rx
import wvlet.airframe.rx.html.all.*
import wvlet.airframe.rx.html.RxElement
import wvlet.airspec.AirSpec

object RenderErrorTest extends AirSpec {

  class MyElem extends RxElement {
    val x = Rx.variable(1)

    override def render = div(
      "hello",
      x.map { x => s" world: ${1 / x}" }
    )
  }

  test("report an error during rendering") {
    val el   = new MyElem()
    val node = el.renderTo("test1")
    node.node shouldMatch { case h: HTMLElement =>
      h.innerHTML shouldBe "<div>hello world: 1</div>"
    }
    el.x := 0
    node.node shouldMatch { case h: HTMLElement =>
      h.innerHTML shouldBe "<div>hello</div>"
    }
  }

  class RxElem extends RxElement {
    val y = Rx.variable(1)

    override def render: RxElement = y.map { v =>
      div(
        "hello world:",
        i(1 / v)
      )
    }
  }

  test("report an error during rendering nested Rx elements") {
    val el   = new RxElem()
    val node = el.renderTo("test2")
    node.node shouldMatch { case h: HTMLElement =>
      h.innerHTML shouldBe "<div>hello world:<i>1</i></div>"
    }
    el.y := 0
    node.node shouldMatch { case h: HTMLElement =>
      h.innerHTML shouldBe empty
    }
  }

}
