package wvlet.log

import java.io.PrintStream

/**
  *
  */
object LogEnv extends LogEnvBase{
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

}
