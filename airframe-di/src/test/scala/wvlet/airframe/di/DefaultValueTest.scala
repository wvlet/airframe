package wvlet.airframe.di

import wvlet.airframe.Design
import wvlet.airspec.AirSpec

/**
  */
object DefaultValueTest {
  // This type of default values often used in configuration classes
  case class A(a: Long = 10, b: Long = 100, c: Long = 1000)

  case class B(a: A)
}

/**
  */
class DefaultValueTest extends AirSpec {
  import DefaultValueTest._
  test("populate default values") {
    Design.newSilentDesign.build[B] { b =>
      b.a shouldBe A()
    }
  }
}
