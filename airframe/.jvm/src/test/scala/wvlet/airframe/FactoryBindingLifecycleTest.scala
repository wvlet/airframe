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
package wvlet.airframe
import java.util.concurrent.atomic.AtomicInteger

import javax.annotation.{PostConstruct, PreDestroy}
import wvlet.airspec.AirSpec
import wvlet.log.LogSupport

object FactoryBindingLifecycleTest {
  val startCounter  = collection.mutable.Map[Int, AtomicInteger]()
  val endCounter    = collection.mutable.Map[Int, AtomicInteger]()
  val threadCounter = new AtomicInteger()

  trait MyThread extends LogSupport {
    debug("hello MyThread")
    threadCounter.incrementAndGet()
  }

  trait MyClient extends LogSupport {
    val port      = bind[Int]
    val singleton = bind[MyThread]

    @PostConstruct
    def start: Unit = {
      debug(s"start client for ${port}")
      startCounter.getOrElseUpdate(port, new AtomicInteger()).incrementAndGet()
    }

    @PreDestroy
    def end: Unit = {
      debug(s"end client for ${port}")
      endCounter.getOrElseUpdate(port, new AtomicInteger()).incrementAndGet()
    }
  }

  trait ClientFactory {
    val factory = bindFactory[Int => MyClient]
  }
}

/**
  */
class FactoryBindingLifecycleTest extends AirSpec {
  import FactoryBindingLifecycleTest._

  def `run shutdown hooks`: Unit = {
    threadCounter.get() shouldBe 0
    newSilentDesign.build[ClientFactory] { f =>
      startCounter shouldBe empty
      endCounter shouldBe empty

      val c1 = f.factory(8081)
      startCounter(8081).get() shouldBe 1
      endCounter.get(8081) shouldBe empty

      val c2 = f.factory(8082)
      startCounter(8082).get() shouldBe 1
      endCounter.get(8082) shouldBe empty
    }

    startCounter(8081).get() shouldBe 1
    startCounter(8082).get() shouldBe 1
    endCounter(8081).get() shouldBe 1
    endCounter(8082).get() shouldBe 1

    threadCounter.get() shouldBe 1 // Generate the singleton MyThread only once
  }
}
