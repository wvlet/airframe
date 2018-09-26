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

import java.io.PrintStream
import java.util.concurrent.atomic.AtomicInteger

import wvlet.airframe.AirframeException.{CYCLIC_DEPENDENCY, MISSING_DEPENDENCY, MISSING_SESSION}
import wvlet.log.LogSupport
import wvlet.surface
import wvlet.surface.{Primitive, Surface}

import scala.util.Random

case class ExecutorConfig(numThreads: Int)

object ServiceMixinExample {

  trait Printer {
    def print(s: String): Unit
  }

  case class ConsoleConfig(out: PrintStream)

  class ConsolePrinter(config: ConsoleConfig) extends Printer with LogSupport {
    info(s"using config: ${config}")

    def print(s: String): Unit = { config.out.println(s) }
  }

  class LogPrinter extends Printer with LogSupport {
    def print(s: String): Unit = { info(s) }
  }

  class Fortune {
    def generate: String = {
      val pattern = Seq("Hello", "How are you?")
      pattern(Random.nextInt(pattern.length))
    }
  }

  trait PrinterService {
    protected def printer = bind[Printer]
  }

  trait FortuneService {
    protected def fortune = bind[Fortune]
  }

  /**
    * Mix-in printer/fortune instances
    * Pros:
    *   - trait can be shared with multiple components
    *   - xxxService trait can be a module
    * Cons:
    *   - Need to define XXXService boilerplate, which just has a val or def of the service object
    *   - Cannot change the variable name without defining additional XXXService trait
    *     - Need to care about variable naming conflict
    *   - We don't know the missing dependency at compile time
    */
  trait FortunePrinterMixin extends PrinterService with FortuneService {
    printer.print(fortune.generate)
  }

  /**
    * Using local val/def injection
    *
    * Pros:
    *   - Service reference (e.g., printer, fortune) can be scoped inside the trait.
    *   - No boilerplate code is required
    * Cons:
    *   - To reuse it in other traits, we still need to care about the naming conflict
    */
  trait FortunePrinterEmbedded {
    protected def printer = bind[Printer]
    protected def fortune = bind[Fortune]

    printer.print(fortune.generate)
  }

  /**
    * Using Constructor for dependency injection (e.g., Guice)
    *
    * Pros:
    *   - Close to the traditional OO programming style
    *   - Can be used without DI framework
    * Cons:
    *   - To add/remove modules, we need to create another constructor or class.
    * -> code duplication occurs
    *   - It's hard to enhance the class functionality
    *   - Users needs to know the order of constructor arguments
    * -
    */
  //class FortunePrinterAsClass @Inject ()(printer: Printer, fortune: Fortune) {
  //    printer.print(fortune.generate)
  //}

  class HeavyObject() extends LogSupport {
    info(f"Heavy Process!!: ${this.hashCode()}%x")
  }

  trait HeavySingletonService {
    val heavy = bind[HeavyObject]
  }

  trait AirframeAppA extends HeavySingletonService {}

  trait AirframeAppB extends HeavySingletonService {}

  case class A(b: B)
  case class B(a: A)

  class EagerSingleton extends LogSupport {
    info("initialized")
    val initializedTime = System.nanoTime()
  }

  class ClassWithContext(val c: Session) extends FortunePrinterMixin with LogSupport {
    //info(s"context ${c}") // we should access context since Scala will remove private field, which is never used
  }

  case class HelloConfig(message: String)

  trait FactoryExample {
    val hello = bind { config: HelloConfig =>
      s"${config.message}"
    }
    val hello2 = bind { (c1: HelloConfig, c2: EagerSingleton) =>
      s"${c1.message}:${c2.getClass.getSimpleName}"
    }

    val helloFromProvider = bind(provider _)

    def provider(config: HelloConfig): String = config.message
  }

  case class Fruit(name: String)

  type Apple  = Fruit
  type Banana = Fruit
  type Lemon  = Fruit

  trait TaggedBinding {
    val apple  = bind[Apple]
    val banana = bind[Banana]
    val lemon  = bind(lemonProvider _)

    def lemonProvider(f: Lemon) = f
  }

  trait Nested {
    val nest = bind[Nest1]
  }

  trait Nest1 extends LogSupport {
    info("instantiated Nest1")
    val nest2 = bind[Nest2]
  }

  class Nest2()

  class ClassInjection {
    val obj = bind[HeavyObject]
  }

  class NestedClassInjection {
    val nest = bind[Nest1]
  }

  trait AbstractModule {
    def hello: Unit
  }

  trait ConcreteModule extends AbstractModule with LogSupport {
    def hello: Unit = { info("hello!") }
  }

  object ConcreteSingleton extends AbstractModule with LogSupport {
    def hello: Unit = { info("hello singleton!") }
  }

  trait NonAbstractModule extends LogSupport {
    info(s"This should be built")
  }

  object SingletonOfNonAbstractModules extends NonAbstractModule {
    info("Hello singleton")
  }

  trait NestedAbstractModule {
    val m = bind[AbstractModule]
  }

  trait EagerSingletonWithInject extends LogSupport {
    info("initialized")
    val heavy           = bind[HeavyObject]
    val initializedTime = System.nanoTime()
  }

  class MyModule extends LogSupport {

    val initCount  = new AtomicInteger(0)
    val startCount = new AtomicInteger(0)
    var closeCount = new AtomicInteger(0)

    def init: Unit = {
      info("initialized")
      initCount.incrementAndGet()
    }
    def start: Unit = {
      info("started")
      startCount.incrementAndGet()
    }

    def close: Unit = {
      info("closed")
      closeCount.incrementAndGet()
    }
  }

  trait LifeCycleExample {
    val module = bind[MyModule]
      .onInit(_.init)
      .onShutdown(_.close)
  }

  trait BindLifeCycleExample {
    val module = bind[MyModule].withLifeCycle(
      init = _.init,
      start = _.start,
      shutdown = _.close
    )
  }

  trait BindLifeCycleExample2 {
    val module = bind[MyModule]
      .onInit(_.init)
      .onStart(_.start)
      .onShutdown(_.close)
  }

  trait MissingDep {
    val obj = bind[String]
  }

  trait Test
}

import wvlet.airframe.ServiceMixinExample._

/**
  *
  */
class AirframeTest extends AirframeSpec {

  "Airframe" should {

    "be able to use wvlet.airframe.Airframe to define a new design" in {
      val d = wvlet.airframe.Airframe.newDesign

      // For test coverage
      d.withLifeCycleLogging.noLifeCycleLogging
        .withSession { session =>
          }
    }

    "create a design" in {
      // Both should work
      val d  = newDesign.bind[Printer].to[ConsolePrinter]
      val d1 = newDesign.bind[Printer].to[ConsolePrinter]
    }

    "instantiate class" in {
      val d = newDesign
        .bind[Printer].to[ConsolePrinter]
        .bind[ConsoleConfig].toInstance(ConsoleConfig(System.err))

      val m = d.newSession.build[FortunePrinterMixin]
    }

    "create singleton" in {
      val d = newDesign
        .bind[HeavyObject].toSingleton

      val session = d.newSession
      val a       = session.build[AirframeAppA]
      val b       = session.build[AirframeAppB]
      a.heavy shouldEqual b.heavy
    }

    "create singleton eagerly" in {
      val start = System.nanoTime()
      val session =
        newDesign
          .bind[EagerSingleton].toEagerSingleton
          .newSession
      val current = System.nanoTime()
      val s       = session.build[EagerSingleton]
      s.initializedTime should be >= start
      s.initializedTime should be <= current
    }

    "create eager singleton type" taggedAs ("to-singleton") in {
      val d = newDesign
        .bind[ConsoleConfig].toInstance(ConsoleConfig(System.err))
        .bind[Printer].toEagerSingletonOf[ConsolePrinter]

      val p = d.newSession.build[Printer]
      p.getClass shouldBe classOf[ConsolePrinter]

      val d2 = d.bind[Printer].toSingletonOf[ConsolePrinter]
      val ho = d2.newSession.build[Printer]
      ho.getClass shouldBe classOf[ConsolePrinter]
    }

    "forbid binding to the same type" in {
      warn(s"Running cyclic dependency check test")
      val ex = intercept[CYCLIC_DEPENDENCY] {
        val d = newDesign
          .bind[Printer].to[Printer]
      }
      ex.deps should contain(surface.of[Printer])
      ex.toString.contains("CYCLIC_DEPENDENCY") shouldBe true

      intercept[CYCLIC_DEPENDENCY] {
        val d = newDesign
          .bind[Printer].toSingletonOf[Printer]
      }.deps should contain(surface.of[Printer])

      intercept[CYCLIC_DEPENDENCY] {
        val d = newDesign
          .bind[Printer].toEagerSingletonOf[Printer]
      }.deps should contain(surface.of[Printer])
    }

//    trait HasCycle {
//      val obj = bind[A]
//    }

    "found cyclic dependencies" taggedAs ("cyclic") in {
      // This will be shown as compilation error in Surface
      pending
//      val c = newDesign.newSession
//      warn(s"Running cyclic dependency test: A->B->A")
//
//      val caught = intercept[CYCLIC_DEPENDENCY] {
//        c.build[HasCycle]
//      }
//      warn(s"${caught}")
//      caught.deps should contain(surface.of[A])
//      caught.deps should contain(surface.of[B])
    }

    "detect missing dependencies" in {
      val d = newDesign
      warn(s"Running missing dependency check")
      val caught = intercept[MISSING_DEPENDENCY] {
        d.newSession.build[MissingDep]
      }
      warn(s"${caught}")
      caught.stack should contain(Primitive.String)
    }

    "find a session in parameter" in {
      pending
      val session = newDesign
        .bind[Printer].to[ConsolePrinter]
        .bind[ConsoleConfig].toInstance(ConsoleConfig(System.err))
        .newSession
      new ClassWithContext(session)
    }

    "support binding listener" taggedAs ("listener") in {
      val counter = new AtomicInteger(0)

      val design =
        newDesign
          .bind[EagerSingleton].toEagerSingleton
          .bind[ConsoleConfig].toInstance(ConsoleConfig(System.err))

      val session = design.newSessionBuilder
        .withEventHandler(new LifeCycleEventHandler {
          override def onInit(l: LifeCycleManager, t: Surface, injectee: AnyRef): Unit = {
            logger.debug(s"injected: ${t}")
            counter.incrementAndGet()
          }
        })
        .create

      session.build[ConsoleConfig]
      counter.get shouldBe 2
    }

    "support binding via factory" taggedAs ("factory-binding") in {
      val d = newDesign
        .bind[HelloConfig].toInstance(HelloConfig("Hello Airframe!"))

      val session = d.newSession
      val f       = session.build[FactoryExample]
      f.hello shouldBe "Hello Airframe!"
      f.helloFromProvider shouldBe "Hello Airframe!"

      info(f.hello2)
    }

    "support type alias" taggedAs ("alias") in {
      val apple = surface.of[Apple]
      warn(s"apple: ${apple}, alias:${apple.isAlias}")

      val d = newDesign
        .bind[Apple].toInstance(Fruit("apple"))
        .bind[Banana].toInstance(Fruit("banana"))
        .bind[Lemon].toInstance(Fruit("lemon"))

      val session = d.newSession
      val tagged  = session.build[TaggedBinding]
      tagged.apple.name shouldBe ("apple")
      tagged.banana.name shouldBe ("banana")
      tagged.lemon.name shouldBe ("lemon")
    }

    "support nested context injection" taggedAs ("nested") in {
      val session = newDesign.newSession
      session.build[Nested]
    }

    "support injecting to a class" in {
      val d = newDesign
      val s = d.newSession.build[ClassInjection]
      s.obj shouldNot be(null)

      d.newSession.build[NestedClassInjection]
    }

    "build abstract type that has concrete binding" taggedAs ("abstract") in {
      val d = newDesign
        .bind[AbstractModule].to[ConcreteModule]
      val s = d.newSession
      val m = s.build[AbstractModule]
      m.hello
    }

    "build nested abstract type that has concrete binding" taggedAs ("nested-abstract") in {
      val d = newDesign
        .bind[AbstractModule].to[ConcreteModule]
      val s = d.newSession
      val m = s.build[NestedAbstractModule]
      m.m.hello
    }

    "build a trait bound to singleton" taggedAs ("singleton") in {
      val d = newDesign
        .bind[AbstractModule].toInstance(ConcreteSingleton)
      val s = d.newSession
      val m = s.build[AbstractModule]
      m.hello
    }

    "build a trait" taggedAs ("trait") in {
      val h = newDesign
      val s = h.newSession
      val m = s.build[NonAbstractModule]
    }

    "build a trait to singleton" taggedAs ("trait-singleton") in {
      val d =
        newDesign
          .bind[NonAbstractModule].toInstance(SingletonOfNonAbstractModules)

      val m = d.newSession.build[NonAbstractModule]
      m shouldBe theSameInstanceAs(SingletonOfNonAbstractModules)

    }

    "create single with inject eagerly" in {
      val start = System.nanoTime()
      val d = newSilentDesign
        .bind[EagerSingletonWithInject].toEagerSingleton
      val s       = d.newSession.build[EagerSingletonWithInject]
      val current = System.nanoTime()
      s.initializedTime should be >= start
      s.initializedTime should be <= current
    }

    "support onInit and onShutdown" taggedAs ("lifecycle") in {
      val session = newSilentDesign.newSession
      val e       = session.build[LifeCycleExample]
      e.module.initCount.get() shouldBe 1
      session.start
      session.shutdown
      e.module.closeCount.get() shouldBe 1
    }

    "bind lifecycle code" taggedAs ("bind-init") in {
      val session = newSilentDesign.newSession
      val e       = session.build[BindLifeCycleExample]
      e.module.initCount.get() shouldBe 1

      session.start
      e.module.startCount.get() shouldBe 1

      session.shutdown
      e.module.closeCount.get() shouldBe 1
    }

    "bind lifecycle" taggedAs ("bind-lifecycle") in {
      val session = newSilentDesign.newSession
      val e       = session.build[BindLifeCycleExample2]
      e.module.initCount.get() shouldBe 1

      session.start
      e.module.startCount.get() shouldBe 1

      session.shutdown
      e.module.closeCount.get() shouldBe 1
    }

    "extend Design" in {
      val d1 = newDesign
        .bind[HeavyObject].toSingleton

      val d2 = newDesign
        .bind[ConsoleConfig].toInstance(ConsoleConfig(System.err))

      val d = d1 + d2

      val session = d.noLifeCycleLogging.newSession
      session.build[HeavyObject]
      session.build[ConsoleConfig]
    }

    "throw MISSING_SESSION" in {

      warn("Running MISSING_SESSION test")
      val caught = intercept[MISSING_SESSION] {
        Session.findSession(surface.of[Test], new Test {})
      }
      warn(caught.getMessage)
    }
  }
}
