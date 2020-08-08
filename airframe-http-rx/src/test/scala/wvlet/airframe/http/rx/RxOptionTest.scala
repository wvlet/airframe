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
package wvlet.airframe.http.rx
import wvlet.airspec.AirSpec

/**
  */
class RxOptionTest extends AirSpec {
  test("eval RxOption") {
    val opt = Rx.option("world")
    val v   = opt.map(x => s"hello ${x}")
    v.run(x => x shouldBe "hello world")
  }

  test("support none") {
    val opt = Rx.none
    val v   = opt.map(x => s"hello ${x}")
    v.run(x => fail("should not reach here"))
  }

  test("filter true") {
    val opt = Rx.option("world")
    val v   = opt.filter(_.startsWith("world")).map(x => s"hello ${x}")
    v.run(x => x shouldBe "hello world")
  }

  test("filter false") {
    val opt = Rx.option("world")
    val v   = opt.filter(_.startsWith("xxx")).map(x => s"hello ${x}")
    v.run(x => fail("should not reach here"))
  }
}
