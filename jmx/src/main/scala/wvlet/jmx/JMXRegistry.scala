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
package wvlet.jmx

import javax.management._

import wvlet.log.LogSupport

import scala.util.{Failure, Try}
import scala.reflect.runtime.{universe => ru}

/**
  *
  */
trait JMXRegistry extends JMXMBeanServerService with LogSupport {

  private var registeredMBean = Set.empty[ObjectName]

  def register[A: ru.WeakTypeTag](obj: A) {
    val cl          = obj.getClass
    val packageName = cl.getPackage.getName
    val name        = s"${packageName}:name=${cl.getSimpleName}"
    register(name, obj)
  }

  def register[A: ru.WeakTypeTag](name: String, obj: A) {
    register(new ObjectName(name), obj)
  }

  def register[A: ru.WeakTypeTag](objectName: ObjectName, obj: A) {
    val mbean = JMXMBean.of(obj)
    mbeanServer.registerMBean(mbean, objectName)
    synchronized {
      registeredMBean += objectName
    }
    debug(s"Registered mbean: ${mbean}")
  }

  def unregister(name: String) {
    mbeanServer.unregisterMBean(new ObjectName(name))
  }

  def unregisterAll {
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
