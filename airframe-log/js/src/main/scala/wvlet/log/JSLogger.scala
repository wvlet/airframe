package wvlet.log

import scala.scalajs.js.annotation.{JSExport, JSExportTopLevel}

/**
  */
@JSExportTopLevel("log")
object JSLogger {
  @JSExport
  def setDefaultLogLevel(level: String): Boolean = {
    Logger.setDefaultLogLevel(LogLevel(level))
    true
  }

  @JSExport
  def setLogLevel(loggerName: String, level: String): Boolean = {
    Logger(loggerName).setLogLevel(LogLevel(level))
    true
  }
}
