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
  test("eval") {
    val opt = Rx.option(Some("world"))
    val v   = opt.map(x => s"hello ${x}")
    v.run(x => x shouldBe "hello world")
  }

  test("none") {
    val opt = Rx.none
    val v   = opt.map(x => s"hello ${x}")
    v.run(x => fail("should not reach here"))
  }

  test("filter true") {
    val opt = Rx.option(Some("world"))
    val v   = opt.filter(_.startsWith("world")).map(x => s"hello ${x}")
    v.run(x => x shouldBe "hello world")
  }

  test("filter false") {
    val opt = Rx.option(Some("world"))
    val v   = opt.filter(_.startsWith("xxx")).map(x => s"hello ${x}")
    v.run(x => fail("should not reach here"))
  }

  test("add name") {
    val r = Rx.option(Some("hello")).withName("opt test")
    debug(r)
  }

  test("flatMap") {
    val opt = Rx.option(Some("hello"))
    val v   = opt.flatMap(x => Rx.const(s"hello ${x}"))
    v.run(x => x shouldBe "hello hello")
  }

  test("for-comprehension") {
    val a = for (x <- Rx.option(Some("hello"))) yield {
      x + " world"
    }
    a.run(_ shouldBe "hello world")
  }

  test("toOption") {
    val opt = Rx.const(Some("hello")).toOption
    val a   = opt.map(x => s"${x} option")

    a.run(_ shouldBe "hello option")
  }

  test("option variable") {
    val v = Rx.optionVariable(Some("hello"))
    val o = v.map { x => s"${x} world" }
    o.run(_ shouldBe "hello world")
  }

  test("eval option variable") {
    val v = Rx.optionVariable(Some("hello"))
    v.run(_ shouldBe "hello")
  }

  test("set option variable") {
    val v = Rx.optionVariable(Some("hello"))
    val o = v.map { x => s"${x} world" }

    v.set(Some("good morning"))
    o.run(_ shouldBe "good morning world")
  }

}
