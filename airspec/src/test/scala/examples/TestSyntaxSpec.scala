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
    debug("hello AirSpec test()")
  }

  test("test pending") {
    pending
  }

  test("one arg method") { s: String =>
    debug(s"received arg: ${s}")
    s shouldBe "hello"
  }

  test("two arg method") { (s: String, i: Int) =>
    val m = s"${s} ${i}"
    debug(s"${m}")
    m shouldBe "hello 10"
  }

  test("three arg method") { (s: String, i: Int, b: Boolean) =>
    val m = s"${s} ${i} ${b}"
    debug(s"${m}")
    m shouldBe "hello 10 true"
  }

  test("4 arg method") { (s: String, i: Int, b: Boolean, ss: Session) =>
    val m = s"${s} ${i} ${b}"
    debug(s"${m}")
    m shouldBe "hello 10 true"
  }

  test("5 arg method") { (s: String, i: Int, b: Boolean, ss: Session, ctx: spi.AirSpecContext) =>
    val m = s"${s} ${i} ${b}"
    debug(s"${m}: ${ctx.testName}")
    ctx.testName shouldBe "5 arg method"
    m shouldBe "hello 10 true"
  }

  test("local design override", design = newDesign.bind[String].toInstance("world")) { s: String =>
    debug(s"arg: ${s}")
    s shouldBe "world"
  }

  test("nested tests") {
    test("test1") {
      debug("hello test1")
    }

    test("test2") {
      debug("hello test2")
      test("test A") {
        debug("further nesting")
      }
      test("test B") {
        debug("hello")
      }
    }
  }

}
