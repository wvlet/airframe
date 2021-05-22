package wvlet.airframe.di

import wvlet.airframe.AirframeException.MISSING_DEPENDENCY
import wvlet.airframe.newSilentDesign
import wvlet.airframe.surface.Surface
import wvlet.airspec.AirSpec

object DI3Test extends AirSpec {

  trait NonAbstractTrait {}

  test("report error when building a trait with no binding") {
    val e = intercept[MISSING_DEPENDENCY] {
      newSilentDesign.build[NonAbstractTrait] { m => }
    }
    e.stack.contains(Surface.of[NonAbstractTrait]) shouldBe true
  }

}
