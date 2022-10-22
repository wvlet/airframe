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

import wvlet.airspec.AirSpec
import wvlet.log.LogSupport

import java.lang.management.ManagementFactory
import javax.management.ObjectName
import scala.util.Random

@JMX(description = "A example MBean object")
class SampleMBean {
  @JMX(description = "free memory size")
  def freeMemory: Long = {
    Runtime.getRuntime.freeMemory()
  }
}

@JMX(description = "A example MBean object")
class NamedMBean {
  @JMX(name = "memory.free", description = "free memory size")
  def freeMemory: Long = {
    Runtime.getRuntime.freeMemory()
  }
}

case class FieldMBean(@JMX a: Int, @JMX b: String)

class NestedMBean {
  @JMX(description = "nested stat")
  def stat: Stat = {
    Stat(Random.nextInt(10), "nested JMX bean")
  }
}

case class Stat(@JMX count: Int, @JMX state: String)

trait MyJMXApp extends LogSupport {}
object MyJMXAppObj

/**
  */
class JMXRegistryTest extends AirSpec {
  val agent = new JMXAgent(new JMXConfig())

  override protected def afterAll: Unit = {
    agent.unregisterAll
  }

  test("register a new mbean") {
    val b = new SampleMBean
    agent.register(b)

    val mbeanServer = ManagementFactory.getPlatformMBeanServer
    val mbean       = mbeanServer.getMBeanInfo(new ObjectName("wvlet.airframe.jmx:name=SampleMBean"))
    val attrs       = mbean.getAttributes
    attrs.length shouldBe 1
    attrs(0).getName shouldBe "freeMemory"

    if (!JMXUtil.isAtLeastJava9) {
      val m = agent.getMBeanInfo("wvlet.airframe.jmx:name=SampleMBean")
      debug(m)

      val a = agent.getMBeanAttribute("wvlet.airframe.jmx:name=SampleMBean", "freeMemory")
      debug(a)
    }
  }

  test("specify property name by annotation") {
    val b = new NamedMBean
    agent.register(b)

    val mbeanServer = ManagementFactory.getPlatformMBeanServer
    val mbean       = mbeanServer.getMBeanInfo(new ObjectName("wvlet.airframe.jmx:name=NamedMBean"))
    val attrs       = mbean.getAttributes
    attrs.length shouldBe 1
    attrs(0).getName shouldBe "memory.free"

    if (!JMXUtil.isAtLeastJava9) {
      val m = agent.getMBeanInfo("wvlet.airframe.jmx:name=NamedMBean")
      debug(m)

      val a = agent.getMBeanAttribute("wvlet.airframe.jmx:name=NamedMBean", "memory.free")
      debug(a)
    }
  }

  test("support class field") {
    val f = new FieldMBean(1, "apple")
    agent.register(f)

    val mbeanServer = ManagementFactory.getPlatformMBeanServer
    val mbean       = mbeanServer.getMBeanInfo(new ObjectName("wvlet.airframe.jmx:name=FieldMBean"))
    mbean.getAttributes.length shouldBe 2
    mbeanServer.getAttribute(new ObjectName("wvlet.airframe.jmx:name=FieldMBean"), "a") shouldBe 1
    mbeanServer.getAttribute(new ObjectName("wvlet.airframe.jmx:name=FieldMBean"), "b") shouldBe "apple"

    if (!JMXUtil.isAtLeastJava9) {
      val m = agent.getMBeanInfo("wvlet.airframe.jmx:name=FieldMBean")
      info(m)

      agent.getMBeanAttribute("wvlet.airframe.jmx:name=FieldMBean", "a") shouldBe 1
      agent.getMBeanAttribute("wvlet.airframe.jmx:name=FieldMBean", "b") shouldBe "apple"
    }
  }

  test("handle nested JMX MBean") {
    val n = new NestedMBean
    agent.register(n)

    val mbeanServer = ManagementFactory.getPlatformMBeanServer
    val mbean       = mbeanServer.getMBeanInfo(new ObjectName("wvlet.airframe.jmx:name=NestedMBean"))
    mbean.getAttributes.length shouldBe 2
    mbeanServer
      .getAttribute(
        new ObjectName("wvlet.airframe.jmx:name=NestedMBean"),
        "stat.count"
      ).toString.toInt <= 10 shouldBe true
    mbeanServer.getAttribute(
      new ObjectName("wvlet.airframe.jmx:name=NestedMBean"),
      "stat.state"
    ) shouldBe "nested JMX bean"

    if (!JMXUtil.isAtLeastJava9) {
      val m = agent.getMBeanInfo("wvlet.airframe.jmx:name=NestedMBean")
      info(m)

      agent.getMBeanAttribute("wvlet.airframe.jmx:name=NestedMBean", "stat.count").toString.toInt <= 10 shouldBe true
      agent.getMBeanAttribute("wvlet.airframe.jmx:name=NestedMBean", "stat.state") shouldBe "nested JMX bean"
    }
  }

  test("avoid double registration") {
    val f = new FieldMBean(1, "apple")
    agent.register(f)
    agent.register(f)
  }

  test("support complex trait name") {
    agent.register[MyJMXApp](new MyJMXApp {})
  }
}
