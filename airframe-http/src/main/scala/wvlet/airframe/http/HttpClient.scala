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

import wvlet.airframe.codec.MessageCodec
import wvlet.airframe.control.Retry
import wvlet.airframe.control.Retry.RetryContext
import wvlet.airframe.json.JSON.{JSONArray, JSONObject}
import wvlet.airframe.surface.Surface
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
    * Send an HTTP request and get the response. It will throw an exception for non successful responses (after reaching the max retry limit)
    *
    * @throws HttpClientMaxRetryException if max retry reaches
    * @throws HttpClientException for non-retryable error is happend
    */
  def send(req: Req, requestFilter: Req => Req = identity): F[Resp]

  /**
    * Send an HTTP request and returns a response (or the last response if the request is retried)
    */
  def sendSafe(req: Req, requestFilter: Req => Req = identity): F[Resp]

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
  def getResource[ResourceRequest: ru.TypeTag, Resource: ru.TypeTag](
      resourcePath: String,
      resourceRequest: ResourceRequest,
      requestFilter: Req => Req = identity
  ): F[Resource]
  def getOps[Resource: ru.TypeTag, OperationResponse: ru.TypeTag](
      resourcePath: String,
      resource: Resource,
      requestFilter: Req => Req = identity
  ): F[OperationResponse] = getResource[Resource, OperationResponse](resourcePath, resource, requestFilter)
  def list[OperationResponse: ru.TypeTag](
      resourcePath: String,
      requestFilter: Req => Req = identity
  ): F[OperationResponse]
  def post[Resource: ru.TypeTag](
      resourcePath: String,
      resource: Resource,
      requestFilter: Req => Req = identity
  ): F[Resource]
  def postRaw[Resource: ru.TypeTag](
      resourcePath: String,
      resource: Resource,
      requestFilter: Req => Req = identity
  ): F[Resp]
  def postOps[Resource: ru.TypeTag, OperationResponse: ru.TypeTag](
      resourcePath: String,
      resource: Resource,
      requestFilter: Req => Req = identity
  ): F[OperationResponse]
  def put[Resource: ru.TypeTag](
      resourcePath: String,
      resource: Resource,
      requestFilter: Req => Req = identity
  ): F[Resource]
  def putRaw[Resource: ru.TypeTag](
      resourcePath: String,
      resource: Resource,
      requestFilter: Req => Req = identity
  ): F[Resp]
  def putOps[Resource: ru.TypeTag, OperationResponse: ru.TypeTag](
      resourcePath: String,
      resource: Resource,
      requestFilter: Req => Req = identity
  ): F[OperationResponse]
  def delete[OperationResponse: ru.TypeTag](
      resourcePath: String,
      requestFilter: Req => Req = identity
  ): F[OperationResponse]
  def deleteRaw(
      resourcePath: String,
      requestFilter: Req => Req = identity
  ): F[Resp]
  def deleteOps[Resource: ru.TypeTag, OperationResponse: ru.TypeTag](
      resourcePath: String,
      resource: Resource,
      requestFilter: Req => Req = identity
  ): F[OperationResponse]
  def patch[Resource: ru.TypeTag](
      resourcePath: String,
      resource: Resource,
      requestFilter: Req => Req = identity
  ): F[Resource]
  def patchRaw[Resource: ru.TypeTag](
      resourcePath: String,
      resource: Resource,
      requestFilter: Req => Req = identity
  ): F[Resp]
  def patchOps[Resource: ru.TypeTag, OperationResponse: ru.TypeTag](
      resourcePath: String,
      resource: Resource,
      requestFilter: Req => Req = identity
  ): F[OperationResponse]

  def syncClient: HttpSyncClient[Req, Resp] = new HttpSyncClientAdapter(this)
}

/**
  * A synchronous HTTP Client interface
  *
  * @tparam Req
  * @tparam Resp
  */
trait HttpSyncClient[Req, Resp] extends AutoCloseable {

  def send(req: Req, requestFilter: Req => Req = identity): Resp

  def sendSafe(req: Req, requestFilter: Req => Req = identity): Resp

  def get[Resource: ru.TypeTag](resourcePath: String, requestFilter: Req => Req = identity): Resource

  def getResource[ResourceRequest: ru.TypeTag, Resource: ru.TypeTag](
      resourcePath: String,
      resourceRequest: ResourceRequest,
      requestFilter: Req => Req = identity
  ): Resource = {
    getOps[ResourceRequest, Resource](resourcePath, resourceRequest, requestFilter)
  }

  def getOps[Resource: ru.TypeTag, OperationResponse: ru.TypeTag](
      resourcePath: String,
      resource: Resource,
      requestFilter: Req => Req = identity
  ): OperationResponse

  def list[OperationResponse: ru.TypeTag](
      resourcePath: String,
      requestFilter: Req => Req = identity
  ): OperationResponse

  def post[Resource: ru.TypeTag](
      resourcePath: String,
      resource: Resource,
      requestFilter: Req => Req = identity
  ): Resource
  def postRaw[Resource: ru.TypeTag](
      resourcePath: String,
      resource: Resource,
      requestFilter: Req => Req = identity
  ): Resp
  def postOps[Resource: ru.TypeTag, OperationResponse: ru.TypeTag](
      resourcePath: String,
      resource: Resource,
      requestFilter: Req => Req = identity
  ): OperationResponse

  def put[Resource: ru.TypeTag](
      resourcePath: String,
      resource: Resource,
      requestFilter: Req => Req = identity
  ): Resource
  def putRaw[Resource: ru.TypeTag](
      resourcePath: String,
      resource: Resource,
      requestFilter: Req => Req = identity
  ): Resp
  def putOps[Resource: ru.TypeTag, OperationResponse: ru.TypeTag](
      resourcePath: String,
      resource: Resource,
      requestFilter: Req => Req = identity
  ): OperationResponse

  def delete[OperationResponse: ru.TypeTag](
      resourcePath: String,
      requestFilter: Req => Req = identity
  ): OperationResponse
  def deleteRaw(
      resourcePath: String,
      requestFilter: Req => Req = identity
  ): Resp
  def deleteOps[Resource: ru.TypeTag, OperationResponse: ru.TypeTag](
      resourcePath: String,
      resource: Resource,
      requestFilter: Req => Req = identity
  ): OperationResponse

  def patch[Resource: ru.TypeTag](
      resourcePath: String,
      resource: Resource,
      requestFilter: Req => Req = identity
  ): Resource
  def patchRaw[Resource: ru.TypeTag](
      resourcePath: String,
      resource: Resource,
      requestFilter: Req => Req = identity
  ): Resp
  def patchOps[Resource: ru.TypeTag, OperationResponse: ru.TypeTag](
      resourcePath: String,
      resource: Resource,
      requestFilter: Req => Req = identity
  ): OperationResponse
}

/**
  * A synchronous HttpClient that awaits responses.
  *
  * @param asyncClient
  * @tparam F
  * @tparam Req
  * @tparam Resp
  */
class HttpSyncClientAdapter[F[_], Req, Resp](asyncClient: HttpClient[F, Req, Resp]) extends HttpSyncClient[Req, Resp] {
  protected def awaitF[A](f: F[A]): A = asyncClient.awaitF(f)

  /**
    * Send an HTTP request and get the response. It will throw an exception for non successful responses (after reaching the max retry)
    *
    * @throws HttpClientMaxRetryException if max retry reaches
    * @throws HttpClientException for non-retryable error is happend
    */
  override def send(req: Req, requestFilter: Req => Req = identity): Resp = awaitF(asyncClient.send(req, requestFilter))

  /**
    * Send an HTTP request and returns a response (or the last response if the request is retried)
    */
  override def sendSafe(req: Req, requestFilter: Req => Req = identity): Resp =
    awaitF(asyncClient.sendSafe(req, requestFilter))

  override def get[Resource: ru.TypeTag](resourcePath: String, requestFilter: Req => Req = identity): Resource = {
    awaitF(asyncClient.get[Resource](resourcePath, requestFilter))
  }

  override def getOps[Resource: ru.TypeTag, OperationResponse: ru.TypeTag](
      resourcePath: String,
      resource: Resource,
      requestFilter: Req => Req
  ): OperationResponse = {
    awaitF(asyncClient.getResource[Resource, OperationResponse](resourcePath, resource, requestFilter))
  }

  override def list[OperationResponse: ru.TypeTag](
      resourcePath: String,
      requestFilter: Req => Req = identity
  ): OperationResponse = {
    awaitF(asyncClient.list[OperationResponse](resourcePath, requestFilter))
  }

  override def post[Resource: ru.TypeTag](
      resourcePath: String,
      resource: Resource,
      requestFilter: Req => Req = identity
  ): Resource = {
    awaitF(asyncClient.post[Resource](resourcePath, resource, requestFilter))
  }

  override def postRaw[Resource: ru.TypeTag](
      resourcePath: String,
      resource: Resource,
      requestFilter: Req => Req = identity
  ): Resp = {
    awaitF(asyncClient.postRaw[Resource](resourcePath, resource, requestFilter))
  }

  override def postOps[Resource: ru.TypeTag, OperationResponse: ru.TypeTag](
      resourcePath: String,
      resource: Resource,
      requestFilter: Req => Req = identity
  ): OperationResponse = {
    awaitF(asyncClient.postOps[Resource, OperationResponse](resourcePath, resource, requestFilter))
  }

  override def put[Resource: ru.TypeTag](
      resourcePath: String,
      resource: Resource,
      requestFilter: Req => Req = identity
  ): Resource = {
    awaitF(asyncClient.put[Resource](resourcePath, resource, requestFilter))
  }

  override def putRaw[Resource: ru.TypeTag](
      resourcePath: String,
      resource: Resource,
      requestFilter: Req => Req = identity
  ): Resp = {
    awaitF(asyncClient.putRaw[Resource](resourcePath, resource, requestFilter))
  }

  override def putOps[Resource: ru.TypeTag, OperationResponse: ru.TypeTag](
      resourcePath: String,
      resource: Resource,
      requestFilter: Req => Req = identity
  ): OperationResponse = {
    awaitF(asyncClient.putOps[Resource, OperationResponse](resourcePath, resource, requestFilter))
  }

  override def delete[OperationResponse: ru.TypeTag](
      resourcePath: String,
      requestFilter: Req => Req = identity
  ): OperationResponse = {
    awaitF(asyncClient.delete[OperationResponse](resourcePath, requestFilter))
  }

  override def deleteRaw(
      resourcePath: String,
      requestFilter: Req => Req = identity
  ): Resp = {
    awaitF(asyncClient.deleteRaw(resourcePath, requestFilter))
  }

  override def deleteOps[Resource: ru.TypeTag, OperationResponse: ru.TypeTag](
      resourcePath: String,
      resource: Resource,
      requestFilter: Req => Req = identity
  ): OperationResponse = {
    awaitF(asyncClient.deleteOps[Resource, OperationResponse](resourcePath, resource, requestFilter))
  }

  override def patch[Resource: ru.TypeTag](
      resourcePath: String,
      resource: Resource,
      requestFilter: Req => Req = identity
  ): Resource = {
    awaitF(asyncClient.patch[Resource](resourcePath, resource, requestFilter))
  }

  override def patchRaw[Resource: ru.TypeTag](
      resourcePath: String,
      resource: Resource,
      requestFilter: Req => Req = identity
  ): Resp = {
    awaitF(asyncClient.patchRaw[Resource](resourcePath, resource, requestFilter))
  }

  override def patchOps[Resource: ru.TypeTag, OperationResponse: ru.TypeTag](
      resourcePath: String,
      resource: Resource,
      requestFilter: Req => Req = identity
  ): OperationResponse = {
    awaitF(asyncClient.patchOps[Resource, OperationResponse](resourcePath, resource, requestFilter))
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

    val nextWaitMillis = ctx.nextWaitMillis
    warn(
      f"[${ctx.retryCount}/${ctx.maxRetry}] ${errorMessage}. Retry the request in ${nextWaitMillis / 1000.0}%.3f sec."
    )
  }

  def urlEncode(s: String): String = {
    compat.urlEncode(s)
  }

  /**
    * Generate a GET resource url by embedding the resource object into query parameters
    * @param path
    * @param resource
    * @param resourceSurface
    * @tparam Resource
    * @return
    */
  private[http] def buildResourceUri[Resource](path: String, resource: Resource, resourceSurface: Surface): String = {
    val queryParams    = HttpClient.flattenResourceToQueryParams(resource, resourceSurface)
    val pathWithParams = new StringBuilder()
    pathWithParams.append(path)
    if (queryParams.nonEmpty) {
      val queryParamString = queryParams.entries.map(x => s"${x.key}=${x.value}").mkString("&")
      pathWithParams.append("?")
      pathWithParams.append(queryParamString)
    }
    pathWithParams.result()
  }

  /**
    * Flatten resource objects into query parameters for GET request
    * @param resource
    * @param resourceSurface
    * @tparam Resource
    * @return
    */
  private[http] def flattenResourceToQueryParams[Resource](
      resource: Resource,
      resourceSurface: Surface
  ): HttpMultiMap = {
    val resourceCodec = MessageCodec.ofSurface(resourceSurface).asInstanceOf[MessageCodec[Resource]]
    val resourceJson  = resourceCodec.toJSONObject(resource)

    val queryParams = HttpMultiMap.newBuilder
    resourceJson.v.map {
      case (k, j @ JSONArray(_)) =>
        queryParams += urlEncode(k) -> urlEncode(j.toJSON) // Flatten the JSON array value
      case (k, j @ JSONObject(_)) =>
        queryParams += urlEncode(k) -> urlEncode(j.toJSON) // Flatten the JSON object value
      case (k, other) =>
        queryParams += urlEncode(k) -> urlEncode(other.toString)
    }
    queryParams.result()
  }

}
