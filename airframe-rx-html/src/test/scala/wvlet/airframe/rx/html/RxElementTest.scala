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

import wvlet.airframe.rx.{Cancelable, Rx}
import wvlet.airframe.rx.html.all._
import wvlet.airspec.AirSpec

/**
  */
class RxElementTest extends AirSpec {
  test("create new RxElement") {
    val r = RxElement {
      div()
    }

    // Nesting
    val r2 = RxElement {
      r
    }

    // Add modifier
    r(cls -> "button")
    val r3 = r.add(style -> "color: white", id -> "id1")
    r3.render
  }

  test("embed Rx") {
    val e = RxElement { div() }
    val r = RxElement(Rx(e))

    val x = r.render
    x.render
  }

  test("traverse") {
    val r = RxElement {
      div()
    }
    r(cls -> "main").traverseModifiers { m => Cancelable.empty }

  }

  test("embedded") {
    val em = Embedded("text")
    intercept[Throwable] {
      // No implementation
      em.render
    }
  }

}
