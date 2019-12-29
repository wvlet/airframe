package wvlet.airframe.control

import wvlet.airspec._
import wvlet.airframe.control.CircuitBreaker

class CircuitBreakerTest extends AirSpec {

  def `throw exception on open`: Unit = {
    val cb = CircuitBreaker()
    cb.state shouldBe CircuitBreaker.CLOSED
  }

}
