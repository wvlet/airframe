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
package wvlet.airframe.http.finagle

import com.twitter.finagle.http.{Request, Response, Status}
import com.twitter.finagle.util.DefaultTimer
import com.twitter.finagle.{Service, SimpleFilter, http}
import com.twitter.util._
import wvlet.airframe.control.ResultClass
import wvlet.airframe.control.Retry.{MaxRetryException, RetryContext}
import wvlet.airframe.http.{HttpClientException, HttpClientMaxRetryException}
import wvlet.log.LogSupport

/**
  * A filter for integrating Airframe Retry and Finagle
  */
class FinagleRetryFilter(retry: RetryContext, timer: Timer = DefaultTimer)
    extends SimpleFilter[http.Request, http.Response]
    with LogSupport {
  import com.twitter.conversions.DurationOps._

  private[this] def schedule(d: Duration)(f: => Future[Response]) = {
    if (d > 0.seconds) {
      val promise = new Promise[Response]
      timer.schedule(Time.now + d) {
        promise.become(f)
      }
      promise
    } else {
      f
    }
  }

  private def dispatch(
      retryContext: RetryContext,
      request: Request,
      service: Service[Request, Response]
  ): Future[Response] = {
    val rep = service(request)
    rep.transform { x =>
      val classifier = x match {
        case Throw(e) =>
          retryContext.errorClassifier(e)
        case Return(r) =>
          retryContext.resultClassifier(r)
      }

      classifier match {
        case ResultClass.Succeeded =>
          rep
        case ResultClass.Failed(isRetryable, cause, extraWait) => {
          if (!retryContext.canContinue) {
            // Reached the max retry
            rep.flatMap { r =>
              Future.exception(HttpClientMaxRetryException(FinagleHttpResponseWrapper(r), retryContext, cause))
            }
          } else if (!isRetryable) {
            // Non-retryable failure
            Future.exception(cause)
          } else {
            Future
              .value {
                // Update the retry count
                retryContext.withExtraWait(extraWait).nextRetry(cause)
              }.flatMap { nextRetryContext =>
                // Wait until the next retry
                schedule(nextRetryContext.nextWaitMillis.millis) {
                  // Run the same request again
                  dispatch(nextRetryContext, request, service)
                }
              }
          }
        }
      }
    }
  }

  override def apply(request: Request, service: Service[Request, Response]): Future[Response] = {
    val retryContext = retry.init(Option(request))
    dispatch(retryContext, request, service)
  }
}
