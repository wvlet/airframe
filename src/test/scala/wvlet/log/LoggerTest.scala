package wvlet.log

import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll, _}
import wvlet.log.LogFormatter._

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
    """This is a multi-line
      |log message!""".stripMargin)
  info(Seq(1, 2, 3, 4))
}

/**
  *
  */
class LoggerTest extends Spec {

  override def beforeAll {
    Logger.setDefaultLogLevel(LogLevel.TRACE)
  }

  override def afterAll {
    Logger.setDefaultLogLevel(LogLevel.INFO)
  }

  "logger" should {

    "display log messages" in {
      info("logging test")
      new MyAppClass
    }

    "support simple log format" in {
      Logger.setDefaultFormatter(SimpleLogFormatter)
      new MyAppClass
    }

    "support app log format" in {
      Logger.setDefaultFormatter(AppLogFormatter)
      new MyAppClass
    }

    "support source code log format" in {
      Logger.setDefaultFormatter(SourceCodeLogFormatter)
      new MyAppClass
    }

    "support intellij format" in {
      Logger.setDefaultFormatter(IntelliJLogFormatter)
      new MyAppClass
    }

    "support tsv format" in {
      Logger.setDefaultFormatter(TSVLogFormatter)
      new MyAppClass
    }

    "can create local logger" in {
      Logger.setDefaultFormatter(SourceCodeLogFormatter)
      val l = Logger("org.sample")
      info(s"logger name: ${l.getName}")
      l.info("hello logger")
    }

  }
}
