package wvlet.log

import java.io.PrintStream

/**
  */
private[log] object LogEnv extends LogEnvBase {
  override def isScalaJS: Boolean        = true
  override def defaultLogLevel: LogLevel = LogLevel.INFO

  override def defaultConsoleOutput: PrintStream = Console.out
  override def defaultHandler                    = JSConsoleLogHandler()

  override def getLoggerName(cl: Class[_]): String = {
    var name = cl.getName

    // In Scala.js we cannot use cl.getInterfaces to find the actual type
    val pos = name.indexOf("$")
    if (pos > 0) {
      // Remove trailing $xxx
      name = name.substring(0, pos)
    }
    name
  }
  override def scheduleLogLevelScan: Unit = {
    // no-op
  }
  override def stopScheduledLogLevelScan: Unit = {
    // no-op
  }

  override def scanLogLevels: Unit = {
    // no-op
  }

  override def scanLogLevels(loglevelFileCandidates: Seq[String]): Unit = {
    // no-op
  }

  override def registerJMX: Unit = {
    // no-op
  }

  override def unregisterJMX: Unit = {
    // no-op
  }
}
