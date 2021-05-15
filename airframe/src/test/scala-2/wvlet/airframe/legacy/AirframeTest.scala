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
package wvlet.airframe.legacy

import java.io.PrintStream
import java.util.concurrent.atomic.AtomicInteger

import wvlet.airframe.AirframeException.{CYCLIC_DEPENDENCY, MISSING_DEPENDENCY, MISSING_SESSION}
import wvlet.airframe.lifecycle.{LifeCycleEventHandler, LifeCycleManager}
import wvlet.log.LogSupport
import wvlet.airframe.surface.{Primitive, Surface}
import wvlet.airspec.AirSpec

import scala.util.Random

case class ExecutorConfig(numThreads: Int)

object ServiceMixinExample {
  trait Printer {
    def print(s: String): Unit
  }

  case class ConsoleConfig(out: PrintStream)

  class ConsolePrinter(config: ConsoleConfig) extends Printer with LogSupport {
    debug(s"using config: ${config}")

    def print(s: String): Unit = { config.out.println(s) }
  }

  class LogPrinter extends Printer with LogSupport {
    def print(s: String): Unit = { debug(s) }
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
    debug(f"Heavy Process!!: ${this.hashCode()}%x")
  }

  trait HeavySingletonService {
    val heavy = bind[HeavyObject]
  }

  trait AirframeAppA extends HeavySingletonService {}

  trait AirframeAppB extends HeavySingletonService {}

  case class A(b: B)
  case class B(a: A)

  class EagerSingleton extends LogSupport {
    debug("initialized")
    val initializedTime = System.nanoTime()
  }

  class ClassWithContext(val c: Session) extends FortunePrinterMixin with LogSupport {
    //info(s"context ${c}") // we should access context since Scala will remove private field, which is never used
  }

  case class HelloConfig(message: String)

  trait FactoryExample {
    val hello  = bind { config: HelloConfig => s"${config.message}" }
    val hello2 = bind { (c1: HelloConfig, c2: EagerSingleton) => s"${c1.message}:${c2.getClass.getSimpleName}" }

    val helloFromProvider = bind(provider _)

    def provider(config: HelloConfig): String = config.message
  }

  case class LocalFruit(name: String)

  type Apple  = LocalFruit
  type Banana = LocalFruit
  type Lemon  = LocalFruit

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
    debug("instantiated Nest1")
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
    def hello: Unit = { debug("hello!") }
  }

  object ConcreteSingleton extends AbstractModule with LogSupport {
    def hello: Unit = { debug("hello singleton!") }
  }

  trait NonAbstractModule extends LogSupport {
    debug("This should be built")
  }

  object SingletonOfNonAbstractModules extends NonAbstractModule {
    debug("Hello singleton")
  }

  trait NestedAbstractModule {
    val m = bind[AbstractModule]
  }

  trait EagerSingletonWithInject extends LogSupport {
    debug("initialized")
    val heavy           = bind[HeavyObject]
    val initializedTime = System.nanoTime()
  }

  class MyModule extends LogSupport {
    val initCount  = new AtomicInteger(0)
    val startCount = new AtomicInteger(0)
    var closeCount = new AtomicInteger(0)

    def init: Unit = {
      debug("initialized")
      initCount.incrementAndGet()
    }
    def start: Unit = {
      debug("started")
      startCount.incrementAndGet()
    }

    def close: Unit = {
      debug("closed")
      closeCount.incrementAndGet()
    }
  }

  trait LifeCycleExample {
    val module = bind[MyModule]
      .onInit(_.init)
      .onShutdown(_.close)
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
  */
class AirframeTest extends AirSpec {
  scalaJsSupport

  def `be able to use wvlet.airframe.Design to define a new design`: Unit = {
    val d = Design.newDesign

    // For test coverage
    d.withLifeCycleLogging.noLifeCycleLogging
      .withSession { session =>
        // do nothing
      }
  }

  def `create a design`: Unit = {
    // Both should work
    val d  = newDesign.bind[Printer].to[ConsolePrinter]
    val d1 = newDesign.bind[Printer].to[ConsolePrinter]
  }

  def `instantiate class`: Unit = {
    val d = newDesign
      .bind[Printer].to[ConsolePrinter]
      .bind[ConsoleConfig].toInstance(ConsoleConfig(System.err))

    val m = d.newSession.build[FortunePrinterMixin]
  }

  def `create singleton`: Unit = {
    val d = newDesign
      .bind[HeavyObject].toSingleton

    val session = d.newSession
    val a       = session.build[AirframeAppA]
    val b       = session.build[AirframeAppB]
    a.heavy shouldBe b.heavy
  }

  def `create singleton eagerly`: Unit = {
    val start = System.nanoTime()
    val session =
      newDesign
        .bind[EagerSingleton].toEagerSingleton
        .newSession
    val current = System.nanoTime()
    val s       = session.build[EagerSingleton]
    s.initializedTime >= start shouldBe true
    s.initializedTime <= current shouldBe true
  }

  def `create eager singleton type`: Unit = {
    val d = newDesign
      .bind[ConsoleConfig].toInstance(ConsoleConfig(System.err))
      .bind[Printer].toEagerSingletonOf[ConsolePrinter]

    val p = d.newSession.build[Printer]
    p.getClass shouldBe classOf[ConsolePrinter]

    val d2 = d.bind[Printer].toSingletonOf[ConsolePrinter]
    val ho = d2.newSession.build[Printer]
    ho.getClass shouldBe classOf[ConsolePrinter]
  }

  def `forbid binding to the same type`: Unit = {
    warn("Running cyclic dependency check test")
    val ex = intercept[CYCLIC_DEPENDENCY] {
      val d = newDesign
        .bind[Printer].to[Printer]
    }
    ex.deps.contains(Surface.of[Printer]) shouldBe true
    ex.toString.contains("CYCLIC_DEPENDENCY") shouldBe true

    intercept[CYCLIC_DEPENDENCY] {
      val d = newDesign
        .bind[Printer].toSingletonOf[Printer]
    }.deps.contains(Surface.of[Printer]) shouldBe true

    intercept[CYCLIC_DEPENDENCY] {
      val d = newDesign
        .bind[Printer].toEagerSingletonOf[Printer]
    }.deps.contains(Surface.of[Printer]) shouldBe true
  }

  def `found cyclic dependencies`: Unit = {
    //
    pendingUntil("Fixing a compilation error in Surface")
    //      val c = newDesign.newSession
    //      warn("Running cyclic dependency test: A->B->A")
    //
    //      val caught = intercept[CYCLIC_DEPENDENCY] {
    //        c.build[HasCycle]
    //      }
    //      warn(s"${caught}")
    //      caught.deps should contain(Surface.of[A])
    //      caught.deps should contain(Surface.of[B])
  }

  def `detect missing dependencies`: Unit = {
    val d = newDesign
    warn("Running missing dependency check")
    val caught = intercept[MISSING_DEPENDENCY] {
      d.newSession.build[MissingDep]
    }
    warn(s"${caught}")
    caught.stack.contains(Primitive.String) shouldBe true
  }

  def `find a session in parameter`: Unit = {
    pending
    val session = newDesign
      .bind[Printer].to[ConsolePrinter]
      .bind[ConsoleConfig].toInstance(ConsoleConfig(System.err))
      .newSession
    new ClassWithContext(session)
  }

  def `support binding listener`: Unit = {
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

  def `support binding via factory`: Unit = {
    val d = newDesign
      .bind[HelloConfig].toInstance(HelloConfig("Hello Airframe!"))

    val session = d.newSession
    val f       = session.build[FactoryExample]
    f.hello shouldBe "Hello Airframe!"
    f.helloFromProvider shouldBe "Hello Airframe!"

    debug(f.hello2)
  }
  def `support type alias`: Unit = {
    val apple = Surface.of[Apple]
    debug(s"apple: ${apple}, alias:${apple.isAlias}")

    val d = newDesign
      .bind[Apple].toInstance(LocalFruit("apple"))
      .bind[Banana].toInstance(LocalFruit("banana"))
      .bind[Lemon].toInstance(LocalFruit("lemon"))

    val session = d.newSession
    val tagged  = session.build[TaggedBinding]
    tagged.apple.name shouldBe ("apple")
    tagged.banana.name shouldBe ("banana")
    tagged.lemon.name shouldBe ("lemon")
  }

  def `support nested context injection`: Unit = {
    val session = newDesign.newSession
    session.build[Nested]
  }
  def `support injecting to a class`: Unit = {
    val d = newDesign
    val s = d.newSession.build[ClassInjection]
    s.obj != null shouldBe true

    d.newSession.build[NestedClassInjection]
  }

  def `build abstract type that has concrete binding`: Unit = {
    val d = newDesign
      .bind[AbstractModule].to[ConcreteModule]
    val s = d.newSession
    val m = s.build[AbstractModule]
    m.hello
  }

  def `build nested abstract type that has concrete binding`: Unit = {
    val d = newDesign
      .bind[AbstractModule].to[ConcreteModule]
    val s = d.newSession
    val m = s.build[NestedAbstractModule]
    m.m.hello
  }

  def `build a trait bound to singleton`: Unit = {
    val d = newDesign
      .bind[AbstractModule].toInstance(ConcreteSingleton)
    val s = d.newSession
    val m = s.build[AbstractModule]
    m.hello
  }

  def `build a trait`: Unit = {
    val h = newDesign
    val s = h.newSession
    val m = s.build[NonAbstractModule]
  }

  def `build a trait to singleton`: Unit = {
    val d =
      newDesign
        .bind[NonAbstractModule].toInstance(SingletonOfNonAbstractModules)

    val m = d.newSession.build[NonAbstractModule]
    m shouldBeTheSameInstanceAs SingletonOfNonAbstractModules
  }

  def `create single with inject eagerly`: Unit = {
    val start = System.nanoTime()
    val d = newSilentDesign
      .bind[EagerSingletonWithInject].toEagerSingleton
    val s       = d.newSession.build[EagerSingletonWithInject]
    val current = System.nanoTime()
    s.initializedTime >= start shouldBe true
    s.initializedTime <= current shouldBe true
  }

  def `support onInit and onShutdown`: Unit = {
    val session = newSilentDesign.newSession
    val e       = session.build[LifeCycleExample]
    e.module.initCount.get() shouldBe 1
    session.start
    session.shutdown
    e.module.closeCount.get() shouldBe 1
  }

  def `bind lifecycle`: Unit = {
    val session = newSilentDesign.newSession
    val e       = session.build[BindLifeCycleExample2]
    e.module.initCount.get() shouldBe 1

    session.start
    e.module.startCount.get() shouldBe 1

    session.shutdown
    e.module.closeCount.get() shouldBe 1
  }

  def `extend Design`: Unit = {
    val d1 = newDesign
      .bind[HeavyObject].toSingleton

    val d2 = newDesign
      .bind[ConsoleConfig].toInstance(ConsoleConfig(System.err))

    val d = d1 + d2

    val session = d.noLifeCycleLogging.newSession
    session.build[HeavyObject]
    session.build[ConsoleConfig]
  }

  def `throw MISSING_SESSION`: Unit = {
    warn("Running MISSING_SESSION test")
    val caught = intercept[MISSING_SESSION] {
      Session.findSession(Surface.of[Test], new Test {})
    }
    warn(caught.getMessage)
  }
}
