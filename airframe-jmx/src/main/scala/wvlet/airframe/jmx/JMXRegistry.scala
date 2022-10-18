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

import javax.management._

import wvlet.log.LogSupport

import scala.util.{Failure, Try}

/**
  */
trait JMXRegistry extends JMXMBeanServerService with JMXRegistryCompat with LogSupport {
  private var registeredMBean = Set.empty[ObjectName]

  def register[A](mbean: JMXMBean, obj: A): Unit = {
    val cl          = obj.getClass
    val packageName = cl.getPackage.getName
    val name        = s"${packageName}:name=${JMXRegistry.getSimpleClassName(cl)}"
    register(mbean, name, obj)
  }

  def register[A](mbean: JMXMBean, name: String, obj: A): Unit = {
    register(mbean, new ObjectName(name), obj)
  }

  def register[A](mbean: JMXMBean, objectName: ObjectName, obj: A): Unit = {
    synchronized {
      if (mbeanServer.isRegistered(objectName)) {
        // Avoid the duplicate registration
        mbeanServer.unregisterMBean(objectName)
      }
      mbeanServer.registerMBean(mbean, objectName)
      registeredMBean += objectName
    }
    debug(s"Registered mbean: ${mbean}")
  }

  def unregister(name: String): Unit = {
    mbeanServer.unregisterMBean(new ObjectName(name))
  }

  def unregisterAll: Unit = {
    synchronized {
      for (name <- registeredMBean) {
        Try(mbeanServer.unregisterMBean(name)) match {
          case Failure(e) =>
            error(e.getMessage, e)
          case _ =>
        }
      }
    }
  }
}

object JMXRegistry {
  /*
   * Cleanup Scala-specific class names, which cannot be registered as JMX entries
   */
  private[jmx] def getSimpleClassName(cl: Class[_]): String = {
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

    // Extract the leaf logger name
    val dotPos = name.lastIndexOf(".")
    if (dotPos == -1) {
      name
    } else {
      name.substring(dotPos + 1)
    }
  }
}
