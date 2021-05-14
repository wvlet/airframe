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
package wvlet.airframe.di.lifecycle

import java.util.concurrent.atomic.AtomicInteger
import wvlet.airframe.di.DIException.{MULTIPLE_SHUTDOWN_FAILURES, SHUTDOWN_FAILURE}
import wvlet.airframe.di.Design
import wvlet.airspec.AirSpec
import wvlet.log.{LogLevel, LogSupport, Logger}

class Counter extends LogSupport {
  val initialized = new AtomicInteger(0)
  val injected    = new AtomicInteger(0)
  val shutdowned  = new AtomicInteger(0)
  val started     = new AtomicInteger(0)
}

class CounterUser(
    val counter1: CounterService,
    val counter2: CounterService
)

class CounterService(val counter: Counter) extends LogSupport {
  def initCount     = counter.initialized.get()
  def injectCount   = counter.injected.get()
  def startCount    = counter.started.get()
  def shutdownCount = counter.shutdowned.get()
}

class User1(c: Counter) extends CounterService(c)
class User2(c: Counter) extends CounterService(c)

class LifeCycleOrder(v: Int) {}

/**
  */
object LifeCycleManagerTest extends AirSpec {
  scalaJsSupport

  private val counterDesign =
    Design.newSilentDesign
      .bind[CounterService].toSingleton
      .bind[Counter].toSingleton
      .onInit { c =>
        debug(s"init: ${c.initialized.get()}")
        c.initialized.incrementAndGet()
      }
      .onInject { c =>
        debug(s"injected: ${c.injected.get()}")
        c.injected.incrementAndGet()
      }
      .onStart { c =>
        debug(s"start: ${c.started.get()}")
        c.started.incrementAndGet()
      }
      .onShutdown { c =>
        debug(s"shutdown: ${c.shutdowned.get()}")
        c.shutdowned.incrementAndGet()
      }

  test("call lifecycle hooks properly for singleton") {
    val session      = counterDesign.newSession
    val multiCounter = session.build[CounterUser]
    multiCounter.counter1 shouldBeTheSameInstanceAs (multiCounter.counter2)

    multiCounter.counter1.initCount shouldBe 1
    multiCounter.counter1.injectCount shouldBe 1
    multiCounter.counter1.startCount shouldBe 0
    multiCounter.counter1.shutdownCount shouldBe 0

    session.start
    multiCounter.counter1.initCount shouldBe 1
    multiCounter.counter1.injectCount shouldBe 1
    multiCounter.counter1.startCount shouldBe 1
    multiCounter.counter1.shutdownCount shouldBe 0
    session.shutdown

    multiCounter.counter1.initCount shouldBe 1
    multiCounter.counter1.injectCount shouldBe 1
    multiCounter.counter1.startCount shouldBe 1
    multiCounter.counter1.shutdownCount shouldBe 1
  }

  test("start and shutdown only once for singleton referenced multiple times") {
    val session = counterDesign.newSession

    val u1 = session.build[User1]
    val u2 = session.build[User2]

    // Should have the same service instance
    u1.counter shouldBeTheSameInstanceAs (u2.counter)

    session.start

    session.shutdown

    // Counter should be initialized only once
    u1.initCount shouldBe 1
    u2.initCount shouldBe 1

    u1.injectCount shouldBe 1
    u2.injectCount shouldBe 1

    // Counter also should be started only once
    u1.startCount shouldBe 1
    u2.startCount shouldBe 1

    // Shutdown should be called only once
    u1.shutdownCount shouldBe 1
    u2.shutdownCount shouldBe 1
  }

  test("run start hook when the session is already started") {
    val session = counterDesign.newSession

    var cs: CounterService = null
    session.start {
      cs = session.build[CounterService]
      cs.initCount shouldBe 1
      cs.startCount shouldBe 1
      cs.shutdownCount shouldBe 0
    }
    cs.initCount shouldBe 1
    cs.startCount shouldBe 1
    cs.shutdownCount shouldBe 1
  }

  test("run start hook only once for singleton after session is started") {
    val session = counterDesign.newSession

    var cs: CounterService  = null
    var cs2: CounterService = null
    session.start {
      cs = session.build[CounterService]
      cs.initCount shouldBe 1
      cs.injectCount shouldBe 1
      cs.startCount shouldBe 1
      cs.shutdownCount shouldBe 0

      cs2 = session.build[CounterService]
      cs2.initCount shouldBe 1
      cs2.injectCount shouldBe 1 // CounterService is already instantiated
      cs2.startCount shouldBe 1
      cs2.shutdownCount shouldBe 0
    }
    cs.initCount shouldBe 1
    cs.injectCount shouldBe 1
    cs.startCount shouldBe 1
    cs.shutdownCount shouldBe 1

    cs.counter shouldBeTheSameInstanceAs (cs2.counter)
  }

  test("execute beforeShutdown hook") {
    val t           = new AtomicInteger(0)
    var init        = 0
    var start       = 0
    var preShutdown = 0
    var shutdown    = 0

    val session = Design.newSilentDesign
      .bind[Int].toInstance(0)
      .onInit { x => init = t.incrementAndGet() }
      .onStart { x => start = t.incrementAndGet() }
      .beforeShutdown { x => preShutdown = t.incrementAndGet() }
      .onShutdown { x => shutdown = t.incrementAndGet() }
      .newSession

    val l = session.build[LifeCycleOrder]
    session.start {}
    init shouldBe 1
    start shouldBe 2
    preShutdown shouldBe 3
    shutdown shouldBe 4
  }

  test("show life cycle log") {
    Design.newDesign.withSession { session =>
      // Just show debug logs
    }

    val l       = Logger("wvlet.airframe")
    val current = l.getLogLevel
    try {
      l.setLogLevel(LogLevel.DEBUG)
      Design.newSilentDesign.withSession { session =>
        // Show debug level session life cycle log
      }
    } finally {
      l.setLogLevel(current)
    }
  }

  class CloseExceptionTest extends AutoCloseable {
    override def close(): Unit = {
      throw new IllegalStateException("failure test")
    }
  }

  test("handle exceptions in shutdown hooks") {
    val e = intercept[SHUTDOWN_FAILURE] {
      Design.newSilentDesign.build[CloseExceptionTest] { x => }
    }
    e.getMessage.contains("failure test") shouldBe true
  }

  class MultipleShutdownExceptionTest(t: CloseExceptionTest) extends AutoCloseable {
    override def close(): Unit = {
      throw new IllegalStateException("failure 2")
    }
  }

  test("handle multiple exceptions") {
    val e = intercept[MULTIPLE_SHUTDOWN_FAILURES] {
      Design.newSilentDesign
        .bind[CloseExceptionTest].toSingleton // Inner class needs to be defined where the outer context can be found
        .build[MultipleShutdownExceptionTest] { x => }
    }
    debug(e)
    e.causes.find(_.getMessage.contains("failure test")) shouldBe defined
    e.causes.find(_.getMessage.contains("failure 2")) shouldBe defined
  }
}
