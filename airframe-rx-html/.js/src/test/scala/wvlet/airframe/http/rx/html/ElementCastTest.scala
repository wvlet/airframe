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
import wvlet.airframe.rx.html.all._
import wvlet.airframe.rx.html.compat.MouseEvent
import wvlet.airframe.rx.html.{RxDOM, RxElement}
import wvlet.airspec.AirSpec

class ElementCastTest extends AirSpec {

  test("normal cast") {
    div(
      onclick -> { (e: MouseEvent) =>
        Option(e.target).collect { case el: HTMLElement =>
        }
      }
    )
  }

  test("cast with helper method") {
    import RxDOM._
    var handled = false
    val rx = new RxElement {
      override def render = div(
        id -> "test",
        onclick -> { (e: MouseEvent) =>
          handleEvent(e.target) { case el: HTMLElement =>
            handled = true
          }
        }
      )
    }
    val el = rx.renderNode("main")
    getHTMLElementById("test").foreach(_.click())
    handled shouldBe true
  }
}
