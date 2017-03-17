package wvlet.log

import java.io.PrintStream

/**
  *
  */
trait LogEnvBase {
  def defaultConsoleOutput : PrintStream

  /**
    *
    * @param cl
    * @return
    */
  def getLoggerName(cl:Class[_]) : String
}
