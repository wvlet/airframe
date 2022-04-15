package wvlet.log
import java.io.{FileDescriptor, FileOutputStream, PrintStream}
import java.lang.management.ManagementFactory
import javax.management.{InstanceAlreadyExistsException, ObjectName, MBeanServer}
import wvlet.log.LogFormatter.SourceCodeLogFormatter

/**
  */
private[log] object LogEnv extends LogEnvBase {
  override def isScalaJS: Boolean        = false
  override def defaultLogLevel: LogLevel = LogLevel.INFO

  override lazy val defaultConsoleOutput: PrintStream = {
    // Note: In normal circumstances, using System.err here is fine, but
    // System.err can be replaced with other implementation
    // (e.g., airlift.Logging, which is used in Trino https://github.com/airlift/airlift/blob/master/log-manager/src/main/java/io/airlift/log/Logging.java),
    // so we create a stderr stream explicitly here.
    new PrintStream(new FileOutputStream(FileDescriptor.err))
  }
  override def defaultHandler: java.util.logging.Handler = {
    new ConsoleLogHandler(SourceCodeLogFormatter)
  }

  /**
    * @param cl
    * @return
    */
  override def getLoggerName(cl: Class[_]): String = {
    var name = cl.getName

    if (name.endsWith("$")) {
      // Remove trailing $ of Scala Object name
      name = name.substring(0, name.length - 1)
    }

    // When class is an anonymous trait
    if (name.contains("$anon$")) {
      import collection.JavaConverters._
      val interfaces = cl.getInterfaces
      if (interfaces != null && interfaces.length > 0) {
        // Use the first interface name instead of the anonymous name
        name = interfaces(0).getName
      }
    }
    name
  }
  override def scheduleLogLevelScan: Unit = {
    LogLevelScanner.scheduleLogLevelScan
  }
  override def stopScheduledLogLevelScan: Unit = {
    LogLevelScanner.stopScheduledLogLevelScan
  }
  override def scanLogLevels: Unit = {
    LogLevelScanner.scanLogLevels
  }
  override def scanLogLevels(loglevelFileCandidates: Seq[String]): Unit = {
    LogLevelScanner.scanLogLevels(loglevelFileCandidates)
  }

  private def onGraalVM: Boolean = {
    // https://www.graalvm.org/sdk/javadoc/index.html?constant-values.html
    val graalVMFlag = Option(System.getProperty("org.graalvm.nativeimage.kind"))
    graalVMFlag.map(p => p == "executable" || p == "shared").getOrElse(false)
  }

  private val mBeanName = new ObjectName("wvlet.log:type=Logger")

  // Register JMX entry upon start-up
  registerJMX

  private lazy val getMBeanServer: Option[MBeanServer] = {
    // A workaround for an issue, that in some environment (e.g., JDK17 + IntelliJ),
    // NPE with `Cannot invoke "jdk.internal.platform.CgroupInfo.getMountPoint()" because "anyController" is null`
    // error can be thrown. https://github.com/wvlet/airframe/issues/2127
    try {
      Some(ManagementFactory.getPlatformMBeanServer)
    } catch {
      case e: Throwable =>
        // Show an error once without using the logger itself
        e.printStackTrace()
        None
    }
  }

  override def registerJMX: Unit = {
    if (!onGraalVM) {
      // Register the log level configuration interface to JMX
      getMBeanServer.foreach { mbeanServer =>
        if (!mbeanServer.isRegistered(mBeanName)) {
          try {
            mbeanServer.registerMBean(LoggerJMX, mBeanName)
          } catch {
            case e: InstanceAlreadyExistsException =>
            // this exception can happen as JMX entries can be initialized by different class loaders while running sbt
          }
        }
      }
    }
  }

  override def unregisterJMX: Unit = {
    if (!onGraalVM) {
      getMBeanServer.foreach { mbeanServer =>
        if (mbeanServer.isRegistered(mBeanName)) {
          mbeanServer.unregisterMBean(mBeanName)
        }
      }
    }
  }
}
