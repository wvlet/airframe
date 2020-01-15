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

import scala.reflect.ClassTag
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

  def withBoundedBackoff(
      initialIntervalMillis: Int = 100,
      maxTotalWaitMillis: Int = 180000,
      multiplier: Double = 1.5
  ): RetryContext = {
    require(initialIntervalMillis > 0, s"initialWaitMillis must be > 0: ${initialIntervalMillis}")

    // S = totalWaitMillis = w * r^0 + w * r^1 + w * r^2 + ...  + w * r^n
    // S = w * (1-r^n) / (1-r)
    // r^n = 1 - S * (1-r)/w
    // n * log(r) = log(1 - S * (1-r) / w)
    val N = math.log(1 - (maxTotalWaitMillis * (1 - multiplier) / initialIntervalMillis)) / math.log(multiplier)

    def total(n: Int) = initialIntervalMillis * (1 - math.pow(multiplier, n)) / (1 - multiplier)

    var maxRetry = N.ceil.toInt
    while (maxRetry > 0 && total(maxRetry) > maxTotalWaitMillis) {
      maxRetry -= 1
    }
    var maxIntervalMillis = initialIntervalMillis * math.pow(multiplier, N).toInt
    withBackOff(
      maxRetry = maxRetry.max(0),
      initialIntervalMillis = initialIntervalMillis,
      maxIntervalMillis = maxIntervalMillis,
      multiplier = multiplier
    )
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
      baseWaitMillis = retryConfig.initialIntervalMillis,
      extraWaitMillis = 0
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

  private def REPORT_RETRY_COUNT: RetryContext => Unit = { ctx: RetryContext =>
    warn(
      f"[${ctx.retryCount}/${ctx.maxRetry}] Execution failed: ${ctx.lastError.getMessage}. Retrying in ${ctx.nextWaitMillis / 1000.0}%.2f sec."
    )
  }

  private def RETHROW_ALL: Throwable => ResultClass = { e: Throwable =>
    throw e
  }

  private[control] val noExtraWait = ExtraWait()

  case class ExtraWait(maxExtraWaitMillis: Int = 0, factor: Double = 0.0) {
    require(maxExtraWaitMillis >= 0)
    require(factor >= 0)

    def hasNoWait: Boolean = {
      maxExtraWaitMillis == 0 && factor == 0.0
    }

    // Compute the extra wait millis based on the next wait millis
    def extraWaitMillis(nextWaitMillis: Int): Int = {
      if (maxExtraWaitMillis == 0) {
        if (factor == 0.0) {
          0
        } else {
          (nextWaitMillis * factor).toInt
        }
      } else {
        if (factor == 0.0) {
          maxExtraWaitMillis
        } else {
          (nextWaitMillis * factor).toInt.min(maxExtraWaitMillis)
        }
      }
    }
  }

  case class RetryContext(
      context: Option[Any],
      lastError: Throwable,
      retryCount: Int,
      maxRetry: Int,
      retryWaitStrategy: RetryPolicy,
      nextWaitMillis: Int,
      baseWaitMillis: Int,
      extraWaitMillis: Int,
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
        baseWaitMillis = retryWaitStrategy.retryPolicyConfig.initialIntervalMillis,
        extraWaitMillis = 0
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
      val nextRetryCtx = this.copy(
        lastError = retryReason,
        retryCount = retryCount + 1,
        nextWaitMillis = retryWaitStrategy.nextWait(baseWaitMillis) + extraWaitMillis,
        baseWaitMillis = retryWaitStrategy.updateBaseWait(baseWaitMillis),
        extraWaitMillis = 0
      )
      beforeRetryAction(nextRetryCtx)
      nextRetryCtx
    }

    def withExtraWait(extraWait: ExtraWait): RetryContext = {
      if (extraWait.hasNoWait && this.extraWaitMillis == 0) {
        this
      } else {
        this.copy(extraWaitMillis = extraWait.extraWaitMillis(nextWaitMillis))
      }
    }

    def withRetryWaitStrategy(newRetryWaitStrategy: RetryPolicy): RetryContext = {
      this.copy(retryWaitStrategy = newRetryWaitStrategy)
    }

    def withMaxRetry(newMaxRetry: Int): RetryContext = {
      this.copy(maxRetry = newMaxRetry)
    }

    def noRetry: RetryContext = {
      this.copy(maxRetry = 0)
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
      * Clear the default beforeRetry action
      */
    def noRetryLogging: RetryContext = {
      this.copy(beforeRetryAction = { x: RetryContext =>
      })
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

    def run[A: ClassTag](body: => A): A = {
      runInternal(None)(body)
    }

    def runWithContext[A: ClassTag](context: Any)(body: => A): A = {
      runInternal(Option(context))(body)
    }

    protected def runInternal[A: ClassTag](context: Option[Any])(body: => A): A = {
      var result: Option[A]          = None
      var retryContext: RetryContext = init(context)

      do {
        val ret = Try(body)
        val resultClass = ret match {
          case Success(x) =>
            // Test whether the code block execution is succeeded or failed
            resultClassifier(x)
          case Failure(RetryableFailure(e)) =>
            ResultClass.retryableFailure(e)
          case Failure(e) =>
            errorClassifier(e)
        }

        resultClass match {
          case ResultClass.Succeeded(x) =>
            // OK. Exit the loop
            val clazz = implicitly[ClassTag[A]].runtimeClass
            if (clazz.isAssignableFrom(x.getClass)) {
              result = Some(x.asInstanceOf[A])
            } else {
              ResultClass.nonRetryableFailure(new ClassCastException(s"${x} is not an instance of ${resultClass}"))
            }
          case ResultClass.Failed(isRetryable, cause, extraWait) if isRetryable =>
            // Retryable error
            retryContext = retryContext.withExtraWait(extraWait).nextRetry(cause)
            // Wait until the next retry
            Thread.sleep(retryContext.nextWaitMillis)
          case ResultClass.Failed(isRetryable, cause, _) if !isRetryable =>
            // Non-retryable error. Exit the loop by throwing the exception
            throw cause
        }
      } while (result.isEmpty && retryContext.canContinue)

      result match {
        case Some(a) =>
          a
        case None =>
          throw MaxRetryException(retryContext)
      }
    }
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
