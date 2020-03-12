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
import wvlet.airframe.rx.html.all._
import wvlet.airspec.AirSpec

/**
  *
  */
object NestedElementTest extends AirSpec {

  class MyElement(name: String) extends RxElement {
    override def render: RxElement = div(
      cls -> "button",
      name
    )
  }

  test("compile nested elements") {
    val lst = (1 to 3)
    div(
      id -> "nested",
      // Seq[RxElement]
      lst.toSeq.map { i => new MyElement(s"button ${i}") },
      // Iterable[RxElement]
      lst.toIterable.map { i => new MyElement(s"button ${i}") }
    )
  }

  test("embed nested Rx[Seq[X]]") {
    val rx = Rx.variable(Seq(1, 2, 3))
    div(
      rx.map { i => p(i) }
    )
  }
}
