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
package wvlet.airframe.http
import wvlet.airframe.control.Retry
import wvlet.airframe.control.Retry.{RetryContext, Retryer}
import wvlet.airframe.http.HttpClientException.defaultHttpExceptionHandler
import wvlet.log.LogSupport

class HttpClient

object HttpClient extends LogSupport {
  def newClient: HttpClient = ???

  def defaultRetryer: Retryer = {
    Retry
      .withBackOff(maxRetry = 10)
      .withErrorHandler(defaultErrorHandler)
      .beforeRetry(defaultBeforeRetryAction)
  }

  def defaultErrorHandler(ctx: RetryContext): Unit = {
    warn(s"Request failed: ${ctx.lastError.getMessage}")
    defaultHttpExceptionHandler().applyOrElse(ctx.lastError, { x: Throwable =>
      x match {
        case _ => throw x
      }
    })
  }

  def defaultBeforeRetryAction(ctx: RetryContext): Unit = {
    val extraWaitMillis =
      ctx.lastError match {
        case e: HttpClientException if e.status == HttpStatus.ServiceUnavailable_503 =>
          // Server is busy (e.g., S3 slow down). We need to reduce the request rate.
          (ctx.nextWaitMillis * 0.5).toLong // Add an extra wait
        case _ =>
          0
      }
    val nextWaitMillis = ctx.nextWaitMillis + extraWaitMillis
    warn(f"[${ctx.retryCount}/${ctx.maxRetry}] Retry the request in ${nextWaitMillis / 1000.0}%.2f sec.")
    Thread.sleep(extraWaitMillis)
  }

}
