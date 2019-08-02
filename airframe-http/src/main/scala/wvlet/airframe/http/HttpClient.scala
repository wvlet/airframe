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
import java.net.URLEncoder

import wvlet.airframe.control.Retry
import wvlet.airframe.control.Retry.{AddExtraRetryWait, RetryContext}
import wvlet.log.LogSupport

import scala.language.higherKinds
import scala.reflect.ClassTag
import scala.reflect.runtime.{universe => ru}

/**
  * Asynchronous HTTP Client interface
  *
  * @tparam F An abstraction for Future type (e.g., Resolves the differences between Twitter Future, Scala Future, etc.)
  * @tparam Req
  * @tparam Resp
  */
trait HttpClient[F[_], Req, Resp] extends AutoCloseable {

  /**
    * Send an HTTP request and get the response. It will throw an exception for non successful responses
    *
    * @throws HttpClientMaxRetryException if max retry reaches
    * @throws HttpClientException for non-retryable error is happend
    */
  def send(req: Req): F[Resp]

  /**
    * Send an HTTP request and returns a response (or the last response if the request is retried)
    */
  def sendSafe(req: Req): F[Resp]

  /**
    * Await the response and extract the return value
    *
    * @param f
    * @tparam A
    * @return
    */
  private[http] def awaitF[A](f: F[A]): A

  def get[Resource: ru.TypeTag](resourcePath: String, requestFilter: Req => Req = identity): F[Resource]

  /**
    * Send a get request using the ResourceRequest. ResourceRequest parameters will be expanded as URL query strings
    */
  def getResource[ResourceRequest: ru.TypeTag, Resource: ru.TypeTag](resourcePath: String,
                                                                     resourceRequest: ResourceRequest,
                                                                     requestFilter: Req => Req = identity): F[Resource]

  def list[OperationResponse: ru.TypeTag](resourcePath: String,
                                          requestFilter: Req => Req = identity): F[OperationResponse]
  def post[Resource: ru.TypeTag](resourcePath: String,
                                 resource: Resource,
                                 requestFilter: Req => Req = identity): F[Resource]
  def postOps[Resource: ru.TypeTag, OperationResponse: ru.TypeTag](
      resourcePath: String,
      resource: Resource,
      requestFilter: Req => Req = identity): F[OperationResponse]
  def put[Resource: ru.TypeTag](resourcePath: String,
                                resource: Resource,
                                requestFilter: Req => Req = identity): F[Resource]
  def putOps[Resource: ru.TypeTag, OperationResponse: ru.TypeTag](
      resourcePath: String,
      resource: Resource,
      requestFilter: Req => Req = identity): F[OperationResponse]
  def delete[OperationResponse: ru.TypeTag](resourcePath: String,
                                            requestFilter: Req => Req = identity): F[OperationResponse]

  def syncClient: HttpSyncClient[F, Req, Resp] = new HttpSyncClient(this)
}

/**
  * A synchronous HttpClient that awaits responses.
  *
  * @param asyncClient
  * @tparam F
  * @tparam Req
  * @tparam Resp
  */
class HttpSyncClient[F[_], Req, Resp](asyncClient: HttpClient[F, Req, Resp]) extends AutoCloseable {
  protected def awaitF[A](f: F[A]): A = asyncClient.awaitF(f)

  /**
    * Send an HTTP request and get the response. It will throw an exception for non successful responses
    *
    * @throws HttpClientMaxRetryException if max retry reaches
    * @throws HttpClientException for non-retryable error is happend
    */
  def send(req: Req): Resp = awaitF(asyncClient.send(req))

  /**
    * Send an HTTP request and returns a response (or the last response if the request is retried)
    */
  def sendSafe(req: Req): Resp = awaitF(asyncClient.sendSafe(req))

  def get[Resource: ru.TypeTag](resourcePath: String, requestFilter: Req => Req = identity): Resource = {
    awaitF(asyncClient.get[Resource](resourcePath, requestFilter))
  }
  def getResource[ResourceRequest: ru.TypeTag, Resource: ru.TypeTag](resourcePath: String,
                                                                     resourceRequest: ResourceRequest,
                                                                     requestFilter: Req => Req = identity): Resource = {
    awaitF(asyncClient.getResource[ResourceRequest, Resource](resourcePath, resourceRequest, requestFilter))
  }
  def list[OperationResponse: ru.TypeTag](resourcePath: String,
                                          requestFilter: Req => Req = identity): OperationResponse = {
    awaitF(asyncClient.list[OperationResponse](resourcePath, requestFilter))
  }

  def post[Resource: ru.TypeTag](resourcePath: String,
                                 resource: Resource,
                                 requestFilter: Req => Req = identity): Resource = {
    awaitF(asyncClient.post[Resource](resourcePath, resource, requestFilter))
  }
  def postOps[Resource: ru.TypeTag, OperationResponse: ru.TypeTag](
      resourcePath: String,
      resource: Resource,
      requestFilter: Req => Req = identity): OperationResponse = {
    awaitF(asyncClient.postOps[Resource, OperationResponse](resourcePath, resource, requestFilter))
  }

  def put[Resource: ru.TypeTag](resourcePath: String,
                                resource: Resource,
                                requestFilter: Req => Req = identity): Resource = {
    awaitF(asyncClient.put[Resource](resourcePath, resource, requestFilter))
  }
  def putOps[Resource: ru.TypeTag, OperationResponse: ru.TypeTag](
      resourcePath: String,
      resource: Resource,
      requestFilter: Req => Req = identity): OperationResponse = {
    awaitF(asyncClient.putOps[Resource, OperationResponse](resourcePath, resource, requestFilter))
  }

  def delete[OperationResponse: ru.TypeTag](resourcePath: String,
                                            requestFilter: Req => Req = identity): OperationResponse = {
    awaitF(asyncClient.delete[OperationResponse](resourcePath, requestFilter))
  }

  override def close(): Unit = {
    asyncClient.close()
  }
}

object HttpClient extends LogSupport {

  def defaultHttpClientRetry[Req: HttpRequestAdapter: ClassTag, Resp: HttpResponseAdapter]: RetryContext = {
    Retry
      .withBackOff(maxRetry = 10)
      .withResultClassifier(HttpClientException.classifyHttpResponse[Resp])
      .withErrorClassifier(HttpClientException.classifyExecutionFailure)
      .beforeRetry(defaultBeforeRetryAction[Req])
  }

  def defaultBeforeRetryAction[Req: HttpRequestAdapter: ClassTag](ctx: RetryContext): Any = {
    val cls = implicitly[ClassTag[Req]].runtimeClass

    val errorMessage = ctx.context match {
      case Some(r) if cls.isAssignableFrom(r.getClass) =>
        val adapter = implicitly[HttpRequestAdapter[Req]]
        val req     = r.asInstanceOf[Req]
        val path    = adapter.pathOf(req)
        s"Request to ${path} is failed: ${ctx.lastError.getMessage}"
      case _ =>
        s"Request is failed: ${ctx.lastError.getMessage}"
    }

    val extraWaitMillis =
      ctx.lastError match {
        case e: HttpClientException if e.status == HttpStatus.ServiceUnavailable_503 =>
          // Server is busy (e.g., S3 slow down). We need to reduce the request rate.
          (ctx.nextWaitMillis * 0.5).toLong // Add an extra wait
        case _ =>
          0
      }
    val nextWaitMillis = ctx.nextWaitMillis + extraWaitMillis
    warn(
      f"[${ctx.retryCount}/${ctx.maxRetry}] ${errorMessage}. Retry the request in ${nextWaitMillis / 1000.0}%.3f sec.")
    AddExtraRetryWait(extraWaitMillis.toInt)
  }

  def urlEncode(s: String): String = {
    URLEncoder.encode(s, "UTF-8")
  }

}
