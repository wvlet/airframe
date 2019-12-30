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

/**
  * Thrown when the circuit breaker is open
  */
case class CircuitBreakerOpenException(context: CircuitBreakerContext) extends Exception

/**
  *
  */
object CircuitBreaker extends LogSupport {
  sealed trait CircuitBreakerState
  case object OPEN      extends CircuitBreakerState
  case object HALF_OPEN extends CircuitBreakerState
  case object CLOSED    extends CircuitBreakerState

  def default: CircuitBreaker = new CircuitBreaker()
  def withFailureThreshold(numFailures: Int, numExecutions: Int = 10): CircuitBreaker = {
    default.withHealthCheckPolicy(HealthCheckPolicy.markDeadOnFailureThreshold(numFailures, numExecutions))
  }

  private[control] def throwOpenException: CircuitBreakerContext => Unit = { ctx: CircuitBreakerContext =>
    throw CircuitBreakerOpenException(ctx)
  }

  private[control] def reportStateChange = { ctx: CircuitBreakerContext =>
    info(s"CircuitBreaker(name:${ctx.name}) state is changed to ${ctx.state}")
  }
}

import CircuitBreaker._

/**
  * A safe interface for accessing CircuitBreaker states when handling events.
  */
trait CircuitBreakerContext {
  def name: String
  def state: CircuitBreakerState
  def lastFailure: Option[Throwable]
}

case class CircuitBreaker(
    name: String = "default",
    healthCheckPolicy: HealthCheckPolicy = HealthCheckPolicy.markDeadOnConsecutiveFailures(3),
    resultClassifier: Any => ResultClass = ResultClass.ALWAYS_SUCCEED,
    errorClassifier: Throwable => ResultClass = ResultClass.ALWAYS_RETRY,
    onOpenHandler: CircuitBreakerContext => Unit = CircuitBreaker.throwOpenException,
    onStateChangeListener: CircuitBreakerContext => Unit = CircuitBreaker.reportStateChange,
    var lastFailure: Option[Throwable] = None,
    private val currentState: AtomicReference[CircuitBreakerState] = new AtomicReference(CircuitBreaker.CLOSED)
) extends CircuitBreakerContext {
  def state: CircuitBreakerState = currentState.get()

  def withName(newName: String): CircuitBreaker = {
    this.copy(name = newName)
  }
  def withHealthCheckPolicy(newHealthCheckPolicy: HealthCheckPolicy): CircuitBreaker = {
    this.copy(healthCheckPolicy = newHealthCheckPolicy)
  }
  def withResultClassifier(newResultClassifier: Any => ResultClass): CircuitBreaker = {
    this.copy(resultClassifier = newResultClassifier)
  }
  def withErrorClassifier(newErrorClassifier: Throwable => ResultClass): CircuitBreaker = {
    this.copy(errorClassifier = newErrorClassifier)
  }

  /**
    * Set an event listner that monitors CircuitBreaker state changes
    */
  def onStateChange(listener: CircuitBreakerContext => Unit): CircuitBreaker = {
    this.copy(onStateChangeListener = listener)
  }

  /**
    * Defines the action when trying to use the open circuit. The default
    * behavior is to throw CircuitBreakerOpenException
    */
  def onOpen(handler: CircuitBreakerContext => Unit): CircuitBreaker = {
    this.copy(onOpenHandler = handler)
  }

  /**
    * Reset the lastFailure and close the circuit
    */
  def reset: Unit = {
    lastFailure = None
    currentState.set(CLOSED)
  }

  def setState(newState: CircuitBreakerState): this.type = {
    currentState.set(newState)
    onStateChangeListener(this)
    this
  }

  def open: this.type     = setState(OPEN)
  def halfOpen: this.type = setState(HALF_OPEN)
  def close: this.type    = setState(CLOSED)

  def isConnected: Boolean = {
    currentState.get() == CLOSED && !healthCheckPolicy.isMarkedDead
  }

  /**
    * If the connection is open, perform the specified action. The
    * default behavior is throwing CircuitBreakerOpenException
    */
  def verifyConnection: Unit = {
    if (healthCheckPolicy.isMarkedDead) {
      open
    }

    if (!isConnected) {
      onOpenHandler(this)
    }
  }

  def recordSuccess: Unit = {
    healthCheckPolicy.recovered
    currentState.get() match {
      case HALF_OPEN =>
        healthCheckPolicy.recovered
        close
      case _ =>
    }
    healthCheckPolicy.recordSuccess
  }

  def recordFailure(e: Throwable): Unit = {
    lastFailure = Some(e)
    healthCheckPolicy.recordFailure
  }

  def run[A](body: => A): Unit = {
    verifyConnection

    val result = Try(body)
    val resultClass = result match {
      case Success(x) => resultClassifier(x)
      case Failure(RetryableFailure(e)) =>
        ResultClass.retryableFailure(e)
      case Failure(e) =>
        errorClassifier(e)
    }
    resultClass match {
      case Succeeded =>
        recordSuccess
        result.get
      case Failed(retryable, cause, extraWait) =>
        recordFailure(cause)
        throw cause
    }
  }
}
