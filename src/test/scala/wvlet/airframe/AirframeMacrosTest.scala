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

import wvlet.log.{LogLevel, LogSupport, Logger}

trait NonAbstractTrait extends LogSupport {
  info("hello trait")
}

trait AbstractTrait extends LogSupport {
  def abstractMethod : Unit
}

trait ConcreteTrait extends AbstractTrait {
  override def abstractMethod: Unit = { info("hello abstract trait") }
}

trait App1 {
  val t = bind[NonAbstractTrait]
}

trait App2 {
  val t = bind[AbstractTrait]
}

class ConcreteClass {
  val t = bind[NonAbstractTrait]
}

case class D1(id:Int)
case class D2(id:Int)
case class D3(id:Int)
case class D4(id:Int)
case class D5(id:Int)
case class App(d1:D1=D1(0), d2:D2=D2(0), d3:D3=D3(0), d4:D4=D4(0), d5:D5=D5(0)) extends LogSupport {
  debug(s"Created ${toString()}")
}

trait ProviderExample {
  // Constructor binding
  val c = bind[App]

  // Provider binding
  val p0 = bind { App() }
  val p1 = bind { d1:D1 => App(d1) }
  val p2 = bind { (d1:D1, d2:D2) => App(d1, d2) }
  val p3 = bind { (d1:D1, d2:D2, d3:D3) => App(d1, d2, d3) }
  val p4 = bind { (d1:D1, d2:D2, d3:D3, d4:D4) => App(d1, d2, d3, d4) }
  val p5 = bind { (d1:D1, d2:D2, d3:D3, d4:D4, d5:D5) => App(d1, d2, d3, d4, d5) }
}

/**
  *
  */
class AirframeMacrosTest extends AirframeSpec {
  "AirframeMacro" should {
    "build trait at compile time" in {
      val session = newDesign.newSession
      session.build[NonAbstractTrait]
      session.build[App1]
    }

    "build abstract trait" in {
      val session = newDesign.bind[AbstractTrait].to[ConcreteTrait]
                    .newSession

      val t = session.build[AbstractTrait]
      val app = session.build[App2]
      t.abstractMethod
      app.t.abstractMethod
    }

    "inject Session to concrete class" in {
      newDesign.newSession.build[ConcreteClass]
    }

    "build object with provider" taggedAs("provider") in {
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

      val p = newDesign
              .bind[D1].toInstance(d1)
              .bind[D2].toInstance(d2)
              .bind[D3].toInstance(d3)
              .bind[D4].toInstance(d4)
              .bind[D5].toInstance(d5)
              .newSession.build[ProviderExample]

      p.c shouldBe App(d1, d2, d3, d4, d5)
      p.p0 shouldBe App(z1, z2, z3, z4, z5)
      p.p1 shouldBe App(d1, z2, z3, z4, z5)
      p.p2 shouldBe App(d1, d2, z3, z4, z5)
      p.p3 shouldBe App(d1, d2, d3, z4, z5)
      p.p4 shouldBe App(d1, d2, d3, d4, z5)
      p.p5 shouldBe App(d1, d2, d3, d4, d5)
    }
  }
}
