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

  def retryableFailure(e: Throwable)    = Failed(isRetryable = true, e)
  def nonRetryableFailure(e: Throwable) = Failed(isRetryable = false, e)

  def withBackOff(
      maxRetry: Int = 3,
      initialIntervalMillis: Int = 100,
      maxIntervalMillis: Int = 15000,
      multiplier: Double = 1.5
  ): RetryContext = {
    defaultRetryContext.withMaxRetry(maxRetry).withBackOff(initialIntervalMillis, maxIntervalMillis, multiplier)
  }

  def withJitter(
      maxRetry: Int = 3,
      initialIntervalMillis: Int = 100,
      maxIntervalMillis: Int = 15000,
      multiplier: Double = 1.5
  ): RetryContext = {
    defaultRetryContext.withMaxRetry(maxRetry).withJitter(initialIntervalMillis, maxIntervalMillis, multiplier)
  }

  private val defaultRetryContext: RetryContext = {
    val retryConfig = RetryPolicyConfig()
    RetryContext(
      context = None,
      lastError = NOT_STARTED,
      retryCount = 0,
      maxRetry = 3,
      retryWaitStrategy = new ExponentialBackOff(retryConfig),
      nextWaitMillis = retryConfig.initialIntervalMillis,
      baseWaitMillis = retryConfig.initialIntervalMillis
    )
  }

  case class MaxRetryException(retryContext: RetryContext)
      extends Exception(
        s"Reached the max retry count ${retryContext.retryCount}/${retryContext.maxRetry}: ${retryContext.lastError.getMessage}",
        retryContext.lastError
      )

  // Throw this to force retry the execution
  case class RetryableFailure(e: Throwable) extends Exception(e)

  case object NOT_STARTED extends Exception("Code is not executed")

  // Return this class at the beforeRetryAction to add extra wait time.
  case class AddExtraRetryWait(extraWaitMillis: Int)

  private def REPORT_RETRY_COUNT: RetryContext => Unit = { ctx: RetryContext =>
    warn(
      f"[${ctx.retryCount}/${ctx.maxRetry}] Execution failed: ${ctx.lastError.getMessage}. Retrying in ${ctx.nextWaitMillis / 1000.0}%.2f sec."
    )
  }

  case class RetryContext(
      context: Option[Any],
      lastError: Throwable,
      retryCount: Int,
      maxRetry: Int,
      retryWaitStrategy: RetryPolicy,
      nextWaitMillis: Int,
      baseWaitMillis: Int,
      resultClassifier: Any => ResultClass = ResultClass.ALWAYS_SUCCEED,
      errorClassifier: Throwable => ResultClass = ResultClass.ALWAYS_RETRY,
      beforeRetryAction: RetryContext => Any = REPORT_RETRY_COUNT
  ) {
    def init(context: Option[Any] = None): RetryContext = {
      this.copy(
        context = context,
        lastError = NOT_STARTED,
        retryCount = 0,
        nextWaitMillis = retryWaitStrategy.retryPolicyConfig.initialIntervalMillis,
        baseWaitMillis = retryWaitStrategy.retryPolicyConfig.initialIntervalMillis
      )
    }

    def canContinue: Boolean = {
      retryCount < maxRetry
    }

    /**
      * Update the retry context, including retry count, last error, next wait time, etc.
      *
      * @param retryReason
      * @return the next retry context
      */
    def nextRetry(retryReason: Throwable): RetryContext = {
      val nextRetry = this.copy(
        lastError = retryReason,
        retryCount = retryCount + 1,
        nextWaitMillis = retryWaitStrategy.nextWait(baseWaitMillis),
        baseWaitMillis = retryWaitStrategy.updateBaseWait(baseWaitMillis)
      )
      beforeRetryAction(nextRetry) match {
        case AddExtraRetryWait(extraWaitMillis) if extraWaitMillis > 0 =>
          nextRetry.withExtraWaitMillis(extraWaitMillis)
        case _ =>
          nextRetry
      }
    }

    def withExtraWaitMillis(extraWaitMillis: Int): RetryContext = {
      this.copy(nextWaitMillis = this.nextWaitMillis + extraWaitMillis)
    }

    def withRetryWaitStrategy(newRetryWaitStrategy: RetryPolicy): RetryContext = {
      this.copy(retryWaitStrategy = newRetryWaitStrategy)
    }

    def withMaxRetry(newMaxRetry: Int): RetryContext = {
      this.copy(maxRetry = newMaxRetry)
    }

    def withBackOff(
        initialIntervalMillis: Int = 100,
        maxIntervalMillis: Int = 15000,
        multiplier: Double = 1.5
    ): RetryContext = {
      val config = RetryPolicyConfig(initialIntervalMillis, maxIntervalMillis, multiplier)
      this.copy(retryWaitStrategy = new ExponentialBackOff(config))
    }

    def withJitter(
        initialIntervalMillis: Int = 100,
        maxIntervalMillis: Int = 15000,
        multiplier: Double = 1.5
    ): RetryContext = {
      val config = RetryPolicyConfig(initialIntervalMillis, maxIntervalMillis, multiplier)
      this.copy(retryWaitStrategy = new Jitter(config))
    }

    def withResultClassifier[U](newResultClassifier: U => ResultClass): RetryContext = {
      this.copy(resultClassifier = newResultClassifier.asInstanceOf[Any => ResultClass])
    }

    /**
      * Set a detailed error handler upon Exception. If the given exception is not retryable,
      * just rethrow the exception. Otherwise, consume the exception.
      */
    def withErrorClassifier(errorClassifier: Throwable => ResultClass): RetryContext = {
      this.copy(errorClassifier = errorClassifier)
    }

    def beforeRetry[U](handler: RetryContext => U): RetryContext = {
      this.copy(beforeRetryAction = handler)
    }

    /**
      * Add a partial function that accepts exceptions that need to be retried.
      *
      * @param errorClassifier
      * @return
      */
    def retryOn(errorClassifier: PartialFunction[Throwable, ResultClass]): RetryContext = {
      this.copy(errorClassifier = { e: Throwable =>
        errorClassifier.applyOrElse(e, RETHROW_ALL)
      })
    }

    def run[A](body: => A): A = {
      runInternal(None)(body)
    }

    def runWithContext[A](context: Any)(body: => A): A = {
      runInternal(Option(context))(body)
    }

    protected def runInternal[A](context: Option[Any])(body: => A): A = {
      var result: Option[A]          = None
      var retryContext: RetryContext = init(context)

      while (result.isEmpty && retryContext.canContinue) {
        val ret = Try(body)
        val resultClass = ret match {
          case Success(x) =>
            // Test whether the code block execution is successeded or failed
            resultClassifier(x)
          case Failure(RetryableFailure(e)) =>
            ResultClass.retryableFailure(e)
          case Failure(e) =>
            errorClassifier(e)
        }

        resultClass match {
          case ResultClass.Succeeded =>
            // OK. Exit the loop
            result = Some(ret.get)
          case ResultClass.Failed(isRetryable, cause, extraWaitMillis) if isRetryable =>
            // Retryable error
            retryContext = retryContext.nextRetry(cause)
            // Wait until the next retry
            Thread.sleep(retryContext.nextWaitMillis + extraWaitMillis)
          case ResultClass.Failed(isRetryable, cause, _) if !isRetryable =>
            // Non-retryable error. Exit the loop by throwing the exception
            throw cause
        }
      }

      result match {
        case Some(a) =>
          a
        case None =>
          throw MaxRetryException(retryContext)
      }
    }
  }

  private def RETHROW_ALL: Throwable => ResultClass = { e: Throwable =>
    throw e
  }

  case class RetryPolicyConfig(
      initialIntervalMillis: Int = 100,
      maxIntervalMillis: Int = 15000,
      multiplier: Double = 1.5
  ) {
    require(initialIntervalMillis >= 0)
    require(maxIntervalMillis >= 0)
    require(multiplier >= 0)
  }

  trait RetryPolicy {
    def retryPolicyConfig: RetryPolicyConfig
    def updateBaseWait(waitMillis: Int): Int = {
      math.round(waitMillis * retryPolicyConfig.multiplier).toInt.min(retryPolicyConfig.maxIntervalMillis)
    }
    def nextWait(baseWaitMillis: Int): Int
  }

  class ExponentialBackOff(val retryPolicyConfig: RetryPolicyConfig) extends RetryPolicy {
    override def nextWait(baseWaitMillis: Int): Int = {
      baseWaitMillis
    }
  }

  class Jitter(val retryPolicyConfig: RetryPolicyConfig, rand: Random = new Random()) extends RetryPolicy {
    override def nextWait(baseWaitMillis: Int): Int = {
      (baseWaitMillis.toDouble * rand.nextDouble()).round.toInt
    }
  }
}
