package examples

import wvlet.airspec.spi.AssertionFailure
import wvlet.airspec.AirSpec

class TestCurrentBehavior extends AirSpec {
  test("see current error message") {
    try {
      val plan: String = null
      plan shouldNotBe null
    } catch {
      case e: AssertionFailure =>
        println(s"Current error message: ${e.message}")
        println(s"Source code info: ${e.code}")
        throw e
    }
  }
}