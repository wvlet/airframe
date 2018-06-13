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

import scala.util.Random

/**
  *
  */
object Retry {
  trait RetryWaitStrategy {
    def nextWaitMillis: Int
  }

  case class RetryConfig(initialIntervalMillis: Int = 100, maxIntervalMillis: Int = 15000, multiplier: Double = 1.5) {
    require(initialIntervalMillis >= 0)
    require(maxIntervalMillis >= 0)
    require(multiplier >= 0)
  }

  class ExponentialBackOff(retryConfig: RetryConfig) extends RetryWaitStrategy {
    private var _nextWaitMillis = retryConfig.initialIntervalMillis

    override def nextWaitMillis: Int = {
      val next = _nextWaitMillis
      _nextWaitMillis = math.round(_nextWaitMillis * retryConfig.multiplier).toInt.min(retryConfig.maxIntervalMillis)
      next
    }
  }

  class Jitter(retryConfig: RetryConfig) extends RetryWaitStrategy {
    private val rand            = new Random()
    private var _nextWaitMillis = retryConfig.initialIntervalMillis

    override def nextWaitMillis: Int = {
      val next = (_nextWaitMillis.toDouble * rand.nextDouble()).toInt
      _nextWaitMillis = (_nextWaitMillis * retryConfig.multiplier).round.toInt.min(retryConfig.maxIntervalMillis)
      next
    }
  }
}
