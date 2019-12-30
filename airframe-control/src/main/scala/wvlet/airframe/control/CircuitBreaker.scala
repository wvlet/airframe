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
package wvlet.airframe.control

import java.util.concurrent.atomic.AtomicReference

import wvlet.airframe.control.ResultClass.{Failed, Succeeded}
import wvlet.airframe.control.Retry.RetryableFailure

import scala.util.{Failure, Success, Try}
import wvlet.log.LogSupport

case class CircuitBreakerConfig(
    name: String = "default",
    healthCheckPolicy: HealthCheckPolicy = HealthCheckPolicy.markDeadOnConsecutiveFailures(3),
    resultClassifier: Any => ResultClass = ResultClass.ALWAYS_SUCCEED,
    errorClassifier: Throwable => ResultClass = ResultClass.ALWAYS_RETRY,
    onOpen: CircuitBreakerContext => Unit = CircuitBreaker.throwOpenException,
    onStateChange: CircuitBreakerContext => Unit = CircuitBreaker.reportStateChange
) {
  def withName(name: String): CircuitBreakerConfig = {
    this.copy(name = name)
  }
  def withHealthCheckPolicy(healthCheckPolicy: HealthCheckPolicy): CircuitBreakerConfig = {
    this.copy(healthCheckPolicy = healthCheckPolicy)
  }
}

/**
  *
  */
object CircuitBreaker extends LogSupport {

  /**
    * Thrown when the circuit breaker is open
    */
  case class CircuitBreakerOpenException(context: CircuitBreakerContext) extends Exception

  def default: CircuitBreakerConfig = CircuitBreakerConfig()
  def apply(config: CircuitBreakerConfig): CircuitBreaker = {
    new CircuitBreaker(config = config)
  }

  sealed trait CircuitBreakerState
  case object OPEN      extends CircuitBreakerState
  case object HALF_OPEN extends CircuitBreakerState
  case object CLOSED    extends CircuitBreakerState

  private[control] def throwOpenException: CircuitBreakerContext => Unit = { ctx: CircuitBreakerContext =>
    throw CircuitBreakerOpenException(ctx)
  }

  private[control] def reportStateChange = { ctx: CircuitBreakerContext =>
    info(s"CircuitBreaker(namd:${ctx.name}) state is changed to ${ctx.state}")
  }
}

import CircuitBreaker._

trait CircuitBreakerContext {
  def name: String
  def state: CircuitBreakerState
  def lastFailure: Option[Throwable]
}

case class CircuitBreaker(
    // The name of circuit
    config: CircuitBreakerConfig,
    lastFailure: Option[Throwable] = None,
    private val currentState: AtomicReference[CircuitBreakerState] = new AtomicReference(CircuitBreaker.CLOSED)
) extends CircuitBreakerContext {

  def name: String               = config.name
  def state: CircuitBreakerState = currentState.get()

  def open: this.type = {
    currentState.set(OPEN)
    config.onStateChange(this)
    this
  }
  def halfOpen: this.type = {
    currentState.set(HALF_OPEN)
    config.onStateChange(this)
    this
  }
  def close: this.type = {
    currentState.set(CLOSED)
    config.onStateChange(this)
    this
  }

  def isConnected: Boolean = {
    currentState.get() == CLOSED
  }

  def run[A](body: => A): Unit = {
    if (!isConnected) {
      config.onOpen(this)
    } else {
      val result = Try(body)
      val resultClass = result match {
        case Success(x) => config.resultClassifier(x)
        case Failure(RetryableFailure(e)) =>
          ResultClass.retryableFailure(e)
        case Failure(e) =>
          config.errorClassifier(e)
      }
      resultClass match {
        case Succeeded =>
          config.healthCheckPolicy.recovered
          currentState.get() match {
            case HALF_OPEN | CLOSED =>
              config.healthCheckPolicy.recovered
              close
            case _ =>
          }
          config.healthCheckPolicy.recordSuccess
          result.get
        case Failed(retryable, cause, extraWait) =>
          config.healthCheckPolicy.recordFailure
          throw cause
      }
    }
  }
}
