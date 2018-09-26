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

object FactoryBindingTest {

  case class MyConfig(a: Int)
  case class MyConfig2(b: Int)
  case class D1(d: String)

  trait MyModule1 {
    val config = bind[MyConfig]
    val d1     = bind[D1]
  }

  case class MyModule2(config: MyConfig, d1: D1)

  trait MyModule3 {
    val c1 = bind[MyConfig]
    val c2 = bind[MyConfig2]
    val d1 = bind[D1]
  }

  trait FactorySet {
    val factory1 = bindFactory[MyConfig => MyModule1]

    //val factory3 = bindFactory[(MyConfig, MyConfig2) => MyModule3]
  }

  trait FactorySet2 {
    val factory = bindFactory[MyConfig => MyModule2]
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
        val i1 = f.factory1(MyConfig(15))
        i1.config shouldBe MyConfig(15)
        i1.d1 shouldBe d1

        val i2 = f.factory1(MyConfig(16))
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
  }
}
