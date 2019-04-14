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
import java.util.concurrent.TimeUnit

import wvlet.airframe.codec.MessageCodec
import wvlet.airframe.control.Retry.{RetryContext, Retryer}
import wvlet.airframe.control.{ResultClass, Retry}
import wvlet.airframe.surface.Surface
import wvlet.log.LogSupport

import scala.concurrent.duration.Duration
import scala.language.higherKinds
import scala.reflect.runtime.{universe => ru}

/**
  *
  * @tparam F An abstraction for Future type (e.g., Resolves the differences between Twitter Future, Scala Future, etc.)
  * @tparam Req
  * @tparam Resp
  */
trait HttpClient[F[_], Req, Resp] extends AutoCloseable {
  protected def requestAdapter: HttpRequestAdapter[Req]
  protected def responseAdapter: HttpResponseAdapter[Resp]

  def send(req: Req): F[Resp]
  def await(req: Req): Resp

  protected def newRequest(method: HttpMethod, path: String): Req

  protected def await[A](f: F[A]): A
  def get[A: ru.TypeTag](path: String): F[A]
  def getAwait[A: ru.TypeTag](path: String): A = await(get(path))

  def post[A: ru.TypeTag, R: ru.TypeTag](path: String, data: A): F[R]
  def postAwait[A: ru.TypeTag, R: ru.TypeTag](path: String, data: A): R = await(post(path, data))

  def delete[R: ru.TypeTag](path: String): F[R]
  def deleteAwait[R: ru.TypeTag](path: String): F[R] = await(delete(path))

  def delete[A: ru.TypeTag, R: ru.TypeTag](path: String, data: A): F[R]
  def deleteAwait[A: ru.TypeTag, R: ru.TypeTag](path: String, data: A): F[R] = await(delete(path, data))

  def put[R: ru.TypeTag](path: String): F[R]
  def putAwait[R: ru.TypeTag](path: String, data: R): R = await(put(path, data))

  def put[A: ru.TypeTag, R: ru.TypeTag](path: String, data: A): F[R]
  def putAwait[A: ru.TypeTag, R: ru.TypeTag](path: String, data: A): R = await(put(path, data))
}

object HttpClient extends LogSupport {
  def defaultRetryer: Retryer = {
    Retry
      .withBackOff(maxRetry = 10)
      .withErrorHandler(defaultErrorHandler)
      .beforeRetry(defaultBeforeRetryAction)
  }

  def defaultErrorHandler(ctx: RetryContext): Unit = {
    warn(s"Request failed: ${ctx.lastError.getMessage}")
    HttpClientException.defaultClientExceptionClassifier(ctx.lastError) match {
      case ResultClass.Failed(retryable) =>
        if (!retryable) {
          throw ctx.lastError
        }
    }
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
