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
import wvlet.airspec.AirSpec
import wvlet.log.{LogLevel, LogSupport, Logger}

object SingletonTest {
  type TraitCounter = AtomicInteger

  // This doesn't tell about Singleton
  trait X extends LogSupport {
    debug("new X is instantiated")

    val counter = bind[TraitCounter].onInit { c =>
      val v = c.incrementAndGet()
      debug(s"Counter is initialized: ${v}")
    }
  }

  trait A {
    val t = bind[X]
  }

  trait B {
    val t = bind[X]
  }

  trait SingletonService {
    val service = bind[X]
  }

  trait U1 extends SingletonService
  trait U2 extends SingletonService

  trait NonAbstract extends LogSupport {
    def hello: String = "hello"
  }

  trait C extends NonAbstract {
    override def hello = "nice"
  }

  trait E extends LogSupport {
    val m = bind[NonAbstract]
  }
}

/**
  */
class SingletonTest extends AirSpec {
  scalaJsSupport

  val d =
    newDesign
      .bind[TraitCounter].toInstance(new AtomicInteger(0))

  def `bind singleton with bind[X]` : Unit = {
    val session = d.newSession

    val a = session.build[A]
    val b = session.build[B]

    a.t.counter shouldBeTheSameInstanceAs b.t.counter
    session.build[TraitCounter].get() shouldBe 1
  }

  def `bind singleton with bind[X] as a service`: Unit = {
    val session = d.newSession

    val u1 = session.build[U1]
    val u2 = session.build[U2]

    u1.service.counter shouldBeTheSameInstanceAs u2.service.counter
    u1.service.counter.get() shouldBe 1
  }

  def `support overriding non-abstract singleton trait`: Unit = {
    val d = newDesign
      .bind[E].toSingleton
      .bind[NonAbstract].toSingletonOf[C]

    val session = d.newSession
    val e       = session.build[E]
    e.m.hello shouldBe "nice"
  }
}
