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
package wvlet.airframe.rx.widget

import wvlet.airspec.AirSpec

class NodeJSTest extends AirSpec {
//  test("read resource files using node.js") {
//    val fs  = scalajs.js.Dynamic.global.require("fs")
//    val txt = fs.readFileSync("airframe-rx-widget/src/test/resources/hello.txt").toString()
//    info(txt)
//  }

  test("use scala-js-node-js-v12") {
    // Removing this nodejs access code resolves the error at jsEnvInput phase
    val txt = io.scalajs.nodejs.fs.Fs.readFileSync("airframe-rx-widget/src/test/resources/hello.txt").toString("utf8")
    info(txt)
    txt.contains("hello") shouldBe true
  }
}
