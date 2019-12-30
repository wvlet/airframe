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

  def `support standalone usage`: Unit = {
    val cb = CircuitBreaker.default

    cb.verifyConnection
    try {
      cb.recordSuccess
    } catch {
      case e: Throwable =>
        cb.recordFailure(e)
    }

    // Capture the open exception
    intercept[CircuitBreakerOpenException] {
      cb.open
      cb.verifyConnection
    }
  }
}
