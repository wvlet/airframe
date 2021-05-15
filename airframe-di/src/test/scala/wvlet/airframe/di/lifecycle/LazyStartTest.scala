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
package wvlet.airframe.di.lifecycle

import wvlet.airframe.di.Design

import java.util.concurrent.atomic.AtomicBoolean
import wvlet.airspec.AirSpec
import wvlet.log.LogSupport

object LazyStartTest {
  type F1 = AtomicBoolean
  type F2 = AtomicBoolean

  class MyApp(val a: F1)  extends LogSupport {}
  class MyApp2(val a: F2) extends LogSupport {}
}

/**
  */
class LazyStartTest extends AirSpec {
  scalaJsSupport

  import LazyStartTest._

  val f1 = new AtomicBoolean(false)
  val f2 = new AtomicBoolean(false)

  val d = Design.newSilentDesign
    .bind[MyApp].toSingleton
    .bind[MyApp2].toSingleton
    .bind[F1].toLazyInstance(f1)
    .onStart { x: F1 =>
      x.set(true)
    }
    .onShutdown { x: F1 => x.set(false) }
    .bind[F2].toLazyInstance(f2)
    .onStart { x: F2 =>
      x.set(true)
    }
    .onShutdown { x: F2 => x.set(false) }

  test("support lazy start") {
    (f1.get, f2.get) shouldBe (false, false)
    d.build[MyApp] { app => (f1.get, f2.get) shouldBe (true, false) }
    (f1.get, f2.get) shouldBe (false, false)

    d.withLazyMode.build[MyApp] { app => (f1.get, f2.get) shouldBe (true, false) }
    (f1.get, f2.get) shouldBe (false, false)

    // Override config
    d.withProductionMode.withLazyMode.build[MyApp] { app => (f1.get, f2.get) shouldBe (true, false) }
    (f1.get, f2.get) shouldBe (false, false)

    d.build[MyApp2] { app => (f1.get, f2.get) shouldBe (false, true) }
    (f1.get, f2.get) shouldBe (false, false)
  }

  test("support eager start") {
    (f1.get, f2.get) shouldBe (false, false)
    d.withProductionMode.build[MyApp] { app => (f1.get, f2.get) shouldBe (true, true) }
    (f1.get, f2.get) shouldBe (false, false)

    // Override config
    (f1.get, f2.get) shouldBe (false, false)
    d.withLazyMode.withProductionMode.build[MyApp] { app => (f1.get, f2.get) shouldBe (true, true) }
    (f1.get, f2.get) shouldBe (false, false)

    d.withProductionMode.build[MyApp2] { app => (f1.get, f2.get) shouldBe (true, true) }
    (f1.get, f2.get) shouldBe (false, false)
  }
}
