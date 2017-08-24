package wvlet.log

/**
  *
  */
class JSConsoleLogHandlerTest extends Spec {

  "JSConsoleLogHandler" should {

    Logger.setDefaultHandler(JSConsoleLogHandler())

    error("error message")
    warn("warn message")
    info("info message")
    debug("debug message")
    trace("trace message")
  }
}
