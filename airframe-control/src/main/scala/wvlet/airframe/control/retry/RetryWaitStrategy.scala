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
package wvlet.airframe.control.retry

import scala.util.Random

case class RetryWait(retryCount: Int, nextIntervalMillis: Int)

trait RetryWaitStrategy {
  def nextIntervalMillis: RetryWait
}

abstract class RetryWaitStrategyBase(protected val retryInterval: RetryInterval) extends RetryWaitStrategy {
  private var _retryCount: Int = 0

  protected def nextWait: Int
  override def nextIntervalMillis: RetryWait = {
    val next = nextWait
    _retryCount += 1
    RetryWait(next, _retryCount)
  }
}

class ExponentialBackOff(retryInterval: RetryInterval) extends RetryWaitStrategyBase(retryInterval) {
  private var currentRetryInterval: Int = retryInterval.initialIntervalMillis

  override def nextWait: Int = {
    val current = currentRetryInterval
    currentRetryInterval = math.round(current * retryInterval.multiplier).toInt.min(retryInterval.maxIntervalMillis)
    current
  }
}

class Jitter(retryInterval: RetryInterval) extends RetryWaitStrategyBase(retryInterval) {
  private var currentRetryInterval: Int = retryInterval.initialIntervalMillis
  private val rand                      = new Random()

  override def nextWait: Int = {
    val current = currentRetryInterval
    currentRetryInterval = (current * retryInterval.multiplier).round.toInt.min(retryInterval.maxIntervalMillis)
    (current.toDouble * rand.nextDouble()).round.toInt
  }
}
