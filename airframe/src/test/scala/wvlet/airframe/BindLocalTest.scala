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
package wvlet.airframe

import java.util.concurrent.atomic.AtomicInteger

import wvlet.airspec.AirSpec

/**
  *
  */
class BindLocalTest extends AirSpec {
  scalaJsSupport

  import BindLocalTest._

  class X(counter: AtomicInteger) extends AutoCloseable {
    override def close(): Unit = {
      counter.incrementAndGet()
    }
  }
  trait App {
    val counter = bind[AtomicInteger]
    val x       = bindLocal { new X(counter) }
  }

  def `create a new local instance with a provider`: Unit = {
    val counter = new AtomicInteger()
    val d = newSilentDesign
      .bind[AtomicInteger].toInstance(counter)

    d.build[App] { a =>
      counter.get() shouldBe 0
    }
    counter.get() shouldBe 1
  }

  class Y

  def `create different local instances`: Unit = {
    trait App2 {
      val y0     = bind[Y]
      val yLocal = bindLocal { new Y }
    }

    val d = newSilentDesign.bind[Y].toSingleton
    d.build[App2] { a =>
      a.y0 shouldNotBeTheSameInstanceAs a.yLocal
    }
  }

  trait D1Test {
    val x = bindLocal { d1: D1 =>
      X1(d1)
    }
  }

  def `support bindLocal(D1 => A)` : Unit = {
    val d = newSilentDesign
      .bind[D1].toInstance(D1(1))

    d.build[D1Test] { t =>
      t.x shouldBe X1(D1(1))
    }
  }
}

object BindLocalTest {
  case class D1(v: Int = 0)
  case class X1(d1: D1)
}
