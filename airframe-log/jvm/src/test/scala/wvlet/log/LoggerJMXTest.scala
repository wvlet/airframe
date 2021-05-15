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

import javax.management.{Attribute, ObjectName}

/**
  */
class LoggerJMXTest extends Spec {
  override protected def beforeAll = {
    // Make sure registering the JMX mBean that can be loadable from the current class loader
    LogEnv.unregisterJMX
    LogEnv.registerJMX
  }

  override protected def afterAll: Unit = {
    LogEnv.unregisterJMX
  }

  test("be registered") {
    // Initialize a logger
    val l = Logger.rootLogger

    val mbeanServer = ManagementFactory.getPlatformMBeanServer
    val name        = new ObjectName("wvlet.log:type=Logger")
    assert(mbeanServer.isRegistered(name))

    // Check the default log level
    assert(mbeanServer.getAttribute(name, "DefaultLogLevel").toString == l.getLogLevel.toString)

    val currentLogLevel = l.getLogLevel
    try {
      mbeanServer.setAttribute(name, new Attribute("DefaultLogLevel", "error"))
      assert(l.getLogLevel == LogLevel.ERROR)
    } finally {
      l.setLogLevel(currentLogLevel)
    }
  }

  test("support setting log levels through JMX") {
    // Creating JMX proxy is a bit complicated, so just test LoggerJMX impl here
    val current = LoggerJMX.getLogLevel("wvlet.log")
    try {
      LoggerJMX.setLogLevel("wvlet.log", "WARN")
      assert(LoggerJMX.getLogLevel("wvlet.log") == "warn")
    } finally {
      LoggerJMX.setLogLevel("wvlet.log", current)
    }
  }
}
