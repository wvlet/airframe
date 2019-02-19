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
import wvlet.airframe.Alias.{HelloRef, StringHello}
import wvlet.airframe.surface.Surface

trait Message
case class Hello(message: String) extends Message

object Alias {
  trait Hello[A] {
    def hello: A
  }

  class StringHello extends Hello[String] {
    def hello = "hello world"
  }

  type HelloRef = Hello[String]
}

object DesignTest {
  type ProductionMessage  = Message
  type DevelopmentMessage = Message
  type ProductionString   = String

  val d0 = Design.empty

  val d1 =
    d0.bind[Message].to[Hello]
      .bind[Hello].toInstance(Hello("world"))
      .bind[Message].toSingleton
      .bind[Message].toEagerSingleton
      .bind[Message].toEagerSingletonOf[Hello]
      .bind[Message].toSingletonOf[Hello]
      .bind[ProductionMessage].toInstance(Hello("production"))
      .bind[DevelopmentMessage].toInstance(Hello("development"))
      .noLifeCycleLogging
}

/**
  *
  */
class DesignTest extends AirframeSpec {
  import DesignTest._

  val o = Hello("override")

  "Design" should {
    "be immutable" in {
      d0 shouldEqual Design.empty

      val d2 = d1.bind[Hello].toInstance(Hello("airframe"))
      d2 should not equal (d1)
    }

    "be appendable" in {
      val d2 = d1.bind[Hello].toInstance(o)

      val d3 = d1 + d2
      val d4 = d1.add(d2)

      d3.build[Hello] { h =>
        h should be theSameInstanceAs o
      }
      d4.build[Hello] { h =>
        h should be theSameInstanceAs o
      }
    }

    "display design" in {
      val s = d1.toString
      // sanity test
      s shouldNot be(empty)
      debug(d1.toString)
    }

    "remove binding" in {
      val dd = d1.remove[Message]

      def hasMessage(d: Design): Boolean =
        d.binding.exists(_.from == Surface.of[Message])
      def hasProductionMessage(d: Design): Boolean =
        d.binding.exists(_.from == Surface.of[ProductionMessage])

      hasMessage(d1) shouldBe true
      hasMessage(dd) shouldBe false

      hasProductionMessage(d1) shouldBe true
      hasProductionMessage(dd) shouldBe true
    }

    "bind providers" in {
      val d = newSilentDesign
        .bind[Hello].toProvider { (m: ProductionString) =>
          Hello(m)
        }
        .bind[ProductionString].toInstance("hello production")

      d.build[Hello] { h =>
        h.message shouldBe "hello production"
      }
    }

    "bind type aliases" taggedAs ("alias") in {
      val d = newSilentDesign
        .bind[HelloRef].toInstance(new StringHello)

      d.build[HelloRef] { h =>
        h.hello shouldBe "hello world"
      }
    }

    "start and stop session" in {
      // Sanity test
      newDesign.noLifeCycleLogging
        .withSession { session =>
          // Do nothing
        }
    }
  }
}
