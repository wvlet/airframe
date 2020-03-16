package wvlet.airframe.rx
import java.util.concurrent.atomic.{AtomicInteger, AtomicReference}

import wvlet.airframe.Design
import wvlet.airframe.rx.html.Embedded
import wvlet.airspec._

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
    val v  = Rx.of(2)
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

  test("embedded") {
    val em = Embedded("text")
    intercept[Throwable] {
      // No implementation
      em.render
    }
  }
}
