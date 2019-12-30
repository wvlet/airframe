package wvlet.airframe.control

import wvlet.airspec._
import java.util.concurrent.TimeoutException

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

  def `support failure threshold`: Unit = {
    val cb = CircuitBreaker.withFailureThreshold(2, 5)
    cb.isConnected shouldBe true
    val e = new TimeoutException()
    cb.recordSuccess
    cb.isConnected shouldBe true
    cb.recordSuccess
    cb.isConnected shouldBe true
    cb.recordFailure(e)
    cb.isConnected shouldBe true
    cb.recordFailure(e)
    cb.isConnected shouldBe true
    cb.recordSuccess
    cb.isConnected shouldBe false
    cb.recordSuccess
    cb.isConnected shouldBe false
    cb.recordSuccess
    cb.isConnected shouldBe false
    cb.recordSuccess
    cb.isConnected shouldBe true
    cb.recordSuccess
    cb.isConnected shouldBe true
  }
}
