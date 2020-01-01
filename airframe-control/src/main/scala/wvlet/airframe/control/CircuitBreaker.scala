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
import wvlet.airframe.control.Retry.RetryPolicy
import wvlet.airframe.control.Retry.RetryPolicyConfig
import wvlet.airframe.control.Retry.Jitter

/**
  * An exception thrown when the circuit breaker is open.
  */
case class CircuitBreakerOpenException(context: CircuitBreakerContext) extends Exception

sealed trait CircuitBreakerState

/**
  *
  */
object CircuitBreaker extends LogSupport {

  case object OPEN      extends CircuitBreakerState
  case object HALF_OPEN extends CircuitBreakerState
  case object CLOSED    extends CircuitBreakerState

  def default: CircuitBreaker                         = new CircuitBreaker()
  def newCircuitBreaker(name: String): CircuitBreaker = new CircuitBreaker().withName(name)

  /**
    * Create a CircuitBreaker that will be open after observing numFailures out of numExecutions.
    */
  def withFailureThreshold(numFailures: Int, numExecutions: Int = 10): CircuitBreaker = {
    default.withHealthCheckPolicy(HealthCheckPolicy.markDeadOnFailureThreshold(numFailures, numExecutions))
  }

  /**
    * Create a CircuitBreaker that will be open if the failure rate in a time window exceeds the given threshold.
    * The failure rate will be decayed exponentially as time goes.
    */
  def withFailureRate(failureRate: Double, timeWindowMillis: Int = 60000): CircuitBreaker = {
    default.withHealthCheckPolicy(HealthCheckPolicy.markDeadOnRecentFailureRate(failureRate, timeWindowMillis))
  }

  /**
    * Create a CircuitBreaker that will be open if the number of consecutive failrues excceeds the given threshold.
    *
    */
  def withConsecutiveFailures(numFailures: Int): CircuitBreaker = {
    default.withHealthCheckPolicy(HealthCheckPolicy.markDeadOnConsecutiveFailures(numFailures))
  }

  private[control] def throwOpenException: CircuitBreakerContext => Unit = { ctx: CircuitBreakerContext =>
    throw CircuitBreakerOpenException(ctx)
  }

  private[control] def reportStateChange = { ctx: CircuitBreakerContext =>
    info(s"CircuitBreaker(name:${ctx.name}) is changed to ${ctx.state}")
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
    delayAfterMarkedDead: RetryPolicy = new Jitter(new RetryPolicyConfig(initialIntervalMillis = 30000)), // 30 seconds
    private var nextProvingTimeMillis: Long = Long.MaxValue,
    private var provingWaitTimeMillis: Long = 0L,
    var lastFailure: Option[Throwable] = None,
    private val currentState: AtomicReference[CircuitBreakerState] = new AtomicReference(CircuitBreaker.CLOSED)
) extends CircuitBreakerContext
    with LogSupport {
  def state: CircuitBreakerState = currentState.get()

  /**
    * Set the name of this CircuitBreaker
    */
  def withName(newName: String): CircuitBreaker = {
    this.copy(name = newName)
  }

  /**
    * Set a health check policy, which will be used to determine the state of the target service.
    */
  def withHealthCheckPolicy(newHealthCheckPolicy: HealthCheckPolicy): CircuitBreaker = {
    this.copy(healthCheckPolicy = newHealthCheckPolicy)
  }

  /**
    * Set a classifier to determine whether the execution result of the code block is successful or not.
    */
  def withResultClassifier(newResultClassifier: Any => ResultClass): CircuitBreaker = {
    this.copy(resultClassifier = newResultClassifier)
  }

  /**
    * Set a classifier to determine whether the exception happend in the code block can be ignoreable (Successful) or not for
    * the accessing the target service.
    */
  def withErrorClassifier(newErrorClassifier: Throwable => ResultClass): CircuitBreaker = {
    this.copy(errorClassifier = newErrorClassifier)
  }

  /**
    * Set a delay policy until moving the state from OPEN to HALF_OPEN (probing) state.
    * The default is Jittered-exponential backoff delay with the initial interval of 30 seconds.
    */
  def withDelayAfterMarkedDead(retryPolicy: RetryPolicy): CircuitBreaker = {
    this.copy(delayAfterMarkedDead = retryPolicy)
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
    nextProvingTimeMillis = Long.MaxValue
    provingWaitTimeMillis = 0L
    healthCheckPolicy.recovered
  }

  /**
    * Force setting the current state.
    */
  def setState(newState: CircuitBreakerState): this.type = {
    if (currentState.get() != newState) {
      currentState.set(newState)
      onStateChangeListener(this)
    }
    this
  }

  def open: this.type     = setState(OPEN)
  def halfOpen: this.type = setState(HALF_OPEN)
  def close: this.type    = setState(CLOSED)

  /**
    * Returns true when the circuit can execute the code ( OPEN or HALF_OPEN state)
    */
  def isConnected: Boolean = {
    val s = currentState.get()
    s == CLOSED || s == HALF_OPEN
  }

  /**
    * Note: Use this method only for the standalone mode. Generally, using CircuiteBreaker.run is sufficient.
    *
    * If the connection is open, perform the specified action. The
    * default behavior is fail-fast, i.e., throwing CircuitBreakerOpenException
    */
  def verifyConnection: Unit = {
    if (!isConnected) {
      val currentTime = System.currentTimeMillis()
      if (currentTime > nextProvingTimeMillis) {
        halfOpen
      } else {
        onOpenHandler(this)
      }
    }
  }

  /**
    * Note: Use this method only for the standalone mode. Generally, using CircuitBreaker.run is sufficient.
    *
    * This method reports a successful state to the CircuitBreaker.
    */
  def recordSuccess: Unit = {
    healthCheckPolicy.recordSuccess
    val isDead = healthCheckPolicy.isMarkedDead
    currentState.get() match {
      case HALF_OPEN =>
        // Probe request succeeds, so move to CLOSED state
        healthCheckPolicy.recovered
        close
      case CLOSED if isDead =>
        open
      case OPEN if !isDead =>
        // Service is not marked dead, so try proving at HALF_OPEN state
        halfOpen
      case _ =>
    }
  }

  /**
    * Note: Use this method only for the standalone mode. Generally, using CircuitBreaker.run is sufficient.
    *
    * This method reports a failure state to the CircuitBreaker.
    */
  def recordFailure(e: Throwable): Unit = {
    lastFailure = Some(e)
    healthCheckPolicy.recordFailure
    if (healthCheckPolicy.isMarkedDead) {
      val baseWaitMillis = provingWaitTimeMillis.max(delayAfterMarkedDead.retryPolicyConfig.initialIntervalMillis).toInt
      val nextWaitMillis = delayAfterMarkedDead.nextWait(baseWaitMillis)
      provingWaitTimeMillis = delayAfterMarkedDead.updateBaseWait(baseWaitMillis)
      nextProvingTimeMillis = System.currentTimeMillis() + nextWaitMillis
      open
    }
  }

  /**
    * Execute the body block through the CircuitBreaker.
    *
    * If the state is OPEN, this will throw CircuitBreakerOpenException (fail-fast). The state will move to HALF_OPEN state
    * after a cetain amount of delay, determined by the delayAfterMarkedDead policy.
    *
    * If the state is HALF_OPEN, this method allows running the code block once, and if the result is successful,
    * the state will move to CLOSED. If not, the state will be OPEN again.
    *
    * If the state is CLOSED, the code block will be executed normally. If the result is marked failure or nonRetryable exception
    * is thrown, it will report to the failure to the HealthCheckPolicy. If this policy determins the target service is dead,
    * the circuit will shift to OPEN state to block the future execution.
    *
    */
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
