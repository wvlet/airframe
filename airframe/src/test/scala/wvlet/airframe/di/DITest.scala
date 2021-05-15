package wvlet.airframe.di

import wvlet.airframe.AirframeException.{MISSING_DEPENDENCY, CYCLIC_DEPENDENCY}
import wvlet.airframe.Design
import wvlet.airframe.lifecycle.{LifeCycleEventHandler, LifeCycleManager}
import wvlet.airframe.surface.{Primitive, Surface}
import wvlet.airspec.AirSpec
import wvlet.log.LogSupport

import java.io.PrintStream
import java.util.concurrent.atomic.AtomicInteger
import scala.util.Random

import wvlet.airframe._

/**
  */
object DITest extends AirSpec {

  test("Use wvlet.airframe.di.Design to define a new design") {
    val d = Design.newDesign

    // For test coverage
    d.withLifeCycleLogging.noLifeCycleLogging
      .withSession { session =>
        // do nothing
      }
  }

  trait Printer {
    def print(s: String): Unit
  }

  test("create a design") {
    // Both should work
    val d  = Design.newDesign.bind[Printer].to[ConsolePrinter]
    val d1 = Design.newDesign.bind[Printer].to[ConsolePrinter]
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

  class FortunePrinter(printer: Printer, fortune: Fortune)

  test("instantiate class from constructor") {
    val d = Design.newSilentDesign
      .bind[Printer].to[ConsolePrinter]
      .bind[ConsoleConfig].toInstance(ConsoleConfig(System.err))

    val m = d.newSession.build[FortunePrinter]
  }

  test("bind eager singleton") {
    val d = Design.newSilentDesign
      .bind[ConsoleConfig].toInstance(ConsoleConfig(System.err))
      .bind[Printer].toEagerSingletonOf[ConsolePrinter]

    d.build[Printer] { p =>
      p.getClass shouldBe classOf[ConsolePrinter]
    }

    val d2 = d.bind[Printer].to[ConsolePrinter]
    d2.build[Printer] { ho =>
      ho.getClass shouldBe classOf[ConsolePrinter]
    }
  }

  test("forbid binding to the same type") {
    warn("Running self-cycle binding test")
    val ex = intercept[CYCLIC_DEPENDENCY] {
      val d = Design.newDesign
        .bind[Printer].to[Printer]
    }
    ex.deps.contains(Surface.of[Printer]) shouldBe true
    ex.toString.contains("CYCLIC_DEPENDENCY") shouldBe true

    intercept[CYCLIC_DEPENDENCY] {
      val d = Design.newDesign
        .bind[Printer].to[Printer]
    }.deps.contains(Surface.of[Printer]) shouldBe true

    intercept[CYCLIC_DEPENDENCY] {
      val d = Design.newDesign
        .bind[Printer].toEagerSingletonOf[Printer]
    }.deps.contains(Surface.of[Printer]) shouldBe true
  }

  class CycleB(a: CycleA)
  class CycleA(b: CycleB)

  def `found cyclic dependencies`: Unit = {
    warn("Running cyclic dependency test: A->B->A")
    val caught = intercept[CYCLIC_DEPENDENCY] {
      newSilentDesign.build[CycleA] { c => }
    }
    warn(s"${caught}")
    caught.deps.contains(Surface.of[CycleA]) shouldBe true
    caught.deps.contains(Surface.of[CycleB]) shouldBe true
  }

  class HeavyObject()
  class AirframeAppA(val heavy: HeavyObject)
  class AirframeAppB(val heavy: HeavyObject)

  test("create singleton") {
    val d = Design.newSilentDesign
      .bind[HeavyObject].toSingleton

    val session = d.newSession
    val a       = session.build[AirframeAppA]
    val b       = session.build[AirframeAppB]
    a.heavy shouldBeTheSameInstanceAs b.heavy
    session.close()
  }

  class EagerSingleton extends LogSupport {
    debug("initialized")
    val initializedTime = System.nanoTime()
  }

  test("create singleton eagerly") {
    val start = System.nanoTime()
    val session =
      Design.newSilentDesign
        .bind[EagerSingleton].toEagerSingleton
        .newSession
    val current = System.nanoTime()
    val s       = session.build[EagerSingleton]
    s.initializedTime >= start shouldBe true
    s.initializedTime <= current shouldBe true
  }

  class MissingDep(obj: String)

  test("detect missing dependencies") {
    warn("Running missing dependency check")
    val caught = intercept[MISSING_DEPENDENCY] {
      newSilentDesign.build[MissingDep] { m => }
    }
    warn(s"${caught}")
    caught.stack.contains(Primitive.String) shouldBe true
  }

  class SessionParam(val session: Session)

  test("Bind the current Session as a parameter") {
    newSilentDesign.withSession { session =>
      val p = session.build[SessionParam]
      p.session shouldBeTheSameInstanceAs session
    }
  }

  test("custom LiveCycleEventHandler") {
    val counter = new AtomicInteger(0)

    val design =
      newSilentDesign
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
    session.close()
  }

  case class LocalFruit(name: String)
  type Apple  = LocalFruit
  type Banana = LocalFruit
  type Lemon  = LocalFruit
  case class FruitMarket(apple: Apple, banana: Banana, lemon: Lemon)

  test("support type alias binding") {
    val apple = Surface.of[Apple]
    debug(s"apple: ${apple}, alias:${apple.isAlias}")

    val d = newSilentDesign
      .bind[Apple].toInstance(LocalFruit("apple"))
      .bind[Banana].toInstance(LocalFruit("banana"))
      .bind[Lemon].toInstance(LocalFruit("lemon"))

    d.build[FruitMarket] { market =>
      market.apple.name shouldBe ("apple")
      market.banana.name shouldBe ("banana")
      market.lemon.name shouldBe ("lemon")
    }
  }

  class Nested(val nested1: Nested1)
  class Nested1(val nested2: Nested2)
  class Nested2()

  test("support nested constructor injection") {
    val n2 = new Nested2()
    newSilentDesign
      .bind[Nested2].toInstance(n2)
      .build[Nested] { n =>
        n.nested1.nested2 shouldBeTheSameInstanceAs n2
      }
  }

  trait AbstractModule {
    def hello: String
  }

  class ConcreteModule extends AbstractModule with LogSupport {
    override def hello: String = "hello"
  }

  test("build abstract type that has concrete binding") {
    val d = newSilentDesign
      .bind[AbstractModule].to[ConcreteModule]
    d.build[AbstractModule] { m =>
      m.hello shouldBe "hello"
    }
  }

  class NestedAbstractModule(val a: AbstractModule)

  test("build nested abstract type that has concrete binding") {
    val d = newSilentDesign
      .bind[AbstractModule].to[ConcreteModule]

    d.build[NestedAbstractModule] { n =>
      n.a.hello shouldBe "hello"
    }
  }

  test("build a trait bound to an instance") {
    val d = newSilentDesign
      .bind[AbstractModule].toInstance(new ConcreteModule())

    d.build[AbstractModule] { n =>
      n.hello shouldBe "hello"
    }
  }

  trait NonAbstractTrait {}

  object SingletonOfNonAbstractTrait extends NonAbstractTrait with LogSupport {
    debug("Hello singleton")
  }

  test("build a trait to singleton") {
    val d =
      newSilentDesign
        .bind[NonAbstractTrait].toInstance(SingletonOfNonAbstractTrait)

    d.build[NonAbstractTrait] { m =>
      m shouldBeTheSameInstanceAs SingletonOfNonAbstractTrait
    }
  }

  class EagerSingletonWithInject(heavy: HeavyObject) extends LogSupport {
    debug("initialized")
    val initializedTime = System.nanoTime()
  }

  test("create single with inject eagerly") {
    val start = System.nanoTime()
    val d = newSilentDesign
      .bind[EagerSingletonWithInject].toEagerSingleton
    val s       = d.newSession.build[EagerSingletonWithInject]
    val current = System.nanoTime()
    s.initializedTime >= start shouldBe true
    s.initializedTime <= current shouldBe true
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

  class LifeCycleExample(val module: MyModule)

  class BindLifeCycleExample2(val module: MyModule) {}

  test("support onInit and onShutdown") {
    val d = newSilentDesign
      .bind[MyModule].onInit(_.init).onShutdown(_.close)

    val session = d.newSession
    val e       = session.build[LifeCycleExample]
    e.module.initCount.get() shouldBe 1
    session.start
    session.shutdown
    e.module.closeCount.get() shouldBe 1
  }

  test("bind lifecycle") {
    val session = newSilentDesign
      .bind[MyModule]
      .onInit(_.init)
      .onStart(_.start)
      .onShutdown(_.close)
      .newSession

    val e = session.build[BindLifeCycleExample2]
    e.module.initCount.get() shouldBe 1

    session.start
    e.module.startCount.get() shouldBe 1

    session.shutdown
    e.module.closeCount.get() shouldBe 1
  }

  test("extend Design") {
    val d1 = Design.newDesign
      .bind[HeavyObject].toSingleton

    val d2 = Design.newDesign
      .bind[ConsoleConfig].toInstance(ConsoleConfig(System.err))

    val d = d1 + d2

    val session = d.noLifeCycleLogging.newSession
    session.build[HeavyObject]
    session.build[ConsoleConfig]
  }

}
