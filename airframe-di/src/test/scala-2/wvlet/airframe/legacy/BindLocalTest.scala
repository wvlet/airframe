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
package wvlet.airframe.legacy

import java.util.concurrent.atomic.AtomicInteger

import wvlet.airspec.AirSpec

import wvlet.airframe._

/**
  */
class BindLocalTest extends AirSpec {
  import BindLocalTest._

  class LocalX(counter: AtomicInteger) extends AutoCloseable {
    override def close(): Unit = {
      counter.incrementAndGet()
    }
  }
  trait App {
    val counter = bind[AtomicInteger]
    val x       = bindLocal { new LocalX(counter) }
  }

  test("create a new local instance with a provider") {
    val counter = new AtomicInteger()
    val d = newSilentDesign
      .bind[AtomicInteger].toInstance(counter)

    d.build[App] { a => counter.get() shouldBe 0 }
    counter.get() shouldBe 1
  }

  class Y

  test("create different local instances") {
    trait App2 {
      val y0     = bind[Y]
      val yLocal = bindLocal { new Y }
    }

    val d = newSilentDesign.bind[Y].toSingleton
    // test 2
    d.build[App2] { a => a.y0 shouldNotBeTheSameInstanceAs a.yLocal }
  }

  trait LocalProviderTest {
    val x1 = bindLocal { d1: D1 => X(d1 = d1) }
    val x2 = bindLocal { (d1: D1, d2: D2) => X(d1 = d1, d2 = d2) }
    val x3 = bindLocal { (d1: D1, d2: D2, d3: D3) => X(d1 = d1, d2 = d2, d3 = d3) }
    val x4 = bindLocal { (d1: D1, d2: D2, d3: D3, d4: D4) => X(d1 = d1, d2 = d2, d3 = d3, d4 = d4) }
    val x5 = bindLocal { (d1: D1, d2: D2, d3: D3, d4: D4, d5: D5) => X(d1 = d1, d2 = d2, d3 = d3, d4 = d4, d5 = d5) }
  }

  test("support bindLocal with dependencies") {
    val d = newSilentDesign
      .bind[D1].toInstance(D1(1))
      .bind[D2].toInstance(D2(2))
      .bind[D3].toInstance(D3(3))
      .bind[D4].toInstance(D4(4))
      .bind[D5].toInstance(D5(5))

    d.build[LocalProviderTest] { t =>
      t.x1 shouldBe X(D1(1))
      t.x2 shouldBe X(D1(1), D2(2))
      t.x3 shouldBe X(D1(1), D2(2), D3(3))
      t.x4 shouldBe X(D1(1), D2(2), D3(3), D4(4))
      t.x5 shouldBe X(D1(1), D2(2), D3(3), D4(4), D5(5))
    }
  }
}

object BindLocalTest {
  case class D1(v: Int)
  case class D2(v: Int)
  case class D3(v: Int)
  case class D4(v: Int)
  case class D5(v: Int)

  case class X(d1: D1 = D1(0), d2: D2 = D2(0), d3: D3 = D3(0), d4: D4 = D4(0), d5: D5 = D5(0))
}
