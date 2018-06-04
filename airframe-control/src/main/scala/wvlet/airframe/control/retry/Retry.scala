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

/**
  *
  */
object Retry {
  def retry: Retry = Retry()
}

case class Retry(retryInterval: RetryInterval = RetryInterval(), maxRetry: Int = 3, retryWaitStrategy: RetryInterval => RetryWaitStrategy = new ExponentialBackOff(_)) {
  def withMaxRetryCount(maxRetry: Int)                                      = Retry(retryInterval, maxRetry, retryWaitStrategy)
  def withWaitStrategy(newWaitStrategy: RetryInterval => RetryWaitStrategy) = Retry(retryInterval, maxRetry, newWaitStrategy)
  def withExponentialBackOff: Retry                                         = Retry(retryInterval, maxRetry, new ExponentialBackOff(_))
  def withJitter: Retry                                                     = Retry(retryInterval, maxRetry, new Jitter(_))
  // def onException(e: Throwable => Boolean): RetryBuilder                 = RetryBuilder(retryInterval, maxRetry, retryWaitStrategy)
}

case class RetryInterval(initialIntervalMillis: Int = 1000, maxIntervalMillis: Int = 15000, multiplier: Double = 1.5) {
  require(initialIntervalMillis >= 0)
  require(maxIntervalMillis >= 0)
  require(multiplier >= 0)
}
