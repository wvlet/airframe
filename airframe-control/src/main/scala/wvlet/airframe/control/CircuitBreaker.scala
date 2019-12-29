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

import wvlet.airframe.control.CircuitBreaker.CircuitBreakerState
import wvlet.airframe.control.ResultClass.{Failed, Succeeded}
import wvlet.airframe.control.Retry.RetryableFailure

import scala.util.{Failure, Success, Try}

/**
  *
  */
object CircuitBreaker {

  def apply(): CircuitBreaker = new CircuitBreaker("circuit-breaker")

  sealed trait CircuitBreakerState
  case object OPEN      extends CircuitBreakerState
  case object HALF_OPEN extends CircuitBreakerState
  case object CLOSED    extends CircuitBreakerState

  private def throwOpenException: CircuitBreakerContext => Unit = { ctx: CircuitBreakerContext =>
    throw CircuitBreakerOpenException(ctx)
  }

  case class CircuitBreakerOpenException(context: CircuitBreakerContext) extends Exception
}

import CircuitBreaker._

trait CircuitBreakerContext {
  def name: String
  def state: CircuitBreakerState
  def lastFailure: Option[Throwable]
}

case class CircuitBreaker(
    // The name of circuit
    name: String,
    lastFailure: Option[Throwable] = None,
    healthCheckPolicy: HealthCheckPolicy,
    resultClassifier: Any => ResultClass = ResultClass.ALWAYS_SUCCEED,
    errorClassifier: Throwable => ResultClass = ResultClass.ALWAYS_RETRY,
    onOpen: CircuitBreakerContext => Unit = CircuitBreaker.throwOpenException,
    onStateChange: CircuitBreakerContext => Unit,
    private val currentState: AtomicReference[CircuitBreakerState] =
      new AtomicReference[CircuitBreakerState](CircuitBreaker.CLOSED)
) extends CircuitBreakerContext {

  def state: CircuitBreakerState = currentState.get()

  def withName(name: String): CircuitBreaker = {
    this.copy(name = name)
  }
  def withHealthCheckPolicy(healthCheckPolicy: HealthCheckPolicy): CircuitBreaker = {
    this.copy(healthCheckPolicy = healthCheckPolicy)
  }

  def open: this.type = {
    currentState.set(OPEN)
    this
  }
  def halfOpen: this.type = {
    currentState.set(HALF_OPEN)
    this
  }
  def close: this.type = {
    currentState.set(CLOSED)
    this
  }

  def isConnected: Boolean = {
    currentState.get() == CLOSED
  }

  def run[A](body: => A): Unit = {
    if (!isConnected) {
      onOpen(this)
    } else {
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
          healthCheckPolicy.recovered
          currentState.get() match {
            case HALF_OPEN | CLOSED =>
              healthCheckPolicy.recovered
              close
            case _ =>
          }
          healthCheckPolicy.recordSuccess
          result.get
        case Failed(retryable, cause, extraWait) =>
          healthCheckPolicy.recordFailure
          throw cause
      }
    }
  }
}
