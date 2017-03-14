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

import wvlet.log.LogSupport

class Counter extends LogSupport {
  val injected  = new AtomicInteger(0)
  val shutdowned = new AtomicInteger(0)
  val started = new AtomicInteger(0)
}

trait CounterUser {
  val counter1 = bind[CounterService]
  val counter2 = bind[CounterService]
}

trait CounterService extends LogSupport {
  val counterService = bind[Counter]
                       .onInjection{ c =>
                         info(s"injection: ${c.injected.get()}")
                         c.injected.incrementAndGet()
                       }
                       .onStart { c =>
                         info(s"start: ${c.injected.get()}")
                         c.started.incrementAndGet()
                       }
                       .onShutdown{ c =>
                         info(s"shutdown: ${c.injected.get()}")
                         c.shutdowned.incrementAndGet()
                       }

  def injectionCount = counterService.injected.get()
  def startCount = counterService.started.get()
  def shutdownCount = counterService.shutdowned.get()
}

trait User1 extends CounterService
trait User2 extends CounterService

/**
  *
  */
class LifeCycleManagerTest extends AirframeSpec {
  "LifeCycleManager" should {
    "call inject hook" in {
      val c = newDesign
              .bind[CounterService].toSingleton
              .newSession
              .build[CounterService]
      c.counterService.injected shouldBe 1
    }

    "call start hook only once in singleton" taggedAs("start") in {
      val session = newDesign
                    .bind[CounterService].toSingleton
                    .newSession
      val multiCounter = session.build[CounterUser]

      session.start
      session.shutdown

      multiCounter.counter1.startCount shouldBe 1
      multiCounter.counter2.startCount shouldBe 1

      multiCounter.counter1.injectionCount shouldBe 2
      multiCounter.counter2.injectionCount shouldBe 2

      multiCounter.counter1.hashCode shouldBe multiCounter.counter2.hashCode
    }

    "start and shutdown only once for singleton referenced multiple times" taggedAs("multi") in {
      val session = newDesign
                    .bind[Counter].toSingleton
                    .newSession

      val u1 = session.build[User1]
      val u2 = session.build[User2]

      // Shoud have the same service instance
      u1.counterService shouldBe theSameInstanceAs (u2.counterService)

      session.start

      session.shutdown

      // Counter is initialized only once
      u1.startCount shouldBe 1
      u2.startCount shouldBe 1

      // Counter should be injected twice
      u1.injectionCount shouldBe 2
      u2.injectionCount shouldBe 2

      // But only single shutdown should be called
      u1.shutdownCount shouldBe 1
      u2.shutdownCount shouldBe 1
    }
  }
}
