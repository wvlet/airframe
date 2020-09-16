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
package wvlet.airframe.rx
import wvlet.airspec.AirSpec

/**
  */
class RxOptionTest extends AirSpec {
  test("eval") {
    val opt = Rx.option(Some("world"))
    val v   = opt.map(x => s"hello ${x}")
    v.run(x => x shouldBe Some("hello world"))
  }

  test("none") {
    val opt = Rx.none
    val v   = opt.map(x => s"hello ${x}")
    v.run(x => x shouldBe empty)
  }

  test("filter true") {
    val opt = Rx.option(Some("world"))
    val v   = opt.filter(_.startsWith("world")).map(x => s"hello ${x}")
    v.run(x => x shouldBe Some("hello world"))
  }

  test("filter false") {
    val opt = Rx.option(Some("world"))
    val v   = opt.filter(_.startsWith("xxx")).map(x => s"hello ${x}")
    v.run(_ shouldBe empty)
  }

  test("flatMap") {
    val opt = Rx.option(Some("hello"))
    val v   = opt.flatMap(x => Rx.const(s"hello ${x}"))
    v.run(_ shouldBe Some("hello hello"))
  }

  test("for-comprehension") {
    val a = for (x <- Rx.option(Some("hello"))) yield {
      x + " world"
    }
    a.run(_ shouldBe Some("hello world"))
  }

  test("toOption") {
    val opt = Rx.const(Some("hello")).toOption
    val a   = opt.map(x => s"${x} option")

    a.run(_ shouldBe Some("hello option"))
  }

  test("option variable") {
    val v = Rx.optionVariable(Some("hello"))
    val o = v.map { x =>
      s"${x} world"
    }
    o.run(_ shouldBe Some("hello world"))
  }

  test("eval option variable") {
    val v = Rx.optionVariable(Some("hello"))
    v.run(_ shouldBe Some("hello"))
  }

  test("set option variable") {
    val v = Rx.optionVariable(Some("hello"))
    val o = v.map { x =>
      s"${x} world"
    }

    v.set(Some("good morning"))
    o.run(_ shouldBe Some("good morning world"))
      // We need to cancel the run to unregister the subscription
      .cancel

    v.set(None)
    o.run(_ shouldBe empty)
  }

  test("convert RxVar to RxOptionVar") {
    val v = Rx.variable(Some("hello")).toOption
    val o = v.map { x =>
      s"${x} world"
    }
    o.run(_ shouldBe Some("hello world")).cancel

    v := None
    o.run(_ shouldBe empty).cancel

    v := Some("good morning")
    o.run(_ shouldBe Some("good morning world")).cancel
  }

  test("getOrElse") {
    val opt = Rx.option(Some("hello"))
    opt.getOrElse("world").run(_ shouldBe "hello")
  }

  test("getOrElse None") {
    val opt = Rx.none
    opt.getOrElse("world").run(_ shouldBe "world")
  }

  test("orElse") {
    val opt = Rx.option(Some("hello"))
    opt.orElse(Some("world")).run(_ shouldBe Some("hello"))
  }

  test("orElse None") {
    val opt = Rx.none
    opt.orElse(Some("world")).run(_ shouldBe Some("world"))
  }

  test("transform") {
    val opt = Rx.option(Some("hello"))
    opt
      .transform {
        case Some(x) => x
        case None    => "world"
      }.run(_ shouldBe "hello")
  }

  test("transform None") {
    val opt = Rx.none
    opt
      .transform {
        case Some(x) => x
        case None    => "world"
      }.run(_ shouldBe "world")
  }

  test("transformOption") {
    val opt = Rx.option(Some("hello"))
    opt
      .transformOption {
        case Some(x) => Some(x)
        case None    => Some("world")
      }
      .run(_ shouldBe Some("hello"))
  }

  test("transformOption None") {
    val opt = Rx.none
    opt
      .transformOption {
        case Some(x) => Some(x)
        case None    => Some("world")
      }
      .run(_ shouldBe Some("world"))
  }

}
