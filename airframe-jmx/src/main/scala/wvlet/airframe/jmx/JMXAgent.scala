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
package wvlet.airframe.jmx

import java.lang.management.ManagementFactory

import javax.management.remote.{JMXConnector, JMXConnectorFactory, JMXServiceURL}
import javax.management.{MBeanInfo, ObjectName}
import wvlet.log.LogSupport
import wvlet.log.io.IOUtil.withResource

case class HostAndPort(host: String, port: Int)

case class JMXConfig(registryPort: Option[Int] = None, rmiPort: Option[Int] = None)

/**
  *
  */
object JMXAgent extends LogSupport {
  def start(registryPort: Int) = new JMXAgent(JMXConfig(registryPort = Some(registryPort)))

  lazy val defaultAgent: JMXAgent = new JMXAgent(new JMXConfig())
}

trait JMXMBeanServerService {
  protected lazy val mbeanServer = ManagementFactory.getPlatformMBeanServer
}

class JMXAgent(config: JMXConfig) extends JMXRegistry with JMXMBeanServerService with LogSupport {
  val serviceUrl: JMXServiceURL = {
    val url = JMXUtil.currentJMXRegistry match {
      case Some(jmxReg) =>
        info(s"JMX registry is already running at ${jmxReg}")
        if (config.registryPort.isDefined) {
          val expectedPort = config.registryPort.get
          if (expectedPort != jmxReg.port) {
            throw new IllegalStateException(
              s"JMX registry is already running using an unexpected port: ${jmxReg.port}. Expected port = ${expectedPort}"
            )
          }
        }
        s"service:jmx:rmi:///jndi/rmi://${jmxReg.host}:${jmxReg.port}/jmxrmi"
      case None =>
        JMXUtil.startAndGetAgentURL(config)
    }
    new JMXServiceURL(url)
  }

  def withConnector[U](f: JMXConnector => U): U = {
    withResource(JMXConnectorFactory.connect(serviceUrl)) { connector => f(connector) }
  }

  def getMBeanInfo(mbeanName: String): MBeanInfo = {
    withConnector { connector => connector.getMBeanServerConnection.getMBeanInfo(new ObjectName(mbeanName)) }
  }

  def getMBeanAttribute(mbeanName: String, attrName: String): Any = {
    withConnector { connector => connector.getMBeanServerConnection.getAttribute(new ObjectName(mbeanName), attrName) }
  }
}
