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

import java.util.concurrent.atomic.AtomicLong

import wvlet.airframe.control.util.ExponentialMovingAverage

/**
  *
  */
trait HealthCheckPolicy {
  def isAlive: Boolean = !isMarkedDead
  def isMarkedDead: Boolean

  /**
    * Called when a request succeeds
    */
  def recordSuccess: Unit

  /**
    * Called when request is failed.
    */
  def recordFailure: Unit

  /**
    * Called when the target service is recovered
    */
  def recovered: Unit
}

object HealthCheckPolicy {

  /**
    * A policy for marking the service dead upon consecutive failures
    */
  def markDeadOnConsecutiveFailures(numFailures: Int): HealthCheckPolicy =
    new HealthCheckPolicy {
      private val consecutiveFailures = new AtomicLong(0L)

      override def isMarkedDead: Boolean = consecutiveFailures.get() >= numFailures

      override def recordSuccess: Unit = {
        consecutiveFailures.set(0)
      }

      /**
        * Called when request is failed.
        * Returns delay
        */
      override def recordFailure: Unit = {
        consecutiveFailures.incrementAndGet()
      }

      /**
        * Called when the target service is recovered
        */
      override def recovered: Unit = {
        consecutiveFailures.set(0)
      }
    }

  def markDeadOnRecentFailureRate(failureRate: Double, timeWindowMillis: Long): HealthCheckPolicy =
    new HealthCheckPolicy {
      private val failureRateEMA = new ExponentialMovingAverage(timeWindowMillis)

      override def isMarkedDead: Boolean = {
        failureRateEMA.last > failureRate
      }

      /**
        * Called when a request succeeds
        */
      override def recordSuccess: Unit = {
        failureRateEMA.update(System.currentTimeMillis(), 0)
      }

      /**
        * Called when request is failed.
        */
      override def recordFailure: Unit = {
        failureRateEMA.update(System.currentTimeMillis(), 1)
      }

      /**
        * Called when the target service is recovered
        */
      override def recovered: Unit = {
        failureRateEMA.reset()
      }
    }
}
