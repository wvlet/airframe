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
  * Retry logic implementation helper
  */
object Retry {

  def withBackOff[C](maxRetry: Int = 3,
                     initialIntervalMillis: Int = 100,
                     maxIntervalMillis: Int = 15000,
                     multiplier: Double = 1.5): Retry.Retryer[C] = {
    val config = RetryConfig(initialIntervalMillis, maxIntervalMillis, multiplier)
    withRetry[C](maxRetry, new ExponentialBackOff(config))
  }

  def withJitter[C](maxRetry: Int = 3,
                    initialIntervalMillis: Int = 100,
                    maxIntervalMillis: Int = 15000,
                    multiplier: Double = 1.5): Retry.Retryer[C] = {
    val config = RetryConfig(initialIntervalMillis, maxIntervalMillis, multiplier)
    withRetry[C](maxRetry, new Jitter(config))
  }

  def withRetry[C](maxRetry: Int = 3, retryWaitStrategy: RetryWaitStrategy): Retryer[C] = {
    Retryer[C](None, maxRetry, retryWaitStrategy)
  }

  case class MaxRetryException(retryState: RetryContext[_]) extends Exception(retryState.lastError)
  case class RetryContext[C](context: Option[C],
                             lastError: Throwable,
                             retryCount: Int,
                             maxRetry: Int,
                             nextWaitMillis: Int) {
    def getContext: Option[C] = context
  }

  private def RETRY_ALL: RetryContext[_] => Unit = { e: RetryContext[_] =>
    // Do nothing
  }
  private val NOT_STARTED = new IllegalStateException("Not started")

  case class Retryer[C](context: Option[C] = None,
                        maxRetry: Int,
                        retryWaitStrategy: RetryWaitStrategy,
                        errorHandler: RetryContext[C] => Any = RETRY_ALL) {

    def withContext(contextObject: C): Retryer[C] = {
      Retryer(Option(contextObject), maxRetry, retryWaitStrategy, errorHandler)
    }

    def retryOnError[U](customHandler: PartialFunction[Throwable, U]): Retryer[C] = {
      Retryer(
        context,
        maxRetry,
        retryWaitStrategy, { s: RetryContext[C] =>
          customHandler.applyOrElse(s.lastError, { x: Throwable =>
            x match {
              case e: Throwable =>
                // Rethrow unhandled exceptions
                throw e
            }
          })
        }
      )
    }

    def retryOn[U](errorHandler: RetryContext[C] => U): Retryer[C] =
      Retryer(context, maxRetry, retryWaitStrategy, errorHandler)

    def runWithContext[A](context: C)(body: => A): A = {
      withContext(context).run(body)
    }

    def run[A](body: => A): A = {
      var result: Option[A]           = None
      var retryCount                  = 0
      var retryWait                   = retryWaitStrategy.retryConfig.initialIntervalMillis
      var retryState: RetryContext[C] = RetryContext(context, NOT_STARTED, 0, maxRetry, retryWait)

      while (result.isEmpty && retryCount < maxRetry) {
        Try(body) match {
          case Success(a) =>
            result = Some(a)
          case Failure(e) =>
            retryCount += 1
            val nextWait = retryWaitStrategy.adjustWait(retryWait)
            retryState = RetryContext(context, e, retryCount, maxRetry, nextWait)
            errorHandler(retryState)
            retryWait = retryWaitStrategy.updateWait(retryWait)
            Thread.sleep(nextWait)
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
