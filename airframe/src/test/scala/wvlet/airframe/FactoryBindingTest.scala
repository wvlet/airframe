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
import wvlet.log.LogSupport

object FactoryBindingTest {

  case class MyConfig(a: Int)
  case class MyConfig2(b: Int)
  case class MyConfig3(b: Int = 0)
  case class MyConfig4(b: Int = 0)
  case class MyConfig5(b: Int = 0)
  case class D1(d: String)

  trait MyModule1 {
    val config = bind[MyConfig]
    val d1     = bind[D1]
  }

  case class MyModule2(config: MyConfig, d1: D1)

  trait MyModule3 {
    val c1 = bind[MyConfig]
    val c2 = bind[MyConfig2]
    val c3 = bind[MyConfig3]
    val c4 = bind[MyConfig4]
    val c5 = bind[MyConfig5]
    val d1 = bind[D1]
  }

  trait FactorySet {
    val factory = bindFactory[MyConfig => MyModule1]
  }

  trait FactorySet2 {
    val factory = bindFactory[MyConfig => MyModule2]
  }

  trait FactorySet3 {
    val f2 = bindFactory2[(MyConfig, MyConfig2) => MyModule3]
    val f3 = bindFactory3[(MyConfig, MyConfig2, MyConfig3) => MyModule3]
    val f4 = bindFactory4[(MyConfig, MyConfig2, MyConfig3, MyConfig4) => MyModule3]
    val f5 = bindFactory5[(MyConfig, MyConfig2, MyConfig3, MyConfig4, MyConfig5) => MyModule3]
  }

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
  *
  */
class FactoryBindingTest extends AirframeSpec {
  import FactoryBindingTest._

  val c1 = MyConfig(10)
  val c2 = MyConfig2(20)
  val d1 = D1("hello hello")

  val d = newDesign.noLifeCycleLogging
    .bind[MyConfig].toInstance(c1)
    .bind[MyConfig2].toInstance(c2)
    .bind[D1].toInstance(d1)

  "Airframe binding" should {
    "create factories to override partial binding" in {
      d.build[FactorySet] { f =>
        val i1 = f.factory(MyConfig(15))
        i1.config shouldBe MyConfig(15)
        i1.d1 shouldBe d1

        val i2 = f.factory(MyConfig(16))
        i2.config shouldBe MyConfig(16)
        i2.d1 shouldBe d1
      }
    }

    "create constructor binding factories" in {
      d.build[FactorySet2] { f =>
        val j1 = f.factory(MyConfig(17))
        j1.config shouldBe MyConfig(17)
        j1.d1 shouldBe d1

        val j2 = f.factory(MyConfig(18))
        j2.config shouldBe MyConfig(18)
        j2.d1 shouldBe d1
      }
    }

    "create factory of many args" in {
      d.build[FactorySet3] { f =>
        {
          val j = f.f2(MyConfig(2), MyConfig2(3))
          j.c1 shouldBe MyConfig(2)
          j.c2 shouldBe MyConfig2(3)
          j.d1 shouldBe d1
        }

        {
          val j = f.f2(MyConfig(3), MyConfig2(4))
          j.c1 shouldBe MyConfig(3)
          j.c2 shouldBe MyConfig2(4)
          j.d1 shouldBe d1
        }

        {
          val j = f.f3(MyConfig(1), MyConfig2(2), MyConfig3(3))
          j.c1 shouldBe MyConfig(1)
          j.c2 shouldBe MyConfig2(2)
          j.c3 shouldBe MyConfig3(3)
          j.d1 shouldBe d1
        }

        {
          val j = f.f4(MyConfig(1), MyConfig2(2), MyConfig3(3), MyConfig4(4))
          j.c1 shouldBe MyConfig(1)
          j.c2 shouldBe MyConfig2(2)
          j.c3 shouldBe MyConfig3(3)
          j.c4 shouldBe MyConfig4(4)
          j.d1 shouldBe d1
        }

        {
          val j = f.f5(MyConfig(1), MyConfig2(2), MyConfig3(3), MyConfig4(4), MyConfig5(5))
          j.c1 shouldBe MyConfig(1)
          j.c2 shouldBe MyConfig2(2)
          j.c3 shouldBe MyConfig3(3)
          j.c4 shouldBe MyConfig4(4)
          j.c5 shouldBe MyConfig5(5)
          j.d1 shouldBe d1
        }
      }
    }

    "run shutdown hooks" in {
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
}
