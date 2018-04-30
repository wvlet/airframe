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
import java.net.ServerSocket
import java.rmi.server.RemoteObject
import javax.management.remote.{JMXConnector, JMXConnectorFactory, JMXConnectorServer, JMXServiceURL}
import javax.management.{MBeanInfo, ObjectName}

import sun.management.Agent
import sun.management.jmxremote.ConnectorBootstrap
import sun.rmi.server.UnicastRef
import wvlet.log.LogSupport

import scala.reflect.ClassTag
import scala.util.{Failure, Success, Try}

case class HostAndPort(host: String, port: Int)

case class JMXConfig(registryPort: Option[Int] = None, rmiPort: Option[Int] = None)

/**
  *
  */
object JMXAgent extends LogSupport {

  def withResource[Resource <: AutoCloseable, U](resource: Resource)(body: Resource => U): U = {
    try {
      body(resource)
    } finally {
      resource.close
    }
  }

  def unusedPort: Int = {
    withResource(new ServerSocket(0)) { socket =>
      socket.getLocalPort
    }
  }

  implicit class WithReflection[A <: AnyRef](cl: Class[A]) {
    def getStaticField[R](name: String)(implicit ev: ClassTag[R]): Option[R] = {
      cl.getDeclaredFields.find(_.getName == name).flatMap { field =>
        val isAccessible = field.isAccessible
        try {
          field.setAccessible(true)
          Option(field.get(null).asInstanceOf[R])
        } finally {
          field.setAccessible(isAccessible)
        }
      }
    }
  }

  private def currentJMXRegistry: Option[HostAndPort] = {

    // In Java 9, sun.management.xxx is unavailable

    // For Java 8
    val jmxServer = classOf[Agent].getStaticField[JMXConnectorServer]("jmxServer")
    val registry  = classOf[ConnectorBootstrap].getStaticField[RemoteObject]("registry")

    (jmxServer, registry) match {
      case (Some(jmx), Some(reg)) =>
        Some(HostAndPort(jmx.getAddress.getHost, reg.getRef.asInstanceOf[UnicastRef].getLiveRef.getPort))
      case other =>
        None
    }
  }

  def start(registryPort: Int) = new JMXAgent(JMXConfig(registryPort = Some(registryPort)))

}

trait JMXMBeanServerService {
  protected lazy val mbeanServer = ManagementFactory.getPlatformMBeanServer
}

class JMXAgent(config: JMXConfig) extends JMXRegistry with JMXMBeanServerService with LogSupport {

  import JMXAgent._

  val serviceUrl: JMXServiceURL = {
    val url = currentJMXRegistry match {
      case Some(jmxReg) =>
        info(s"JMX registry is already running at ${jmxReg}")
        if (config.registryPort.isDefined) {
          val expectedPort = config.registryPort.get
          if (expectedPort != jmxReg.port) {
            throw new IllegalStateException(s"JMX registry is already running using an unexpected port: ${jmxReg.port}. Expected port = ${expectedPort}")
          }
        }
        s"service:jmx:rmi:///jndi/rmi://${jmxReg.host}:${jmxReg.port}/jmxrmi"
      case None =>
        val registryPort = config.registryPort.getOrElse(unusedPort)
        val rmiPort      = config.rmiPort.getOrElse(unusedPort)
        System.setProperty("com.sun.management.jmxremote", "true")
        System.setProperty("com.sun.management.jmxremote.port", registryPort.toString)
        System.setProperty("com.sun.management.jmxremote.rmi.port", rmiPort.toString)
        System.setProperty("com.sun.management.jmxremote.authenticate", "false")
        System.setProperty("com.sun.management.jmxremote.ssl", "false")

        Try(Agent.startAgent()) match {
          case Success(x) =>
            info(s"Started JMX agent at localhost:${registryPort}")
            s"service:jmx:rmi:///jndi/rmi://localhost:${registryPort}/jmxrmi"
          case Failure(e) =>
            warn(e)
            throw e
        }
    }
    new JMXServiceURL(url)
  }

  def withConnetor[U](f: JMXConnector => U): U = {
    withResource(JMXConnectorFactory.connect(serviceUrl)) { connector =>
      f(connector)
    }
  }

  def getMBeanInfo(mbeanName: String): MBeanInfo = {
    withConnetor { connector =>
      connector.getMBeanServerConnection.getMBeanInfo(new ObjectName(mbeanName))
    }
  }

  def getMBeanAttribute(mbeanName: String, attrName: String): Any = {
    withConnetor { connector =>
      connector.getMBeanServerConnection.getAttribute(new ObjectName(mbeanName), attrName)
    }
  }

}
