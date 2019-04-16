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
import wvlet.airframe.surface.Surface
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
                  multiplier: Double = 1.5): RetryContext = {
    defaultRetryContext.withMaxRetry(maxRetry).withBackOff(initialIntervalMillis, maxIntervalMillis, multiplier)
  }

  def withJitter(maxRetry: Int = 3,
                 initialIntervalMillis: Int = 100,
                 maxIntervalMillis: Int = 15000,
                 multiplier: Double = 1.5): RetryContext = {
    defaultRetryContext.withMaxRetry(maxRetry).withJitter(initialIntervalMillis, maxIntervalMillis, multiplier)
  }

  private val defaultRetryContext: RetryContext = {
    val retryConfig = RetryConfig()
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

  case class MaxRetryException(retryState: RetryContext) extends Exception(retryState.lastError)

  // Throw this to force retry the execution
  case class RetryableFailure(e: Throwable) extends Exception(e)

  case object NOT_STARTED extends Exception("Code is not executed")

  // Return this class at the beforeRetryAction to add extra weight time.
  case class AddExtraRetryWait(extraWaitMillis: Int)

  private def REPORT_FAILURE: RetryContext => Unit = { ctx: RetryContext =>
    warn(s"Execution failed: ${ctx.lastError}")
  }

  private def REPORT_RETRY_COUNT: RetryContext => Unit = { ctx: RetryContext =>
    warn(f"[${ctx.retryCount}/${ctx.maxRetry}] Execution failed. Retrying in ${ctx.nextWaitMillis / 1000.0}%.2f sec.")
  }

  case class RetryContext(context: Option[Any],
                          lastError: Throwable,
                          retryCount: Int,
                          maxRetry: Int,
                          retryWaitStrategy: RetryWaitStrategy,
                          nextWaitMillis: Int,
                          baseWaitMillis: Int,
                          resultClassifier: Any => ResultClass = ResultClass.AlwaysSucceed,
                          errorClassifier: Throwable => ResultClass = ResultClass.AlwaysRetry,
                          beforeRetryAction: RetryContext => Any = REPORT_RETRY_COUNT) {

    private def partialUpdate(newParam: Map[String, Any]): RetryContext = {
      val surface = Surface.of[RetryContext]
      val updatedParams = surface.params.map { p =>
        // Overwrite RetryContext parameters if given
        newParam.getOrElse(p.name, p.get(this))
      }
      surface.objectFactory.get.newInstance(updatedParams).asInstanceOf[RetryContext]
    }

    def init(context: Option[Any] = None): RetryContext = {
      partialUpdate(
        Map(
          "context"        -> context,
          "lastError"      -> NOT_STARTED,
          "retryCount"     -> 0,
          "nextWaitMillis" -> retryWaitStrategy.retryConfig.initialIntervalMillis,
          "baseWaitMillis" -> retryWaitStrategy.retryConfig.initialIntervalMillis
        ))
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
      val nextRetry = partialUpdate(
        Map(
          "lastError"      -> retryReason,
          "retryCount"     -> (retryCount + 1),
          "nextWaitMillis" -> retryWaitStrategy.adjustWait(baseWaitMillis),
          "baseWaitMillis" -> retryWaitStrategy.updateWait(nextWaitMillis)
        ))
      beforeRetryAction(nextRetry) match {
        case AddExtraRetryWait(extraWaitMillis) =>
          nextRetry.withExtraWaitMillis(extraWaitMillis)
        case _ =>
          nextRetry
      }
    }

    def withExtraWaitMillis(extraWaitMillis: Int): RetryContext = {
      partialUpdate(Map("nextWaitMillis" -> (this.nextWaitMillis + extraWaitMillis)))
    }

    def withRetryWaitStrategy(newRetryWaitStrategy: RetryWaitStrategy): RetryContext = {
      partialUpdate(Map("retryWaitStrategy" -> newRetryWaitStrategy))
    }

    def withMaxRetry(newMaxRetry: Int): RetryContext = {
      partialUpdate(Map("maxRetry" -> newMaxRetry))
    }

    def withBackOff(initialIntervalMillis: Int = 100,
                    maxIntervalMillis: Int = 15000,
                    multiplier: Double = 1.5): RetryContext = {
      val config = RetryConfig(initialIntervalMillis, maxIntervalMillis, multiplier)
      partialUpdate(Map("retryWaitStrategy" -> new ExponentialBackOff(config)))
    }

    def withJitter(initialIntervalMillis: Int = 100,
                   maxIntervalMillis: Int = 15000,
                   multiplier: Double = 1.5): RetryContext = {
      val config = RetryConfig(initialIntervalMillis, maxIntervalMillis, multiplier)
      partialUpdate(Map("retryWaitStrategy" -> new Jitter(config)))
    }

    def withResultClassifier[U](newResultClassifier: U => ResultClass): RetryContext = {
      partialUpdate(
        Map(
          "resultClassifier" -> newResultClassifier.asInstanceOf[Any => ResultClass]
        ))
    }

    /**
      * Set a detailed error handler upon Exception. If the given exception is not retryable,
      * just rethrow the exception. Otherwise, consume the exception.
      */
    def withErrorClassifier[U](errorClassifier: Throwable => U): RetryContext = {
      partialUpdate(Map("errorClassifier" -> errorClassifier))
    }

    def beforeRetry[U](handler: RetryContext => U): RetryContext = {
      partialUpdate(Map("beforeRetryAction" -> handler))
    }

    /**
      * Add a partial function that accepts exceptions that need to be retried.
      *
      * @param errorClassifier
      * @tparam U
      * @return
      */
    def retryOn[U](errorClassifier: PartialFunction[Throwable, Failed]): RetryContext = {
      partialUpdate(Map("errorClassifier" -> { e: Throwable =>
        errorClassifier.applyOrElse(e, RETHROW_ALL)
      }))
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
          case ResultClass.Failed(isRetryable, cause) if isRetryable =>
            // Retryable error
            retryContext = retryContext.nextRetry(cause)
            Thread.sleep(retryContext.nextWaitMillis)
          case ResultClass.Failed(isRetryable, cause) if !isRetryable =>
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

  private def RETHROW_ALL: Throwable => Unit = { e: Throwable =>
    throw e
  }

  case class Retryer(initRetryContext: RetryContext) extends LogSupport {}

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
