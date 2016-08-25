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
import javax.annotation.{PostConstruct, PreDestroy}

import wvlet.log.{LogLevel, LogSupport, Logger}

class Counter extends LogSupport {
  private val counter = new AtomicInteger(0)
  private val shutdown = new AtomicInteger(0)
  def current: Int = counter.get()
  def shutdownCount : Int = shutdown.get

  @PostConstruct
  def init {
    info(s"init: ${counter.get}")
    counter.incrementAndGet()
  }

  @PreDestroy
  def stop {
    info(s"stop: ${shutdown.get}")
    shutdown.incrementAndGet()
  }
}

trait CounterUser {
  val counter1 = bind[Counter]
  val counter2 = bind[Counter]
}

/**
  *
  */
class LifeCycleManagerTest extends AirframeSpec {
  "LifeCycleManager" should {
    "call init hook only once for singleton" in {
      val c = newDesign
              .bind[Counter].toSingleton
              .newSession
              .build[Counter]
      c.current shouldBe 1

      val multiCounter = newDesign
                         .bind[Counter].toSingleton
                         .newSession
                         .build[CounterUser]

      multiCounter.counter1.current shouldBe 1
      multiCounter.counter2.current shouldBe 1
      multiCounter.counter1.hashCode shouldBe multiCounter.counter2.hashCode
    }

    trait CounterService {
      val counter = bind[Counter]
    }

    trait User1 extends CounterService
    trait User2 extends CounterService

    "start and shutdown only once for singleton referenced multiple times" in {
      val session = newDesign
                    .bind[Counter].toSingleton
                    .newSession

      val u1 = session.build[User1]
      val u2 = session.build[User2]

      u1.counter.hashCode shouldBe u2.counter.hashCode()

      session.shutdown
      u1.counter.shutdownCount shouldBe 1
      u2.counter.shutdownCount shouldBe 1
    }
  }


}
