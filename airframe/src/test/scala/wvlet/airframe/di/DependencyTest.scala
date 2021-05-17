package wvlet.airframe.di

import wvlet.airframe.AirframeException.MISSING_DEPENDENCY
import wvlet.airframe.Design
import wvlet.airspec.AirSpec

/**
  */
object DependencyTest1 {
  class A(val b: B)
  class B(val c: C)
  case class C(d: D)

  trait D
  class DImpl extends D
}

class DependencyTest extends AirSpec {

  test("show missing dependencies") {
    val d = Design.newSilentDesign
    d.withSession { session =>
      val m = intercept[MISSING_DEPENDENCY] {
        val a = session.build[DependencyTest1.A]
      }
      val msg = m.getMessage
      msg.contains("D <- C") shouldBe true
    }
  }

  test("resolve concrete dependencies") {
    val d = Design.newSilentDesign
      .bind[DependencyTest1.D].to[DependencyTest1.DImpl] // abstract class to a concrete trait
    d.withSession { session =>
      val a = session.build[DependencyTest1.A]
    }
  }
}
