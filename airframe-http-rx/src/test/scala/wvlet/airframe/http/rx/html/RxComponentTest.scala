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

import wvlet.airspec.AirSpec
import wvlet.airframe.http.rx.html.all._

/**
  *
  */
class RxComponentTest extends AirSpec {

  test("create RxComponent") {
    val c = RxComponent { x => div(x) }

    val d = c("hello")
    d.render

    val x = c.render(div("content"))
    x.render

    val xx = c(div("content1"), div("content2"))
    xx.render

    val cc = RxComponent.ofTag("mytag")
    cc("hello")
  }

  test("embed multiple elements") {
    val d = RxElement(div("hello"))
    val c = RxComponent { x => div(x) }
    c(d, d, d)
  }

  test("custom tag/attr componetns") {
    val t = RxComponent.ofTag("mytag")
    t.render(div("content"))

  }
}
