package wvlet.log

import java.lang.management.ManagementFactory
import javax.management.remote.{JMXConnectorFactory, JMXServiceURL}
import javax.management._

/**
  * Logger configuration API for JMX
  *
  */
@MXBean abstract trait LoggerJMXConfig {
  def getDefaultLogLevel: String
  def setDefaultLogLevel(logLevel: String): Unit
  def setLogLevel(name: String, logLevel: String): Unit
}

class LoggerJMXConfigImpl extends LoggerJMXConfig {

  def getDefaultLogLevel = Logger.getDefaultLogLevel.name

  def setDefaultLogLevel(logLevel: String) {
    val level = LogLevel(logLevel)
    Logger.setDefaultLogLevel(level)
  }

  def setLogLevel(loggerName: String, logLevel: String) {
    val logger = Logger.apply(loggerName)
    val level = LogLevel(logLevel)
    logger.setLevel(level.jlLevel)
    Logger.rootLogger.info(s"set the log level of $loggerName to $level")
  }
}

object LoggerJMXConfig {

  private val configMBeanName = new ObjectName("wvlet.log:type=LoggerJMXConfig")

  {
    val server = ManagementFactory.getPlatformMBeanServer
    try {
      if(!server.isRegistered(configMBeanName))
        server.registerMBean(new LoggerJMXConfigImpl, configMBeanName)
    }
    catch {
      case e: InstanceAlreadyExistsException => // OK
      case e: Exception => e.printStackTrace()
    }
  }


  def setLogLevelJMX(loggerName:String, logLevel:String) {
    val lc = JMX.newMBeanProxy(ManagementFactory.getPlatformMBeanServer, configMBeanName, classOf[LoggerJMXConfig], true)
    lc.setLogLevel(loggerName, logLevel)
  }

  def setLogLevelJMX(server:MBeanServerConnection, loggerName:String, logLevel:String) {
    val lc = JMX.newMBeanProxy(server, configMBeanName, classOf[LoggerJMXConfig], true)
    lc.setLogLevel(loggerName, logLevel)
    Logger.rootLogger.fine(s"Set the loglevel of $loggerName to $logLevel")
  }
  def setDefaultLogLevelJMX(server:MBeanServerConnection, logLevel:String) {
    val lc = JMX.newMBeanProxy(server, configMBeanName, classOf[LoggerJMXConfig], true)
    lc.setDefaultLogLevel(logLevel)
    Logger.rootLogger.fine(s"Set the default loglevel to $logLevel")
  }

  def getJMXServerAddress(pid:Int) : Option[String] = {
    Option(sun.management.ConnectorAddressLink.importFrom(pid))
  }

  /**
    *
    */
  private def getJMXServer(pid:Int) : Option[MBeanServerConnection] = {
    Logger.rootLogger.info(s"Searching for JMX server pid:$pid")
    val addr = getJMXServerAddress(pid)
    val server : Option[MBeanServerConnection] = addr.map{ a =>
      JMXConnectorFactory.connect(new JMXServiceURL(a))
    } map (_.getMBeanServerConnection)

    if(server.isEmpty) {
      Logger.rootLogger.warning(s"No JMX server (pid:$pid) is found")
    }
    else {
      Logger.rootLogger.info(s"Found a JMX server (pid:$pid)")
      Logger.rootLogger.fine(s"Server address: ${addr.get}")
    }
    server
  }

  def setLogLevel(pid:Int, loggerName:String, logLevel:String) {
    for(server <- getJMXServer(pid)) {
      setLogLevelJMX(server, loggerName, logLevel)
    }
  }

  def setDefaultLogLevel(pid:Int, logLevel:String) {
    for(server <- getJMXServer(pid)) {
      setDefaultLogLevelJMX(server, logLevel)
    }
  }

}

