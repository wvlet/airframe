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
import wvlet.log.LogSupport

/**
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

object HealthCheckPolicy extends LogSupport {

  /**
    * A special health check policy used for disabling circuit breaker
    */
  def alwaysHealthy: HealthCheckPolicy = new HealthCheckPolicy {
    override def isMarkedDead: Boolean = false
    override def recordSuccess: Unit = {}
    override def recordFailure: Unit = {}
    override def recovered: Unit = {}
  }

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
        * Called when request is failed. Returns delay
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
        //logger.warn(s"failure rate: ${failureRateEMA.last}, ${failureRate}")
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

  def markDeadOnFailureThreshold(numFailures: Int, numExecutions: Int) = {
    require(numExecutions > 0, s"numExecusions ${numExecutions} should be larger than 0")
    require(
      numFailures <= numExecutions,
      s"numFailures ${numFailures} should be less than numExections(${numExecutions})"
    )

    new HealthCheckPolicy {
      private val arraySize = (numExecutions + 64 - 1) / 64
      // Circular bit vector of execution hisory 0 (success) or 1 (failure)
      private val executionHistory: Array[Long] = Array.fill[Long](arraySize)(0L)
      private var executionCount: Long          = 0

      private def failureCount = {
        executionHistory.map { java.lang.Long.bitCount(_) }.sum
      }

      override def isMarkedDead: Boolean = {
        if (executionCount < numExecutions) {
          false
        } else {
          failureCount >= numFailures
        }
      }

      private def setAndMove(v: Boolean): Unit = {
        val i    = (executionCount % numExecutions).toInt
        val mask = 1L << (63 - i   % 64)
        if (v == true) {
          executionHistory(i / 64) |= mask
        } else {
          executionHistory(i / 64) &= ~mask
        }
        executionCount += 1
        if (executionCount < 0) {
          // Reset upon overflow
          executionCount = numExecutions
        }
      }

      override def recordSuccess: Unit = setAndMove(false)
      override def recordFailure: Unit = setAndMove(true)

      override def recovered: Unit = {
        executionCount = 0
      }
    }
  }
}
