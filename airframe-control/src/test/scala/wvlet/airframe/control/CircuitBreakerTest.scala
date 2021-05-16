package wvlet.airframe.control

import wvlet.airspec._
import java.util.concurrent.TimeoutException

class CircuitBreakerTest extends AirSpec {

  test("support changing states") {
    val cb = CircuitBreaker.default
    cb.state shouldBe CircuitBreaker.CLOSED
    cb.isConnected shouldBe true

    cb.open
    cb.state shouldBe CircuitBreaker.OPEN
    cb.isConnected shouldBe false

    cb.halfOpen
    cb.state shouldBe CircuitBreaker.HALF_OPEN
    cb.isConnected shouldBe true

    cb.close
    cb.state shouldBe CircuitBreaker.CLOSED
    cb.isConnected shouldBe true
  }

  test("support standalone usage") {
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

  test("support failure threshold") {
    val cb = CircuitBreaker.withFailureThreshold(2, 5)
    val e  = new TimeoutException()
    cb.isConnected shouldBe true

    // 0/0
    cb.recordSuccess
    cb.isConnected shouldBe true

    // 0/1
    cb.recordSuccess
    cb.isConnected shouldBe true

    // 1/2
    cb.recordFailure(e)
    cb.isConnected shouldBe true

    // 2/3
    cb.recordFailure(e)
    cb.isConnected shouldBe true

    // 2/4
    cb.recordSuccess
    cb.isConnected shouldBe false

    // 2/5 -> open the circuit breaker
    cb.recordSuccess
    cb.isConnected shouldBe false

    // 2/5
    cb.recordSuccess
    cb.isConnected shouldBe false

    // 2/5
    cb.recordSuccess
    cb.isConnected shouldBe true

    // 1/5
    cb.recordSuccess
    cb.isConnected shouldBe true
  }

  test("support consecutive failure health checker") {
    val cb = CircuitBreaker.withConsecutiveFailures(2)
    val e  = new TimeoutException()
    cb.isConnected shouldBe true

    // 1/1
    cb.recordSuccess
    cb.isConnected shouldBe true

    // 1/2
    cb.recordFailure(e)
    cb.isConnected shouldBe true

    // 1/3
    cb.recordFailure(e)
    cb.isConnected shouldBe false

    // Force probing
    cb.halfOpen

    // 1/4
    cb.recordSuccess
    cb.isConnected shouldBe true
  }

  test("run code with circuit") {
    val cb = CircuitBreaker.withFailureThreshold(1, 2)

    cb.run {}

    intercept[TimeoutException] {
      cb.run {
        throw new TimeoutException()
      }
    }

    intercept[CircuitBreakerOpenException] {
      cb.run {}
    }
  }

  test("disable circuit breaker") {
    val cb = CircuitBreaker.alwaysClosed
    val e  = new TimeoutException()
    for (i <- 0 to 10) {
      cb.recordFailure(e)
      cb.isConnected shouldBe true
    }
  }
}
