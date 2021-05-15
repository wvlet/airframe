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
package wvlet.airframe.di

import wvlet.airspec.AirSpec
import wvlet.log.LogSupport

object ProviderExample extends Serializable {
  case class D1(id: Int)
  case class D2(id: Int)
  case class D3(id: Int)
  case class D4(id: Int)
  case class D5(id: Int)
  case class App(d1: D1 = D1(0), d2: D2 = D2(0), d3: D3 = D3(0), d4: D4 = D4(0), d5: D5 = D5(0)) extends LogSupport {
    debug(s"Created ${toString()}")
  }

  val d1 = D1(1)
  val d2 = D2(2)
  val d3 = D3(3)
  val d4 = D4(4)
  val d5 = D5(5)

  val z1 = D1(0)
  val z2 = D2(0)
  val z3 = D3(0)
  val z4 = D4(0)
  val z5 = D5(0)

  val providerDesign =
    Design.newDesign
      .bind[D1].toInstance(d1)
      .bind[D2].toInstance(d2)
      .bind[D3].toInstance(d3)
      .bind[D4].toInstance(d4)
      .bind[D5].toInstance(d5)

  def provider1(d1: D1): App                                 = App(d1)
  def provider2(d1: D1, d2: D2): App                         = App(d1, d2)
  def provider3(d1: D1, d2: D2, d3: D3): App                 = App(d1, d2, d3)
  def provider4(d1: D1, d2: D2, d3: D3, d4: D4): App         = App(d1, d2, d3, d4)
  def provider5(d1: D1, d2: D2, d3: D3, d4: D4, d5: D5): App = App(d1, d2, d3, d4, d5)
}

import wvlet.airframe.di.ProviderExample._

class SingletonProviderTest extends AirSpec {
  test("build singleton from provider bindings") {
    val s1 = providerDesign
      .bind[App].toProvider { d1: D1 => App(d1) }
      .newSession
    val p1 = s1.build[App]
    p1 shouldBe App(d1, z2, z3, z4, z5)
    p1 shouldBeTheSameInstanceAs s1.build[App]

    val s2 = providerDesign
      .bind[App].toProvider { (d1: D1, d2: D2) => App(d1, d2) }
      .newSession
    val p2 = s2.build[App]
    p2 shouldBe App(d1, d2, z3, z4, z5)
    p2 shouldBeTheSameInstanceAs s2.build[App]

    val s3 = providerDesign
      .bind[App].toProvider { (d1: D1, d2: D2, d3: D3) => App(d1, d2, d3) }
      .newSession
    val p3 = s3.build[App]
    p3 shouldBe App(d1, d2, d3, z4, z5)
    p3 shouldBeTheSameInstanceAs s3.build[App]

    val s4 = providerDesign
      .bind[App].toProvider { (d1: D1, d2: D2, d3: D3, d4: D4) => App(d1, d2, d3, d4) }
      .newSession
    val p4 = s4.build[App]
    p4 shouldBe App(d1, d2, d3, d4, z5)
    p4 shouldBeTheSameInstanceAs s4.build[App]

    val s5 = providerDesign
      .bind[App].toProvider { (d1: D1, d2: D2, d3: D3, d4: D4, d5: D5) => App(d1, d2, d3, d4, d5) }
      .newSession
    val p5 = s5.build[App]
    p5 shouldBe App(d1, d2, d3, d4, d5)
    p5 shouldBeTheSameInstanceAs s5.build[App]
  }
}

class EagerSingletonProviderTest extends AirSpec {
  test("eagerly build singleton from provider") {
    var p1Initialized = false
    val s1 = providerDesign
      .bind[App].toEagerSingletonProvider { d1: D1 => p1Initialized = true; App(d1) }
      .newSession
    p1Initialized shouldBe true
    val p1 = s1.build[App]
    p1 shouldBe App(d1, z2, z3, z4, z5)
    p1 shouldBeTheSameInstanceAs s1.build[App]

    var p2Initialized = false
    val s2 = providerDesign
      .bind[App].toEagerSingletonProvider { (d1: D1, d2: D2) => p2Initialized = true; App(d1, d2) }
      .newSession
    p2Initialized shouldBe true
    val p2 = s2.build[App]
    p2 shouldBe App(d1, d2, z3, z4, z5)
    p2 shouldBeTheSameInstanceAs s2.build[App]

    var p3Initialized = false
    val s3 = providerDesign
      .bind[App].toEagerSingletonProvider { (d1: D1, d2: D2, d3: D3) => p3Initialized = true; App(d1, d2, d3) }
      .newSession
    p3Initialized shouldBe true
    val p3 = s3.build[App]
    p3 shouldBe App(d1, d2, d3, z4, z5)
    p3 shouldBeTheSameInstanceAs s3.build[App]

    var p4Initialized = false
    val s4 = providerDesign
      .bind[App].toEagerSingletonProvider { (d1: D1, d2: D2, d3: D3, d4: D4) =>
        p4Initialized = true; App(d1, d2, d3, d4)
      }
      .newSession
    p4Initialized shouldBe true
    val p4 = s4.build[App]
    p4 shouldBe App(d1, d2, d3, d4, z5)
    p4 shouldBeTheSameInstanceAs s4.build[App]

    var p5Initialized = false
    val s5 = providerDesign
      .bind[App].toEagerSingletonProvider { (d1: D1, d2: D2, d3: D3, d4: D4, d5: D5) =>
        p5Initialized = true; App(d1, d2, d3, d4, d5)
      }
      .newSession
    p5Initialized shouldBe true
    val p5 = s5.build[App]
    p5 shouldBe App(d1, d2, d3, d4, d5)
    p5 shouldBeTheSameInstanceAs s5.build[App]
  }
}
