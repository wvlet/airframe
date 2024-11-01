package wvlet.airframe.log
import wvlet.airframe.log.LogFormatter.SourceCodeLogFormatter

import java.io.PrintStream
import java.lang.management.ManagementFactory
import java.util.concurrent.atomic.AtomicBoolean
import javax.management.{InstanceAlreadyExistsException, MBeanServer, ObjectName}
import scala.util.control.NonFatal

/**
  */
private[log] object LogEnv extends LogEnvBase:

  private val initialized = new AtomicBoolean(false)
  override def initLogManager(): Unit =
    // Set a custom LogManager to show log messages even in shutdown hooks
    val managerKey = "java.util.logging.manager"
    sys.props.put(managerKey, "wvlet.log.AirframeLogManager")

    if initialized.compareAndSet(false, true) then
      // For unregistering log manager https://github.com/wvlet/airframe/issues/2914
      sys.addShutdownHook {
        sys.props.get(managerKey) match
          case Some(v) if v == "wvlet.log.AirframeLogManager" =>
            sys.props.remove(managerKey)
          case _ =>
      }

  override def isScalaJS: Boolean        = false
  override def defaultLogLevel: LogLevel = LogLevel.INFO

  override val defaultConsoleOutput: PrintStream =
    // Note: In normal circumstances, using System.err here is fine, but
    // System.err can be replaced with other implementation
    // (e.g., airlift.Logging, which is used in Trino https://github.com/airlift/airlift/blob/master/log-manager/src/main/java/io/airlift/log/Logging.java),
    // If that happens, we may need to create a stderr stream explicitly like this
    // new PrintStream(new FileOutputStream(FileDescriptor.err))

    // Use the standard System.err for sbtn native client
    System.err
  override def defaultHandler: java.util.logging.Handler =
    new ConsoleLogHandler(SourceCodeLogFormatter)

  /**
    * @param cl
    * @return
    */
  override def getLoggerName(cl: Class[?]): String =
    var name = cl.getName

    if name.endsWith("$") then
      // Remove trailing $ of Scala Object name
      name = name.substring(0, name.length - 1)

    // When class is an anonymous trait
    if name.contains("$anon$") then
      val interfaces = cl.getInterfaces
      if interfaces != null && interfaces.length > 0 then
        // Use the first interface name instead of the anonymous name
        name = interfaces(0).getName
    name
  override def scheduleLogLevelScan: Unit =
    LogLevelScanner.scheduleLogLevelScan
  override def stopScheduledLogLevelScan: Unit =
    LogLevelScanner.stopScheduledLogLevelScan
  override def scanLogLevels: Unit =
    LogLevelScanner.scanLogLevels
  override def scanLogLevels(loglevelFileCandidates: Seq[String]): Unit =
    LogLevelScanner.scanLogLevels(loglevelFileCandidates)

  private def onGraalVM: Boolean =
    // https://www.graalvm.org/sdk/javadoc/index.html?constant-values.html
    val graalVMFlag = Option(System.getProperty("org.graalvm.nativeimage.kind"))
    graalVMFlag.map(p => p == "executable" || p == "shared").getOrElse(false)

  private val mBeanName = new ObjectName("wvlet.log:type=Logger")

  // Register JMX entry upon start-up
  registerJMX

  private lazy val getMBeanServer: Option[MBeanServer] =
    // A workaround for an issue, that in some environment (e.g., JDK17 + IntelliJ),
    // NPE with `Cannot invoke "jdk.internal.platform.CgroupInfo.getMountPoint()" because "anyController" is null`
    // error can be thrown. https://github.com/wvlet/airframe/issues/2127
    try Some(ManagementFactory.getPlatformMBeanServer)
    catch
      case NonFatal(e) =>
        // Pre-registered wvlet.log.AirframeLogManager might not be found when reloading the project in IntelliJ, so skip this error.
        None

  override def registerJMX: Unit =
    if !onGraalVM then
      // Register the log level configuration interface to JMX
      getMBeanServer.foreach { mbeanServer =>
        if !mbeanServer.isRegistered(mBeanName) then
          try mbeanServer.registerMBean(LoggerJMX, mBeanName)
          catch
            case e: InstanceAlreadyExistsException =>
            // this exception can happen as JMX entries can be initialized by different class loaders while running sbt
      }

  override def unregisterJMX: Unit =
    if !onGraalVM then
      getMBeanServer.foreach { mbeanServer =>
        if mbeanServer.isRegistered(mBeanName) then mbeanServer.unregisterMBean(mBeanName)
      }
