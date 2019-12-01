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

  sealed trait CircuitBreakerState
  case object OPEN      extends CircuitBreakerState
  case object HALF_OPEN extends CircuitBreakerState
  case object CLOSED    extends CircuitBreakerState
}

import CircuitBreaker._

case class CircuitBreaker(
    // The name of circuit
    name: String,
    healthCheckPolicy: HealthCheckPolicy,
    resultClassifier: Any => ResultClass = ResultClass.ALWAYS_SUCCEED,
    errorClassifier: Throwable => ResultClass = ResultClass.ALWAYS_RETRY,
    private var state: AtomicReference[CircuitBreakerState] = new AtomicReference[CircuitBreakerState](CLOSED)
) {

  def open: this.type = {
    state.set(OPEN)
    this
  }
  def halfOpen: this.type = {
    state.set(HALF_OPEN)
    this
  }
  def close: this.type = {
    state.set(CLOSED)
    this
  }

  def isConnected: Boolean = {
    state.get() == CLOSED
  }

  def run[A](body: => A): A = {
    if (isConnected) {
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
          state.get() match {
            case HALF_OPEN | CLOSED =>
              open
            case _ =>
          }
          healthCheckPolicy.recordSuccess
          result.get
        case Failed(retryable, cause, extraWait) =>
          healthCheckPolicy.recordFailure

      }
    }

  }
}
