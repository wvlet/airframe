package examples

import wvlet.airspec._
import wvlet.airframe._

class TestSyntaxSpec extends AirSpec {
  override protected def design =
    newDesign
      .bind[String].toInstance("hello")
      .bind[Int].toInstance(10)
      .bind[Boolean].toInstance(true)

  test("support test syntax") {
    info("hello AirSpec test()")
  }

  test("one arg method") { s: String =>
    info(s"received arg: ${s}")
    s shouldBe "hello"
  }

  test("two arg method") { (s: String, i: Int) =>
    val m = s"${s} ${i}"
    info(s"${m}")
    m shouldBe "hello 10"
  }

  test("three arg method") { (s: String, i: Int, b: Boolean) =>
    val m = s"${s} ${i} ${b}"
    info(s"${m}")
    m shouldBe "hello 10 true"
  }

  test("local design override", design = newDesign.bind[String].toInstance("world")) { s: String =>
    info(s"arg: ${s}")
    s shouldBe "world"
  }

}
