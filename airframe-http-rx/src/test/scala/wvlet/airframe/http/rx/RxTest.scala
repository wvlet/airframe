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
package wvlet.airframe.http.rx

import java.util.concurrent.atomic.{AtomicInteger, AtomicReference}

import wvlet.airframe.Design
import wvlet.airframe.http.rx.html.Embedded
import wvlet.airspec._

import scala.concurrent.Future

object RxTest extends AirSpec {

  test("create a new Rx variable") {
    val v = Rx(1)
    v.toString
    v.get shouldBe 1

    val rx: Rx[String] = v.map(x => s"count: ${x}").withName("sample rx")
    val updateCount    = new AtomicInteger(0)
    val value          = new AtomicReference[String]()
    val subscription = rx.subscribe { x: String =>
      updateCount.incrementAndGet()
      value.set(x)
    }
    // The subscription must be executed upon the registration
    updateCount.get() shouldBe 1
    value.get() shouldBe s"count: 1"

    // Propagate changes
    v := 2
    updateCount.get() shouldBe 2
    value.get() shouldBe s"count: 2"

    // If the value is unchanged, it should not propergate update
    v := 2
    updateCount.get() shouldBe 2
    value.get() shouldBe s"count: 2"

    // If we cancel the subscription, it will no longer propagate the change
    subscription.cancel

    v := 3
    updateCount.get() shouldBe 2
    value.get() shouldBe s"count: 2"
  }

  test("rx variable") {
    val v = Rx.variable(1)
    v.toString
    v.parents shouldBe empty
    v.get shouldBe 1
    v := 2
    v.get shouldBe 2
  }

  test("chain Rx operators") {
    val v  = Rx.const(2)
    val v1 = v.map(_ + 1)
    val v2 = v1.flatMap(i => Rx(i * 2))
    val op = v2.withName("multiply")

    // Parents
    v.parents shouldBe empty
    v1.parents.find(_ eq v) shouldBe defined
    v2.parents.find(_ eq v1) shouldBe defined
    op.parents.find(_ eq v2) shouldBe defined

    // toString sanity test
    v.toString
    v1.toString
    v2.toString
    op.toString

    // Run chain
    val c1 = v.run { v => v shouldBe 2 }
    c1.cancel
    val c2 = v1.run { v => v shouldBe 3 }
    c2.cancel
    val c3 = v2.run { v => v shouldBe 6 }
    c3.cancel
    val c4 = op.run { v => v shouldBe 6 }
    c4.cancel
  }

  test("filter") {
    val v = Rx.const(1)
    v.filter(_ == 1).run { v => v shouldBe 1 }
    v.filter(_ != 1).run { v => fail("cannot reach here") }
  }

  test("filter with Option[X]") {
    val v = Rx.const(Some(10))
    v.filter(_.isDefined).map(_.get).run { x => x shouldBe 10 }

    val n = Rx.const[Option[Int]](None)
    n.filter(_.isDefined).map(_.get).run { x => fail("cannot reach here") }
  }

  test("for-comprehension") {
    val v = for (x <- Rx.const(1) if x == 1) yield x
    v.run(x => x shouldBe 1)

    val n = for (x <- Rx.const(1) if x != 1) yield x
    n.run(x => fail("cannot reach here"))
  }

  test("embedded") {
    val em = Embedded("text")
    intercept[Throwable] {
      // No implementation
      em.render
    }
  }

  implicit val ec: scala.concurrent.ExecutionContext = scala.concurrent.ExecutionContext.global

  test("from Future[X]") {
    val f  = Future.successful(1)
    val rx = f.toRx

    pending("requries async test")
    rx.run(x => x shouldBe Some(1))
  }

  test("from Future[Exception]") {
    val fe: Future[Exception] = Future.failed(new IllegalArgumentException)

    val rx = fe.toRx

    pending("requires async test")
    rx.run { x: Option[Exception] =>
      x match {
        case None => // ok
        case _    => fail()
      }
    }
  }

}
