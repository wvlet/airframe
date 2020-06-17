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
package wvlet.airframe.http.rx.widget

import wvlet.airspec.AirSpec

/**
  */
class NodeJSTest extends AirSpec {
  import scalajs.js.Dynamic.{global => g}

  test("read resource files using node.js") {
    pendingUntil("Resolve node.js access from Scala.js")

    val fs  = g.require("fs")
    val txt = fs.readFileSync("src/test/resources/hello.txt").toString
    txt.contains("hello") shouldBe true
  }
}
