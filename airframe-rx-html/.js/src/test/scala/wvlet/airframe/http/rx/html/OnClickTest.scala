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

import wvlet.airframe.rx.Rx
import wvlet.airframe.rx.html.DOMRenderer
import wvlet.airspec.AirSpec
import wvlet.airframe.rx.html._
import wvlet.airframe.rx.html.all._
import org.scalajs.dom.HTMLElement
import org.scalajs.dom.MouseEvent

class OnClickTest extends AirSpec {

  test("Evaluate onclick -> Rx return value") {
    var clicked       = 0
    var rxEvalCounter = 0
    val (node, c) = DOMRenderer.createNode {
      div(
        onclick -> { (e: MouseEvent) =>
          clicked += 1
          Rx.single {
            rxEvalCounter += 1
          }
        }
      )
    }
    val elem = node.asInstanceOf[HTMLElement]
    elem.click()
    clicked shouldBe 1
    rxEvalCounter shouldBe 1

    elem.click()
    clicked shouldBe 2
    rxEvalCounter shouldBe 2

    // After canceling the subscription, the event handler should not be called
    c.cancel
    elem.click()
    clicked shouldBe 2
    rxEvalCounter shouldBe 2
  }
}
