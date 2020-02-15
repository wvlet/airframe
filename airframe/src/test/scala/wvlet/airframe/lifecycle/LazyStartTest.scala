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
package wvlet.airframe.lifecycle

import java.util.concurrent.atomic.AtomicBoolean

import wvlet.airframe.{bind, newSilentDesign}
import wvlet.airspec.AirSpec
import wvlet.log.LogSupport
import wvlet.airframe._

object LazyStartTest {
  type F1 = AtomicBoolean
  type F2 = AtomicBoolean

  trait MyApp extends LogSupport {
    val a = bind[F1]
      .onStart { x => x.set(true) }
      .onShutdown { x => x.set(false) }
  }

  trait MyApp2 extends LogSupport {
    val a = bind[F2]
      .onStart { x => x.set(true) }
      .onShutdown { x => x.set(false) }
  }
}

/**
  *
  */
class LazyStartTest extends AirSpec {
  scalaJsSupport

  import LazyStartTest._

  val f1 = new AtomicBoolean(false)
  val f2 = new AtomicBoolean(false)

  val d = newSilentDesign
    .bind[MyApp].toSingleton
    .bind[MyApp2].toSingleton
    .bind[F1].toInstance(f1)
    .bind[F2].toInstance(f2)

  def `support lazy start`: Unit = {
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

  def `support eager start`: Unit = {
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
