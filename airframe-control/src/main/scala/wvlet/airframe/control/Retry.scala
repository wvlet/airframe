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

import scala.util.{Failure, Random, Success, Try}

/**
  *
  */
object Retry {

  def withBackOff(maxRetry: Int = 3,
                  initialIntervalMillis: Int = 100,
                  maxIntervalMillis: Int = 15000,
                  multiplier: Double = 1.5): Retry.Retryer = {
    val config = RetryConfig(initialIntervalMillis, maxIntervalMillis, multiplier)
    withRetry(maxRetry, new ExponentialBackOff(config))
  }

  def withJitter(maxRetry: Int = 3,
                 initialIntervalMillis: Int = 100,
                 maxIntervalMillis: Int = 15000,
                 multiplier: Double = 1.5): Retry.Retryer = {
    val config = RetryConfig(initialIntervalMillis, maxIntervalMillis, multiplier)
    withRetry(maxRetry, new Jitter(config))
  }

  def withRetry(maxRetry: Int = 3, retryWaitStrategy: RetryWaitStrategy): Retryer = {
    Retryer(maxRetry, retryWaitStrategy)
  }

  case class MaxRetryException(retryState: LastError) extends Exception(retryState.lastError)
  case class LastError(lastError: Throwable, retryCount: Int, maxRetry: Int, nextWaitMillis: Int)

  private def RETRY_ALL: LastError => Unit = { e: LastError =>
    // Do nothing
  }
  private val NOT_STARTED = new IllegalStateException("Not started")

  case class Retryer(maxRetry: Int, retryWaitStrategy: RetryWaitStrategy, handler: LastError => Any = RETRY_ALL) {

    def retryOnError[U](handler: Throwable => U): Retryer = {
      Retryer(maxRetry, retryWaitStrategy, { s: LastError =>
        handler(s.lastError)
      })
    }
    def retryOn[U](handler: LastError => U): Retryer = Retryer(maxRetry, retryWaitStrategy, handler)

    def run[A](body: => A): A = {
      var result: Option[A]     = None
      var retryCount            = 0
      var retryWait             = retryWaitStrategy.retryConfig.initialIntervalMillis
      var retryState: LastError = LastError(NOT_STARTED, 0, maxRetry, retryWait)

      while (result.isEmpty && retryCount < maxRetry) {
        Try(body) match {
          case Success(a) =>
            result = Some(a)
          case Failure(e) =>
            retryCount += 1
            val nextWait = retryWaitStrategy.adjustWait(retryWait)
            retryState = LastError(e, retryCount, maxRetry, nextWait)
            handler(retryState)
            retryWait = retryWaitStrategy.updateWait(retryWait)
            Compat.sleep(nextWait)
        }
      }
      result match {
        case Some(a) => a
        case None =>
          throw MaxRetryException(retryState)
      }
    }
  }

  case class RetryConfig(initialIntervalMillis: Int = 100, maxIntervalMillis: Int = 15000, multiplier: Double = 1.5) {
    require(initialIntervalMillis >= 0)
    require(maxIntervalMillis >= 0)
    require(multiplier >= 0)
  }

  trait RetryWaitStrategy {
    def retryConfig: RetryConfig
    def updateWait(waitMillis: Int): Int
    def adjustWait(waitMillis: Int): Int
  }

  class ExponentialBackOff(val retryConfig: RetryConfig) extends RetryWaitStrategy {
    override def updateWait(waitMillis: Int): Int = {
      math.round(waitMillis * retryConfig.multiplier).toInt.min(retryConfig.maxIntervalMillis)
    }
    override def adjustWait(waitMillis: Int): Int = {
      waitMillis
    }
  }

  class Jitter(val retryConfig: RetryConfig, rand: Random = new Random()) extends RetryWaitStrategy {
    override def updateWait(waitMillis: Int): Int = {
      math.round(waitMillis * retryConfig.multiplier).toInt.min(retryConfig.maxIntervalMillis)
    }
    override def adjustWait(waitMillis: Int): Int = {
      (waitMillis.toDouble * rand.nextDouble()).round.toInt
    }
  }
}
