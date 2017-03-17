package wvlet.log
import java.io.PrintStream

/**
  *
  */
object LogEnv extends LogEnvBase {
  override def isScalaJS: Boolean = false

  override def defaultConsoleOutput: PrintStream = Console.err
  /**
    *
    * @param cl
    * @return
    */
  override def getLoggerName(cl: Class[_]): String = {
    var name = cl.getName

    if(name.endsWith("$")) {
      // Remove trailing $ of Scala Object name
      name = name.substring(0, name.length-1)
    }

    // When class is an anonymous trait
    if(name.contains("$anon$")) {
      import collection.JavaConverters._
      val interfaces = cl.getInterfaces
      if (interfaces != null && interfaces.length > 0) {
        // Use the first interface name instead of the anonymous name
        name = interfaces(0).getName
      }
    }
    name
  }
}
