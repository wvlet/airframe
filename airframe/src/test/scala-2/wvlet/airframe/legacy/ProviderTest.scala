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

import wvlet.airspec.AirSpec
import wvlet.log.LogSupport

import wvlet.airframe._

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
    newDesign
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

import wvlet.airframe.legacy.ProviderExample._

trait ProviderExample {
  // Constructor binding (singleton)
  val c = bind[App]

  // Provider binding
  val p0 = bind { App() }
  val p1 = bind { d1: D1 => App(d1) }
  val p2 = bind { (d1: D1, d2: D2) => App(d1, d2) }
  val p3 = bind { (d1: D1, d2: D2, d3: D3) => App(d1, d2, d3) }
  val p4 = bind { (d1: D1, d2: D2, d3: D3, d4: D4) => App(d1, d2, d3, d4) }
  val p5 = bind { (d1: D1, d2: D2, d3: D3, d4: D4, d5: D5) => App(d1, d2, d3, d4, d5) }

  // Provider ref binding
  val pp1 = bind(provider1 _)
  val pp2 = bind(provider2 _)
  val pp3 = bind(provider3 _)
  val pp4 = bind(provider4 _)
  val pp5 = bind(provider5 _)
}

trait SingletonProviderExample {
  // Bind singleton
  val ps = bind[App]
}

trait PS0_S {
  // Bind singleton by default
  val p = bind(App())
}

trait PS0 {
  val p = bind(App())
}

trait PS1 {
  // Bind singleton
  val p = bind(provider1 _)
}

trait PS2 {
  val p = bind(provider2 _)
}

trait PS3 {
  val p = bind(provider3 _)
}

trait PS4 {
  val p = bind(provider4 _)
}

trait PS5 {
  val p = bind(provider5 _)
}

/**
  */
class ProviderTest extends AirSpec {
  scalaJsSupport

  test("build singletons with provider") {
    val p = providerDesign.newSession.build[ProviderExample]

    val firstSingleton = App(d1, d2, d3, d4, d5)
    p.c shouldBe firstSingleton

    // The other provider binding will have no effect
    p.p0 shouldBe firstSingleton
    p.p1 shouldBe firstSingleton
    p.p2 shouldBe firstSingleton
    p.p3 shouldBe firstSingleton
    p.p4 shouldBe firstSingleton
    p.p5 shouldBe firstSingleton

    p.pp1 shouldBe firstSingleton
    p.pp2 shouldBe firstSingleton
    p.pp3 shouldBe firstSingleton
    p.pp4 shouldBe firstSingleton
    p.pp5 shouldBe firstSingleton
  }

  test("build object from instance provider bindings") {
    val s1 = providerDesign
      .bind[App].toInstanceProvider { d1: D1 => App(d1) }
      .newSession
    val p1 = s1.build[App]
    p1 shouldBe App(d1, z2, z3, z4, z5)
    p1 shouldNotBeTheSameInstanceAs s1.build[App]

    val s2 = providerDesign
      .bind[App].toInstanceProvider { (d1: D1, d2: D2) => App(d1, d2) }
      .newSession
    val p2 = s2.build[App]
    p2 shouldBe App(d1, d2, z3, z4, z5)
    p2 shouldNotBeTheSameInstanceAs s2.build[App]

    val s3 = providerDesign
      .bind[App].toInstanceProvider { (d1: D1, d2: D2, d3: D3) => App(d1, d2, d3) }
      .newSession
    val p3 = s3.build[App]
    p3 shouldBe App(d1, d2, d3, z4, z5)
    p3 shouldNotBeTheSameInstanceAs s3.build[App]

    val s4 = providerDesign
      .bind[App].toInstanceProvider { (d1: D1, d2: D2, d3: D3, d4: D4) => App(d1, d2, d3, d4) }
      .newSession
    val p4 = s4.build[App]
    p4 shouldBe App(d1, d2, d3, d4, z5)
    p4 shouldNotBeTheSameInstanceAs s4.build[App]

    val s5 = providerDesign
      .bind[App].toInstanceProvider { (d1: D1, d2: D2, d3: D3, d4: D4, d5: D5) => App(d1, d2, d3, d4, d5) }
      .newSession
    val p5 = s5.build[App]
    p5 shouldBe App(d1, d2, d3, d4, d5)
    p5 shouldNotBeTheSameInstanceAs s5.build[App]
  }
}

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

class ProviderRefTest extends AirSpec {
  test("build object from instance provider ref") {
    val s1 = providerDesign
      .bind[App].toInstanceProvider(provider1 _)
      .newSession
    val p1 = s1.build[App]
    p1 shouldBe App(d1, z2, z3, z4, z5)
    p1 shouldNotBeTheSameInstanceAs s1.build[App]

    val s2 = providerDesign
      .bind[App].toInstanceProvider(provider2 _)
      .newSession
    val p2 = s2.build[App]
    p2 shouldBe App(d1, d2, z3, z4, z5)
    p2 shouldNotBeTheSameInstanceAs s2.build[App]

    val s3 = providerDesign
      .bind[App].toInstanceProvider(provider3 _)
      .newSession
    val p3 = s3.build[App]
    p3 shouldBe App(d1, d2, d3, z4, z5)
    p3 shouldNotBeTheSameInstanceAs s3.build[App]

    val s4 = providerDesign
      .bind[App].toInstanceProvider(provider4 _)
      .newSession
    val p4 = s4.build[App]
    p4 shouldBe App(d1, d2, d3, d4, z5)
    p4 shouldNotBeTheSameInstanceAs s4.build[App]

    val s5 = providerDesign
      .bind[App].toInstanceProvider(provider5 _)
      .newSession
    val p5 = s5.build[App]
    p5 shouldBe App(d1, d2, d3, d4, d5)
    p5 shouldNotBeTheSameInstanceAs s5.build[App]
  }

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

  test("bind singletons") {
    val session = providerDesign
      .bind[App].toProvider(provider3 _)
      .newSession

    val p1 = session.build[SingletonProviderExample]
    val p2 = session.build[SingletonProviderExample]
    p1.ps shouldBeTheSameInstanceAs (p2.ps)
    p1.ps shouldBe App(d1, d2, d3, z4, z5)
  }

  test("bind singleton with provider") {
    providerDesign.newSession.build[PS0].p shouldBe App(z1, z2, z3, z4, z5)
    providerDesign.newSession.build[PS0_S].p shouldBe App(z1, z2, z3, z4, z5)

    providerDesign.newSession.build[PS1].p shouldBe App(d1, z2, z3, z4, z5)

    providerDesign.newSession.build[PS2].p shouldBe App(d1, d2, z3, z4, z5)
    providerDesign.newSession.build[PS3].p shouldBe App(d1, d2, d3, z4, z5)
    providerDesign.newSession.build[PS4].p shouldBe App(d1, d2, d3, d4, z5)
    providerDesign.newSession.build[PS5].p shouldBe App(d1, d2, d3, d4, d5)
  }
}
