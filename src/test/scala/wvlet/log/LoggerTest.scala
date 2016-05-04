package wvlet.log

import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll, _}
import wvlet.log.LogFormatter.SourceCodeLogFormatter

trait Spec extends WordSpec with ShouldMatchers with BeforeAndAfter with BeforeAndAfterAll with LogSupport {
  logger.resetHandler(new ConsoleLogHandler(SourceCodeLogFormatter))
}


class MyAppClass extends LogSupport {
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
      info("logging test")
      new MyAppClass

    }
  }
}
