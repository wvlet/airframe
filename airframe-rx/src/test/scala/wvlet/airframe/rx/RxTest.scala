package wvlet.airframe.rx
import java.util.concurrent.atomic.{AtomicInteger, AtomicReference}

import wvlet.airspec._

class RxTest extends AirSpec {

  test("create a new Rx variable") {
    val v = Rx(1)
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

    // If we cancel the subscription, it will no longer propagate the change
    subscription.cancel

    v := 3
    updateCount.get() shouldBe 2
    value.get() shouldBe s"count: 2"
  }
}
