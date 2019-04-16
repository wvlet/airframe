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

import wvlet.airframe.control.ResultClass.Failed
import wvlet.log.LogSupport

import scala.util.{Failure, Random, Success, Try}

/**
  * Retry logic implementation helper
  */
object Retry extends LogSupport {

  /**
    * Classify the code results to Successful or Failed
    */
  type ResultClassifier    = PartialFunction[Any, ResultClass]
  type ExceptionClassifier = PartialFunction[Throwable, Failed]

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

  case class MaxRetryException(retryState: RetryContext) extends Exception(retryState.lastError)
  case class RetryableFailure(e: Throwable)              extends Exception(e)
  case class RetryContext(context: Option[Any],
                          lastError: Throwable,
                          retryCount: Int,
                          maxRetry: Int,
                          retryWaitStrategy: RetryWaitStrategy,
                          nextWaitMillis: Int,
                          baseWaitMillis: Int) {

    def canContinue: Boolean = {
      retryCount < maxRetry
    }

    def update(e: Throwable): RetryContext = {
      RetryContext(context,
                   e,
                   retryCount + 1,
                   maxRetry,
                   retryWaitStrategy,
                   retryWaitStrategy.adjustWait(baseWaitMillis),
                   retryWaitStrategy.updateWait(nextWaitMillis))
    }
  }

  private def RETRY_ALL: RetryContext => Unit = { e: RetryContext =>
    // Do nothing
  }
  private def REPORT_RETRY_COUNT: RetryContext => Unit = { ctx: RetryContext =>
    warn(f"[${ctx.retryCount}/${ctx.maxRetry}] Execution failed. Retrying in ${ctx.nextWaitMillis / 1000.0}%.2f sec.")
  }

  private def RETHROW_ALL: Throwable => Unit = { e: Throwable =>
    throw e
  }

  private val NOT_STARTED = new IllegalStateException("Not started")

  case class Retryer(maxRetry: Int,
                     retryWaitStrategy: RetryWaitStrategy,
                     resultClassifier: Any => ResultClass = ResultClass.AlwaysSucceed,
                     errorHandler: RetryContext => Any = RETRY_ALL,
                     beforeRetryAction: RetryContext => Any = REPORT_RETRY_COUNT)
      extends LogSupport {

    def withRetryWaitStrategy(newRetryWaitStrategy: RetryWaitStrategy): Retryer = {
      Retryer(maxRetry, newRetryWaitStrategy, resultClassifier, errorHandler, beforeRetryAction)
    }

    def withMaxRetry(newMaxRetry: Int): Retryer = {
      Retryer(newMaxRetry, retryWaitStrategy, resultClassifier, errorHandler, beforeRetryAction)
    }

    def withBackOff(initialIntervalMillis: Int = 100,
                    maxIntervalMillis: Int = 15000,
                    multiplier: Double = 1.5): Retryer = {
      val config = RetryConfig(initialIntervalMillis, maxIntervalMillis, multiplier)
      withRetryWaitStrategy(new ExponentialBackOff(config))
    }

    def withJitter(initialIntervalMillis: Int = 100,
                   maxIntervalMillis: Int = 15000,
                   multiplier: Double = 1.5): Retry.Retryer = {
      val config = RetryConfig(initialIntervalMillis, maxIntervalMillis, multiplier)
      withRetryWaitStrategy(new Jitter(config))
    }

    /**
      * Add a partial function that accepts exceptions that need to be retried.
      *
      * @param handlerForRetryableError
      * @tparam U
      * @return
      */
    def retryOn[U](handlerForRetryableError: PartialFunction[Throwable, U]): Retryer = {
      Retryer(
        maxRetry,
        retryWaitStrategy,
        resultClassifier,
        errorHandler = { s: RetryContext =>
          // Rethrow all unhandled exceptions
          handlerForRetryableError.applyOrElse(s.lastError, RETHROW_ALL)
        },
        beforeRetryAction
      )
    }

    def withResultClassifier[U](newResultClassifier: U => ResultClass): Retryer = {
      Retryer(maxRetry,
              retryWaitStrategy,
              newResultClassifier.asInstanceOf[Any => ResultClass],
              errorHandler,
              beforeRetryAction)
    }

    /**
      * Set a detailed error handler upon Exception. If the given exception is not retryable,
      * just rethrow the exception. Otherwise, consume the exception.
      *
      * @param errorHandler
      * @tparam U
      * @return
      */
    def withErrorHandler[U](errorHandler: RetryContext => U): Retryer =
      Retryer(maxRetry, retryWaitStrategy, resultClassifier, errorHandler, beforeRetryAction)

    def beforeRetry[U](handler: RetryContext => U): Retryer = {
      Retryer(maxRetry, retryWaitStrategy, resultClassifier, errorHandler, handler)
    }

    def run[A](body: => A): A = {
      runInternal(None)(body)
    }

    def runWithContext[A](context: Any)(body: => A): A = {
      runInternal(Option(context))(body)
    }

    protected def runInternal[A](context: Option[Any])(body: => A): A = {
      var result: Option[A] = None
      var retryContext: RetryContext = RetryContext(
        context,
        NOT_STARTED,
        0,
        maxRetry,
        retryWaitStrategy,
        retryWaitStrategy.retryConfig.initialIntervalMillis,
        retryWaitStrategy.retryConfig.initialIntervalMillis
      )

      while (result.isEmpty && retryContext.canContinue) {
        def retry(errorType: Throwable, handleError: Boolean): Unit = {
          retryContext = retryContext.update(errorType)
          if (handleError) {
            errorHandler(retryContext)
          }
          beforeRetryAction(retryContext)
          Thread.sleep(retryContext.nextWaitMillis)
        }

        Try(body) match {
          case Success(a) =>
            // Test whether the code block execution is successeded or failed
            resultClassifier(a) match {
              case ResultClass.Succeeded =>
                // OK. Exit the loop
                result = Some(a)
              case ResultClass.Failed(isRetryable, cause) if isRetryable == true =>
                // Retryable error
                retry(cause, handleError = false)
              case ResultClass.Failed(isRetryable, cause) if isRetryable == false =>
                // Non-retryable error
                throw cause
            }
          case Failure(RetryableFailure(e)) =>
            retry(e, handleError = false)
          case Failure(e) =>
            retry(e, handleError = true)
        }
      }
      result match {
        case Some(a) =>
          a
        case None =>
          throw MaxRetryException(retryContext)
      }
    }

    def newRetryContext(context: Option[Any]): RetryContext = {
      RetryContext(
        context,
        NOT_STARTED,
        0,
        maxRetry,
        retryWaitStrategy,
        retryWaitStrategy.retryConfig.initialIntervalMillis,
        retryWaitStrategy.retryConfig.initialIntervalMillis
      )
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
