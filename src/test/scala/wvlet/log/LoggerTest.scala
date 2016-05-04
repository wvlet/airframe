package wvlet.log

import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll, _}

trait Spec extends WordSpec with ShouldMatchers with BeforeAndAfter with BeforeAndAfterAll with Logger


class Test extends Logger {

  error("error message")
  warn("warn message")
  info("info message")
  debug("debug message")
  trace("trace message")

  info(null)
  info(
    """multi line
      |string log""".stripMargin)
  info(Seq(1, 2, 3, 4))

}

/**
  *
  */
class LoggerTest extends Spec {

  "logger" should {
    "display log messages" in {
      new Test
    }
  }
}
