/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package wvlet.log

import java.lang.management.ManagementFactory
import javax.management.remote.{JMXConnectorFactory, JMXServiceURL}
import javax.management._

/**
  * Logger configuration API for JMX
  *
  * TODO: Move this to wvlet-log-jmx project, and use wvlet-jmx
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
    logger.setLogLevel(level)
    //info(s"set the log level of $loggerName to $level")
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
    //info(s"Set the loglevel of $loggerName to $logLevel")
  }
  def setDefaultLogLevelJMX(server:MBeanServerConnection, logLevel:String) {
    val lc = JMX.newMBeanProxy(server, configMBeanName, classOf[LoggerJMXConfig], true)
    lc.setDefaultLogLevel(logLevel)
    //info(s"Set the default loglevel to $logLevel")
  }

  def getJMXServerAddress(pid:Int) : Option[String] = {
    Option(sun.management.ConnectorAddressLink.importFrom(pid))
  }

  /**
    *
    */
  private def getJMXServer(pid:Int) : Option[MBeanServerConnection] = {
    //info(s"Searching for JMX server pid:$pid")
    val addr = getJMXServerAddress(pid)
    val server : Option[MBeanServerConnection] = addr.map{ a =>
      JMXConnectorFactory.connect(new JMXServiceURL(a))
    } map (_.getMBeanServerConnection)

    if(server.isEmpty) {
      //warn(s"No JMX server (pid:$pid) is found")
    }
    else {
      //info(s"Found a JMX server (pid:$pid)")
      //debug(s"Server address: ${addr.get}")
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

