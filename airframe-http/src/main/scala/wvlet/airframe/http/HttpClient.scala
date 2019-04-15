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
import wvlet.airframe.control.Retry.{RetryContext, Retryer}
import wvlet.airframe.control.{ResultClass, Retry}
import wvlet.log.LogSupport

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

  protected def newRequest(method: HttpMethod, path: String): Req

  private[http] def awaitF[A](f: F[A]): A

  def get[Resource: ru.TypeTag](resourcePath: String): F[Resource]
  def list[OperationResponse: ru.TypeTag](resourcePath: String): F[OperationResponse]
  def post[Resource: ru.TypeTag](resourcePath: String, resource: Resource): F[Resource]
  def post[Resource: ru.TypeTag, OperationResponse: ru.TypeTag](resourcePath: String,
                                                                resource: Resource): F[OperationResponse]
  def put[Resource: ru.TypeTag](resourcePath: String, resource: Resource): F[Resource]
  def put[Resource: ru.TypeTag, OperationResponse: ru.TypeTag](resourcePath: String,
                                                               resource: Resource): F[OperationResponse]
  def delete[OperationResponse: ru.TypeTag](resourcePath: String): F[OperationResponse]

  def syncClient: HttpSyncClient[F, Req, Resp] = new HttpSyncClient(this)
}

/**
  * HttpClient that awaits responses.
  *
  * @param asyncClient
  * @tparam F
  * @tparam Req
  * @tparam Resp
  */
class HttpSyncClient[F[_], Req, Resp](asyncClient: HttpClient[F, Req, Resp]) extends AutoCloseable {
  protected def awaitF[A](f: F[A]): A = asyncClient.awaitF(f)

  def send(req: Req): Resp = awaitF(asyncClient.send(req))

  def get[Resource: ru.TypeTag](resourcePath: String): Resource = {
    awaitF(asyncClient.get[Resource](resourcePath))
  }
  def list[OperationResponse: ru.TypeTag](resourcePath: String): OperationResponse = {
    awaitF(asyncClient.list[OperationResponse](resourcePath))
  }

  def post[Resource: ru.TypeTag](resorucePath: String, resource: Resource): Resource = {
    awaitF(asyncClient.post[Resource](resorucePath, resource))
  }
  def post[Resource: ru.TypeTag, OperationResponse: ru.TypeTag](resorucePath: String,
                                                                resource: Resource): OperationResponse = {
    awaitF(asyncClient.post[Resource, OperationResponse](resorucePath, resource))
  }

  def put[Resource: ru.TypeTag](resourcePath: String, resource: Resource): Resource = {
    awaitF(asyncClient.put[Resource](resourcePath, resource))
  }
  def put[Resource: ru.TypeTag, OperationResponse: ru.TypeTag](resourcePath: String,
                                                               resource: Resource): OperationResponse = {
    awaitF(asyncClient.put[Resource, OperationResponse](resourcePath, resource))
  }

  def delete[OperationResponse: ru.TypeTag](resourcePath: String): OperationResponse = {
    awaitF(asyncClient.delete[OperationResponse](resourcePath))
  }

  override def close(): Unit = {
    asyncClient.close()
  }
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
