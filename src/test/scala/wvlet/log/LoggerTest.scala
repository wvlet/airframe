package wvlet.log

import wvlet.test.WvletSpec

class Test extends Logger {

  error("error message")
  warn("warn message")
  info("info message")
  debug("debug message")
  trace("trace message")

  info(null)
  info(Seq(1, 2, 3, 4))

}

/**
  *
  */
class LoggerTest extends WvletSpec {

  "logger" should {
    "display log messages" in {
      new Test
    }
  }
}
