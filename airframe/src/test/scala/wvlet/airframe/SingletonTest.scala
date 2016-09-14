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

import wvlet.airframe.SingletonTest._
import wvlet.log.{LogLevel, LogSupport, Logger}
import wvlet.obj.tag.@@

object SingletonTest {

  trait TraitCounter

  // This doesn't tell about Singleton
  trait X extends LogSupport {
    info("new X is instantiated")

    val counter = bind[AtomicInteger @@ TraitCounter].withLifeCycle(
      init = { c =>
        val v = c.incrementAndGet()
        info(s"Counter is initialized: ${v}")
      }
    )
  }

  trait A {
    val t = bindSingleton[X]
  }

  trait B {
    val t = bindSingleton[X]
  }

  trait SingletonService {
    val service = bindSingleton[X]
  }

  trait U1 extends SingletonService
  trait U2 extends SingletonService
}

/**
  *
  */
class SingletonTest extends AirframeSpec {

  val design =
    newDesign
    .bind[AtomicInteger @@ TraitCounter].toInstance(new AtomicInteger(0))

  "Singleton" should {
    "support bindSingleton[X]" in {
      val session = design.newSession

      val a = session.build[A]
      val b = session.build[B]

      a.t.counter should be theSameInstanceAs b.t.counter
      session.build[AtomicInteger @@ TraitCounter].get() shouldBe 1
    }

    "support using bindSingleton[X] as a service" in {
      val session = design.newSession

      val u1 = session.build[U1]
      val u2 = session.build[U2]

      u1.service.counter should be theSameInstanceAs u2.service.counter
      u1.service.counter.get() shouldBe 1
    }
  }
}
