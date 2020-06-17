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
package wvlet.airframe.lifecycle

import java.util.concurrent.atomic.AtomicInteger

import wvlet.airframe.AirframeException.{MULTIPLE_SHUTDOWN_FAILURES, SHUTDOWN_FAILURE}
import wvlet.airframe.{bind, newDesign, newSilentDesign}
import wvlet.airspec.AirSpec
import wvlet.log.{LogLevel, LogSupport, Logger}
import wvlet.airframe._

class Counter extends LogSupport {
  val initialized = new AtomicInteger(0)
  val injected    = new AtomicInteger(0)
  val shutdowned  = new AtomicInteger(0)
  val started     = new AtomicInteger(0)
}

trait CounterUser {
  val counter1 = bind[CounterService]
  val counter2 = bind[CounterService]
}

trait CounterService extends LogSupport {
  val counterService = bind[Counter]
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

  def initCount     = counterService.initialized.get()
  def injectCount   = counterService.injected.get()
  def startCount    = counterService.started.get()
  def shutdownCount = counterService.shutdowned.get()
}

trait User1 extends CounterService
trait User2 extends CounterService

trait LifeCycleOrder {
  val t           = new AtomicInteger(0)
  var init        = 0
  var start       = 0
  var preShutdown = 0
  var shutdown    = 0

  val v = bind[Int] { 0 }
    .onInit { x => init = t.incrementAndGet() }
    .onStart { x => start = t.incrementAndGet() }
    .beforeShutdown { x => preShutdown = t.incrementAndGet() }
    .onShutdown { x => shutdown = t.incrementAndGet() }
}

/**
  */
class LifeCycleManagerTest extends AirSpec {
  scalaJsSupport

  def `call init hook`: Unit = {
    val c = newSilentDesign.bind[CounterService].toSingleton.newSession.build[CounterService]
    c.initCount shouldBe 1
  }

  def `call lifecycle hooks properly for singleton`: Unit = {
    val session      = newSilentDesign.bind[CounterService].toSingleton.newSession
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

  def `start and shutdown only once for singleton referenced multiple times`: Unit = {
    val session = newSilentDesign.bind[Counter].toSingleton.newSession

    val u1 = session.build[User1]
    val u2 = session.build[User2]

    // Shoud have the same service instance
    u1.counterService shouldBeTheSameInstanceAs (u2.counterService)

    session.start

    session.shutdown

    // Counter should be initialized only once
    u1.initCount shouldBe 1
    u2.initCount shouldBe 1

    u1.injectCount shouldBe 2
    u2.injectCount shouldBe 2

    // Counter also should be started only once
    u1.startCount shouldBe 1
    u2.startCount shouldBe 1

    // Shutdown should be called only once
    u1.shutdownCount shouldBe 1
    u2.shutdownCount shouldBe 1
  }

  def `run start hook when the session is already started`: Unit = {
    val session = newSilentDesign.newSession

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

  def `run start hook only once for singleton after session is started`: Unit = {
    val session = newSilentDesign.bind[Counter].toSingleton.newSession

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

    cs.counterService shouldBeTheSameInstanceAs (cs2.counterService)
  }

  def `execute beforeShutdown hook`: Unit = {
    val session = newSilentDesign.newSession
    val l       = session.build[LifeCycleOrder]
    session.start {}
    l.init shouldBe 1
    l.start shouldBe 2
    l.preShutdown shouldBe 3
    l.shutdown shouldBe 4
  }

  def `show life cycle log`: Unit = {
    newDesign.withSession { session =>
      // Just show debug logs
    }

    val l       = Logger("wvlet.airframe")
    val current = l.getLogLevel
    try {
      l.setLogLevel(LogLevel.DEBUG)
      newSilentDesign.withSession { session =>
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

  def `handle exceptions in shutdown hooks`: Unit = {
    val e = intercept[SHUTDOWN_FAILURE] {
      newSilentDesign.build[CloseExceptionTest] { x => }
    }
    e.getMessage.contains("failure test") shouldBe true
  }

  class MultipleShutdownExceptionTest(t: CloseExceptionTest) extends AutoCloseable {
    override def close(): Unit = {
      throw new IllegalStateException("failure 2")
    }
  }

  def `handle multiple exceptions`: Unit = {
    val e = intercept[MULTIPLE_SHUTDOWN_FAILURES] {
      newSilentDesign
        .bind[CloseExceptionTest].toSingleton // Inner class needs to be defined where the outer context can be found
        .build[MultipleShutdownExceptionTest] { x => }
    }
    debug(e)
    e.causes.find(_.getMessage.contains("failure test")) shouldBe defined
    e.causes.find(_.getMessage.contains("failure 2")) shouldBe defined
  }
}
