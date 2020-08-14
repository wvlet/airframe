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
import java.util.concurrent.atomic.{AtomicBoolean, AtomicInteger, AtomicReference}

import wvlet.airspec.AirSpec

import scala.concurrent.Future
import scala.util.{Failure, Success}

/**
  */
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

  test("force update RxVar") {

    val v     = Rx.variable(1)
    val count = new AtomicInteger(0)
    v.subscribe(newValue => count.incrementAndGet())

    v.get shouldBe 1
    count.get() shouldBe 1
    v.set(1)
    v.get shouldBe 1
    count.get() shouldBe 1

    v.forceSet(1)
    v.get shouldBe 1
    count.get() shouldBe 2

    v.forceUpdate(x => x * 2)
    v.get shouldBe 2
    count.get() shouldBe 3

    v.forceUpdate(x => x)
    v.get shouldBe 2
    count.get() shouldBe 4

    v.update(x => x)
    v.get shouldBe 2
    count.get() shouldBe 4
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

  test("zip") {
    val a = Rx.variable(1)
    val b = Rx.variable("a")

    val x     = a.zip(b)
    var count = 0

    val c = x.run { v =>
      count match {
        case 0 =>
          v shouldBe (1, "a")
        case 1 =>
          v shouldBe (2, "a")
        case 2 =>
          v shouldBe (2, "b")
        case _ =>
      }
    }

    count += 1
    a := 2
    count += 1
    b := "b"
    c.cancel
  }

  test("zip3") {
    val a = Rx.variable(1)
    val b = Rx.variable("a")
    val c = Rx.variable(true)

    val x     = a.zip(b, c)
    var count = 0

    val e = x.run { v =>
      count match {
        case 0 =>
          v shouldBe (1, "a", true)
        case 1 =>
          v shouldBe (2, "a", true)
        case 2 =>
          v shouldBe (2, "b", true)
        case 3 =>
          v shouldBe (2, "b", false)
        case _ =>
      }
    }

    count += 1
    a := 2
    count += 1
    b := "b"
    count += 1
    c := false
    e.cancel
  }

  implicit val ec: scala.concurrent.ExecutionContext = scala.concurrent.ExecutionContext.global

  test("from Future[X]") {
    val f  = Future.successful(1)
    val rx = f.toRx

    pending("requires async test")
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

  test("lastOption") {
    val rx      = Rx.sequence(1, 2, 3).lastOption
    var counter = 0
    rx.run { x =>
      counter += 1
      x shouldBe 3
    }
    counter shouldBe 1
  }

  test("lastOption empty") {
    val rx      = Rx.empty[Int].lastOption
    var counter = 0
    rx.run { x =>
      counter += 1
    }
    counter shouldBe 0
  }

  test("concat") {
    val rx = Rx.single(1).concat(Rx.single(2)).map(_ * 2)
    val b  = Seq.newBuilder[Int]
    rx.run {
      b += _
    }
    b.result() shouldBe Seq(2, 4)
  }

  test("sequence") {
    val rx = Rx.fromSeq(Seq(1, 2, 3)).map(_ * 2)
    val b  = Seq.newBuilder[Int]
    rx.run(b += _)
    b.result() shouldBe Seq(2, 4, 6)
  }

  test("empty") {
    val rx = Rx.empty[Int].map(_ * 2)
    val b  = Seq.newBuilder[Int]
    rx.run(b += _)
    b.result() shouldBe Seq.empty
  }

  test("recover from an error") {
    def recoveryFunction: PartialFunction[Throwable, Any] = {
      case e: IllegalArgumentException => 0
    }

    def newTests(rx: Rx[Int]): Seq[(Rx[Any], Any)] =
      Seq(
        (rx, 1),
        (rx.map(x => x * 2), 2),
        (rx.flatMap(x => Rx.single(3)), 3),
        (rx.filter(_ => true), 1),
        (rx.zip(Rx.single(1)), (1, 1))
      ).map { x =>
        (x._1.recover(recoveryFunction), x._2)
      }

    test("normal behavior") {
      for ((t, expected) <- newTests(Rx.single(1))) {
        var executed = false
        t.run { x =>
          executed = true
          x shouldBe expected
        }
        executed shouldBe true
      }
    }

    test("failure recovery") {
      for ((t, expected) <- newTests(Rx.single(throw new IllegalArgumentException("test failure")))) {
        var executed = false
        t.run { x =>
          executed = true
          x shouldBe 0
        }
        executed shouldBe true
      }
    }
  }

}
