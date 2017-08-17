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

import javax.management.ObjectName
import javax.management.remote.{JMXConnectorFactory, JMXServiceURL}

import wvlet.test.WvletSpec

/**
  *
  */
class JMXAgentTest extends WvletSpec {

  "JMXAgent" should {
    "find jmx registry" in {
      val agent = new JMXAgent(new JMXConfig())
      agent.withConnetor { connector =>
        val connection = connector.getMBeanServerConnection()
        connection.getMBeanCount.toInt shouldBe >(0)
        val m = connection.getMBeanInfo(new ObjectName("java.lang:type=OperatingSystem"))
        m shouldNot be(null)
        debug(m)
      }
    }
  }
}
