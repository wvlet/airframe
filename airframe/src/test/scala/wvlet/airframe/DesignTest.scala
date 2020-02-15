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
import wvlet.airframe.tracing.DefaultTracer
import wvlet.airspec.AirSpec

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
class DesignTest extends AirSpec {
  scalaJsSupport

  import DesignTest._

  val o = Hello("override")

  def `be immutable`: Unit = {
    d0 shouldBe Design.empty

    val d2 = d1.bind[Hello].toInstance(Hello("airframe"))
    d2 shouldNotBe d1
  }

  def `be appendable`: Unit = {
    val d2 = d1.bind[Hello].toInstance(o)

    val d3 = d1 + d2
    val d4 = d1.add(d2)

    d3.build[Hello] { h => h shouldBeTheSameInstanceAs o }
    d4.build[Hello] { h => h shouldBeTheSameInstanceAs o }
  }

  def `display design`: Unit = {
    val s = d1.toString
    // sanity test
    s shouldNotBe empty
    debug(d1.toString)
  }

  def `remove binding`: Unit = {
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

  def `bind providers`: Unit = {
    val d = newSilentDesign
      .bind[Hello].toProvider { (m: ProductionString) => Hello(m) }
      .bind[ProductionString].toInstance("hello production")

    d.build[Hello] { h => h.message shouldBe "hello production" }
  }

  def `bind type aliases`: Unit = {
    val d = newSilentDesign
      .bind[HelloRef].toInstance(new StringHello)

    d.build[HelloRef] { h => h.hello shouldBe "hello world" }
  }

  def `start and stop session`: Unit = {
    // Sanity test
    newDesign.noLifeCycleLogging
      .withSession { session =>
        // Do nothing
      }
  }

  def `set/unset options`: Unit = {
    val d = Design.newDesign
    d.withTracer(DefaultTracer).noTracer
  }

  def `preserve explicit design options`: Unit = {
    val d1 = Design.newDesign.withProductionMode.noLifeCycleLogging
    val d2 = Design.newDesign

    d1.designOptions.enabledLifeCycleLogging shouldBe Some(false)
    d1.designOptions.stage shouldBe Some(Stage.PRODUCTION)

    d2.designOptions.enabledLifeCycleLogging shouldBe empty
    d2.designOptions.stage shouldBe empty

    val d = d1 + d2
    d.designOptions.enabledLifeCycleLogging shouldBe Some(false)
    d.designOptions.stage shouldBe Some(Stage.PRODUCTION)

    val d3 = d2 + d1
    d3.designOptions.enabledLifeCycleLogging shouldBe Some(false)
    d3.designOptions.stage shouldBe Some(Stage.PRODUCTION)
  }

  def `override design options`: Unit = {
    val d1 = Design.newDesign.withProductionMode.noLifeCycleLogging
    val d2 = Design.newDesign.withLazyMode.withLifeCycleLogging
    val d  = d1 + d2
    d.designOptions.enabledLifeCycleLogging shouldBe Some(true)
    d.designOptions.stage shouldBe Some(Stage.DEVELOPMENT)
  }

  def `support run`: Unit = {
    val d = Design.newSilentDesign
      .bind[String].toInstance("hello")
    val ret = d.run { s: String =>
      s shouldBe "hello"
      100
    }
    ret shouldBe 100
  }
}
