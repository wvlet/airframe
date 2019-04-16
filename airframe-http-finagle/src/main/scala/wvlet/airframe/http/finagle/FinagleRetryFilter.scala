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
import wvlet.airframe.control.Retry.{RetryContext, Retryer}
import wvlet.airframe.http.HttpClientException
import wvlet.log.LogSupport

/**
  * A filter for integrating Airframe Retry and Finagle
  */
class FinagleRetryFilter(retryer: RetryContext, timer: Timer = DefaultTimer)
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

  private def dispatch(retryContext: RetryContext,
                       request: Request,
                       service: Service[Request, Response]): Future[Response] = {
    val rep = service(request)
    rep.transform { x =>
      // TODO Use error handler defiend in RetryContext
      val classifier = x match {
        case Throw(e) =>
          HttpClientException.classifyExecutionFailure(e)
        case Return(r) =>
          HttpClientException.classifyHttpResponse[http.Response](r)
      }

      classifier match {
        case ResultClass.Succeeded =>
          rep
        case ResultClass.Failed(isRetryable, cause) => {
          if (retryContext.canContinue && isRetryable) {
            Future
              .value {
                // Update the retry count
                retryContext.nextRetry(cause)
              }.flatMap { nextRetryContext =>
                // Wait until the next retry
                schedule(retryContext.nextWaitMillis.millis) {
                  // Run the same request again
                  dispatch(nextRetryContext, request, service)
                }
              }
          } else {
            // No more retry.
            x match {
              case Throw(e) =>
                warn(e)
                // Create an error response
                val r = Response(Status.BadRequest)
                r.setContentString(cause.getMessage)
                Future.value(r)
              case Return(r) =>
                // Just return the last failed response
                rep
            }
          }
        }
      }
    }
  }

  override def apply(request: Request, service: Service[Request, Response]): Future[Response] = {
    val retryContext = retryer.init(Option(request))
    dispatch(retryContext, request, service)
  }
}
