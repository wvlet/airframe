package wvlet.log

import java.io.PrintStream

/**
  *
  */
object LogEnv extends LogEnvBase with LogSupport {
  override def isScalaJS: Boolean = true
  override def defaultLogLevel: LogLevel = LogLevel.ALL

  override def defaultConsoleOutput: PrintStream = Console.out

  override def getLoggerName(cl: Class[_]): String = {
    var name = cl.getName

    // In Scala.js we cannot use cl.getInterfaces to find the actual type
    val pos = name.indexOf("$")
    if(pos > 0) {
      // Remove trailing $xxx
      name = name.substring(0, pos)
    }
    name
  }
  override def scheduleLogLevelScan: Unit = {
    warn(s"scheduleLogLevelScan is not supported in Scala.js")
  }
  override def stopScheduledLogLevelScan: Unit = {
    warn(s"scheduleLogLevelScan is not supported in Scala.js")
  }
  /**
    * Scan the default log level file only once. To periodically scan, use scheduleLogLevelScan
    */
  override def scanLogLevels: Unit = {
    warn(s"scheduleLogLevelScan is not supported in Scala.js")
  }

  override def scanLogLevels(loglevelFileCandidates: Seq[String]): Unit = {
    warn(s"scheduleLogLevelScan is not supported in Scala.js")
  }
}
