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
import java.rmi.server.RemoteObject
import java.util.Properties

import javax.management.remote.JMXConnectorServer
import sun.rmi.server.UnicastRef
import wvlet.log.LogSupport
import wvlet.log.io.IOUtil

import scala.util.{Failure, Success, Try}

/**
  */
object JMXUtil extends LogSupport {
  implicit class WithReflection(className: String) {
    def invokeStaticMethod(methodName: String): Unit = {
      Try(Class.forName(className).getDeclaredMethod(methodName))
        .map(m => m.invoke(null))
        .recoverWith { case e: Throwable => throw e }
    }

    def getStaticField[R](name: String): Option[R] = {
      Class.forName(className).getDeclaredFields.find(_.getName == name).flatMap { field =>
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

  def isAtLeastJava9 = {
    // Scala 2.11.12 runs with JDK11
    scala.util.Properties.isJavaAtLeast("9")
  }

  private def startAgent(config: JMXConfig): HostAndPort = {
    val registryPort = config.registryPort.getOrElse(IOUtil.unusedPort)
    val rmiPort      = config.rmiPort.getOrElse(IOUtil.unusedPort)
    val p            = new Properties
    p.setProperty("com.sun.management.jmxremote", "true")
    p.setProperty("com.sun.management.jmxremote.port", registryPort.toString)
    p.setProperty("com.sun.management.jmxremote.rmi.port", rmiPort.toString)
    p.setProperty("com.sun.management.jmxremote.authenticate", "false")
    p.setProperty("com.sun.management.jmxremote.ssl", "false")

    import scala.jdk.CollectionConverters._
    if (isAtLeastJava9) {
      // TODO Java9 support
//      Try {
//        val processId = getProcessId.toString
//        import com.sun.tools.attach.VirtualMachine
//        VirtualMachine.list().asScala.find(x => x.id == processId).map { vmDesc =>
//          // This will not work because JVM doesn't allow attaching to self using the same VM.
//          // We need to create a new JVM process to run this code
//          val virtualMachine = vmDesc.provider().attachVirtualMachine(processId)
//          try {
//            virtualMachine.startLocalManagementAgent()
//            virtualMachine.startManagementAgent(p)
//          } finally {
//            virtualMachine.detach()
//          }
//        }
//      }
    } else {
      // Java8
      for ((k, v) <- p.asScala) {
        System.setProperty(k, v)
      }
      "sun.management.Agent".invokeStaticMethod("startAgent")
    }
    HostAndPort("localhost", registryPort)
  }

  private[jmx] def startAndGetAgentURL(config: JMXConfig): String = {
    Try(startAgent(config)) match {
      case Success(x) =>
        debug(s"Started JMX agent at localhost:${x.port}")
        s"service:jmx:rmi:///jndi/rmi://localhost:${x.port}/jmxrmi"
      case Failure(e) =>
        warn(e)
        throw e
    }
  }

  private[jmx] def currentJMXRegistry: Option[HostAndPort] = {
    if (isAtLeastJava9) {
      // In Java 9, sun.management.xxx is unavailable
      None
    } else {
      // For Java 8
      val jmxServer = "sun.management.Agent".getStaticField[JMXConnectorServer]("jmxServer")
      val registry  = "sun.management.jmxremote.ConnectorBootstrap".getStaticField[RemoteObject]("registry")

      (jmxServer, registry) match {
        case (Some(jmx), Some(reg)) =>
          Some(HostAndPort(jmx.getAddress.getHost, reg.getRef.asInstanceOf[UnicastRef].getLiveRef.getPort))
        case other =>
          None
      }
    }
  }

  private def getProcessId: Long = {
    val name = ManagementFactory.getRuntimeMXBean().getName()
    name.substring(0, name.indexOf('@')).toLong
  }
}
