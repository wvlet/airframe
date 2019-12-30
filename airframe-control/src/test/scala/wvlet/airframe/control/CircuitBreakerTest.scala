package wvlet.airframe.control

import wvlet.airspec._

class CircuitBreakerTest extends AirSpec {

  def `support changing states`: Unit = {
    val cb = CircuitBreaker.default
    cb.state shouldBe CircuitBreaker.CLOSED
    cb.isConnected shouldBe true

    cb.open
    cb.state shouldBe CircuitBreaker.OPEN
    cb.isConnected shouldBe false

    cb.halfOpen
    cb.state shouldBe CircuitBreaker.HALF_OPEN
    cb.isConnected shouldBe false

    cb.close
    cb.state shouldBe CircuitBreaker.CLOSED
    cb.isConnected shouldBe true
  }
}
