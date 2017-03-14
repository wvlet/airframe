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
  private val injectCounter  = new AtomicInteger(0)
  private val shutdown = new AtomicInteger(0)
  private val start = new AtomicInteger(0)

  def current: Int = injectCounter.get()
  def shutdownCount: Int = shutdown.get
  def startCount : Int = start.get

  def inject {
    info(s"injected: ${injectCounter.get}")
    injectCounter.incrementAndGet()
  }

  def init = {
    info(s"started: ${start.get()}")
    start.incrementAndGet()
  }

  def stop {
    info(s"stop: ${shutdown.get}")
    shutdown.incrementAndGet()
  }
}

trait CounterUser {
  val counter1 = bind[CounterService]
  val counter2 = bind[CounterService]
}

trait CounterService {
  val counter = bind[Counter]
                .onInjection(_.inject)
                .onStart(_.init)
                .onShutdown(_.stop)

  def current = counter.current
}
trait User1 extends CounterService
trait User2 extends CounterService

/**
  *
  */
class LifeCycleManagerTest extends AirframeSpec {
  "LifeCycleManager" should {
    "call init hook only once for singleton" in {
      val c = newDesign
              .bind[CounterService].toSingleton
              .newSession
              .build[CounterService]
      c.counter.current shouldBe 1

      val multiCounter = newDesign
                         .bind[CounterService].toSingleton
                         .newSession
                         .build[CounterUser]

      multiCounter.counter1.current shouldBe 1
      multiCounter.counter2.current shouldBe 1
      multiCounter.counter1.hashCode shouldBe multiCounter.counter2.hashCode
    }

    "start and shutdown only once for singleton referenced multiple times" taggedAs("multi") in {
      val session = newDesign
                    .bind[Counter].toSingleton
                    .newSession

      val u1 = session.build[User1]
      val u2 = session.build[User2]

      u1.counter.hashCode shouldBe u2.counter.hashCode()

      session.start

      session.shutdown

      // Counter is initialized only once
      u1.counter.startCount shouldBe 1
      u2.counter.startCount shouldBe 1

      // Counter should be injected twice
      u1.counter.current shouldBe 2
      u2.counter.current shouldBe 2

      // But only single shutdown should be called
      u1.counter.shutdownCount shouldBe 1
      u2.counter.shutdownCount shouldBe 1
    }
  }
}
