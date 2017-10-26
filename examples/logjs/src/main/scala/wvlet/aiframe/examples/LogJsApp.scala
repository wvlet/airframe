package wvlet.aiframe.examples;

import wvlet.log._
import scala.scalajs.js

object LogJsApp extends js.JSApp with LogSupport {

  Logger.setDefaultHandler(new JSConsoleLogHandler)
  Logger.setDefaultLogLevel(LogLevel.ALL)

  def main() {
    info("info")
    debug("debug")
    trace("trace")
    warn("warn")
    error("error")
  }
}