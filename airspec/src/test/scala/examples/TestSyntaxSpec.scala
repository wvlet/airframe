package examples

import wvlet.airspec._
import wvlet.airframe._

class TestSyntaxSpec extends AirSpec {
  override protected def design = newDesign.bind[String].toInstance("hello")

  test("support test syntax") {
    info("hello AirSpec test")
  }

  test("one arg method") { s: String =>
    s shouldBe "hello"
  }
}
