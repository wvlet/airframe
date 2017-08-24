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

import wvlet.test.WvletSpec

import scala.util.Random

@JMX(description = "A example MBean object")
class SampleMBean {
  @JMX(description = "free memory size")
  def freeMemory: Long = {
    Runtime.getRuntime.freeMemory()
  }
}

case class FieldMBean(@JMX a: Int, @JMX b: String)

class NestedMBean {
  @JMX(description = "nested stat")
  def stat: Stat = {
    new Stat(Random.nextInt(10), "nested JMX bean")
  }
}

case class Stat(@JMX count: Int, @JMX state: String)

/**
  *
  */
class JMXRegistryTest extends WvletSpec {

  val agent = new JMXAgent(new JMXConfig())

  override def afterAll {
    agent.unregisterAll
  }

  "JMXRegistry" should {
    "register a new mbean" in {
      val b = new SampleMBean
      agent.register(b)
      val m = agent.getMBeanInfo("wvlet.airframe.jmx:name=SampleMBean")
      debug(m)

      val a = agent.getMBeanAttribute("wvlet.airframe.jmx:name=SampleMBean", "freeMemory")
      debug(a)
    }

    "support class field" taggedAs ("class-field") in {
      val f = new FieldMBean(1, "apple")
      agent.register(f)
      val m = agent.getMBeanInfo("wvlet.airframe.jmx:name=FieldMBean")
      info(m)

      agent.getMBeanAttribute("wvlet.airframe.jmx:name=FieldMBean", "a") shouldBe 1
      agent.getMBeanAttribute("wvlet.airframe.jmx:name=FieldMBean", "b") shouldBe "apple"
    }

    "handle nested JMX MBean" taggedAs ("nested") in {
      val n = new NestedMBean
      agent.register(n)
      val m = agent.getMBeanInfo("wvlet.airframe.jmx:name=NestedMBean")
      info(m)

      agent.getMBeanAttribute("wvlet.airframe.jmx:name=NestedMBean", "stat.count").toString.toInt should be <= 10
      agent.getMBeanAttribute("wvlet.airframe.jmx:name=NestedMBean", "stat.state") shouldBe ("nested JMX bean")
    }

  }
}
