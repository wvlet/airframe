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

import java.util.concurrent.TimeUnit

import com.twitter.finagle.http.{Request, Response}
import com.twitter.finagle.{Http, Service, http}
import com.twitter.util._
import wvlet.airframe.codec.{MessageCodec, MessageCodecFactory}
import wvlet.airframe.control.Retry
import wvlet.airframe.control.Retry.RetryContext
import wvlet.airframe.http.HttpClient.urlEncode
import wvlet.airframe.http._
import wvlet.airframe.json.JSON.{JSONArray, JSONObject}
import wvlet.log.LogSupport

import scala.reflect.runtime.{universe => ru}
import scala.util.control.NonFatal

case class FinagleClientConfig(
    initClient: Http.Client => Http.Client = FinagleClient.defaultInitClient,
    requestFilter: http.Request => http.Request = identity,
    timeout: Duration = Duration(90, TimeUnit.SECONDS),
    retry: RetryContext = FinagleClient.defaultRetry
)

class FinagleClient(address: ServerAddress, config: FinagleClientConfig)
    extends HttpClient[Future, http.Request, http.Response]
    with LogSupport {

  private[this] val client = {
    val retryFilter                = new FinagleRetryFilter(config.retry)
    var finagleClient: Http.Client = config.initClient(Http.client)

    address.scheme.map {
      case "https" =>
        // Set TLS for http (443) connection
        finagleClient = finagleClient.withTls(address.host)
      case _ =>
    }
    debug(s"Starting a FinagleClient for ${address}")
    retryFilter andThen finagleClient.newService(address.hostAndPort)
  }

  // Use this method to access the native Finagle HTTP client
  def nativeClient: Service[Request, Response] = client

  override def send(req: Request, requestFilter: Request => Request = identity): Future[Response] = {
    // Apply the common filter in the config first, the apply the additional filter
    val request = requestFilter(config.requestFilter(req))
    // Add HOST header if missing
    if (request.host.isEmpty) {
      request.host = address.hostAndPort
    }
    client.apply(request)
  }

  /**
    * Send the request without applying any requestFilter
    */
  def sendRaw(req: Request): Future[Response] = {
    client.apply(req)
  }

  private def toRawUnsafe(resp: HttpResponse[_]): Response = {
    resp.asInstanceOf[HttpResponse[Response]].toRaw
  }

  override def sendSafe(req: Request, requestFilter: Request => Request = identity): Future[Response] = {
    try {
      send(req, requestFilter).rescue {
        case e: HttpClientException =>
          Future.value(toRawUnsafe(e.response))
      }
    } catch {
      case e: HttpClientException =>
        Future.value(toRawUnsafe(e.response))
      case NonFatal(e) =>
        Future.exception(e)
    }
  }

  def close: Unit = {
    debug(s"Closing FinagleClient for ${address}")
    client.close()
  }

  /**
    * Create a new Request
    */
  protected def newRequest(method: HttpMethod, path: String): Request = {
    Request(toFinagleHttpMethod(method), path)
  }

  /**
    * Await the result of Future[A]. It will throw an exception if some error happens
    */
  override private[http] def awaitF[A](f: Future[A]): A = {
    val r = Await.result(f, config.timeout)
    trace(r)
    r
  }

  private val codecFactory  = MessageCodecFactory.defaultFactory.withObjectMapCodec
  private val responseCodec = new HttpResponseCodec[Response]

  private def convert[A: ru.TypeTag](response: Future[Response]): Future[A] = {
    if (implicitly[ru.TypeTag[A]] == ru.typeTag[Response]) {
      // Can return the response as is
      response.asInstanceOf[Future[A]]
    } else {
      // Need a conversion
      val codec = MessageCodec.of[A]
      response
        .map { r =>
          val msgpack = responseCodec.toMsgPack(r)
          codec.unpack(msgpack)
        }
    }
  }

  private def toJson[Resource: ru.TypeTag](resource: Resource): String = {
    val resourceCodec = codecFactory.of[Resource]
    // TODO: Support non-json content body
    val json = resourceCodec.toJson(resource)
    json
  }

  override def get[Resource: ru.TypeTag](
      resourcePath: String,
      requestFilter: Request => Request = identity
  ): Future[Resource] = {
    convert[Resource](send(newRequest(HttpMethod.GET, resourcePath), requestFilter))
  }

  override def getResource[ResourceRequest: ru.TypeTag, Resource: ru.TypeTag](
      resourcePath: String,
      resourceRequest: ResourceRequest,
      requestFilter: Request => Request = identity
  ): Future[Resource] = {

    // Read resource as JSON
    val resourceRequestJsonValue = codecFactory.of[ResourceRequest].toJSONObject(resourceRequest)
    val queryParams: Seq[String] =
      resourceRequestJsonValue.v.map {
        case (k, j @ JSONArray(_)) =>
          s"${urlEncode(k)}=${urlEncode(j.toJSON)}" // Flatten the JSON array value
        case (k, j @ JSONObject(_)) =>
          s"${urlEncode(k)}=${urlEncode(j.toJSON)}" // Flatten the JSON object value
        case (k, other) =>
          s"${urlEncode(k)}=${urlEncode(other.toString)}"
      }

    // Build query strings
    val pathWithQueryParam = new StringBuilder
    pathWithQueryParam.append(resourcePath)
    pathWithQueryParam.append("?")
    pathWithQueryParam.append(queryParams.mkString("&"))

    convert[Resource](send(newRequest(HttpMethod.GET, pathWithQueryParam.result()), requestFilter))
  }

  override def list[OperationResponse: ru.TypeTag](
      resourcePath: String,
      requestFilter: Request => Request = identity
  ): Future[OperationResponse] = {
    convert[OperationResponse](send(newRequest(HttpMethod.GET, resourcePath), requestFilter))
  }

  override def post[Resource: ru.TypeTag](
      resourcePath: String,
      resource: Resource,
      requestFilter: Request => Request = identity
  ): Future[Resource] = {
    val r = newRequest(HttpMethod.POST, resourcePath)
    r.setContentTypeJson()
    r.setContentString(toJson(resource))
    convert[Resource](send(r, requestFilter))
  }
  override def postOps[Resource: ru.TypeTag, OperationResponse: ru.TypeTag](
      resourcePath: String,
      resource: Resource,
      requestFilter: Request => Request = identity
  ): Future[OperationResponse] = {
    val r = newRequest(HttpMethod.POST, resourcePath)
    r.setContentTypeJson()
    r.setContentString(toJson(resource))
    convert[OperationResponse](send(r, requestFilter))
  }

  override def put[Resource: ru.TypeTag](
      resourcePath: String,
      resource: Resource,
      requestFilter: Request => Request = identity
  ): Future[Resource] = {
    val r = newRequest(HttpMethod.PUT, resourcePath)
    r.setContentTypeJson()
    r.setContentString(toJson(resource))
    convert[Resource](send(r, requestFilter))
  }
  override def putOps[Resource: ru.TypeTag, OperationResponse: ru.TypeTag](
      resourcePath: String,
      resource: Resource,
      requestFilter: Request => Request = identity
  ): Future[OperationResponse] = {
    val r = newRequest(HttpMethod.PUT, resourcePath)
    r.setContentTypeJson()
    r.setContentString(toJson(resource))
    convert[OperationResponse](send(r, requestFilter))
  }

  override def delete[OperationResponse: ru.TypeTag](
      resourcePath: String,
      requestFilter: Request => Request = identity
  ): Future[OperationResponse] = {
    convert[OperationResponse](send(newRequest(HttpMethod.DELETE, resourcePath), requestFilter))
  }
  override def deleteOps[Resource: ru.TypeTag, OperationResponse: ru.TypeTag](
      resourcePath: String,
      resource: Resource,
      requestFilter: Request => Request = identity
  ): Future[OperationResponse] = {
    val r = newRequest(HttpMethod.DELETE, resourcePath)
    r.setContentTypeJson()
    r.setContentString(toJson(resource))
    convert[OperationResponse](send(r, requestFilter))
  }

  override def patch[Resource: ru.TypeTag](
      resourcePath: String,
      resource: Resource,
      requestFilter: Request => Request = identity
  ): Future[Resource] = {
    val r = newRequest(HttpMethod.PATCH, resourcePath)
    r.setContentTypeJson()
    r.setContentString(toJson(resource))
    convert[Resource](send(r, requestFilter))
  }
  override def patchOps[Resource: ru.TypeTag, OperationResponse: ru.TypeTag](
      resourcePath: String,
      resource: Resource,
      requestFilter: Request => Request = identity
  ): Future[OperationResponse] = {
    val r = newRequest(HttpMethod.PATCH, resourcePath)
    r.setContentTypeJson()
    r.setContentString(toJson(resource))
    convert[OperationResponse](send(r, requestFilter))
  }

}

/**
  *
  */
object FinagleClient extends LogSupport {

  def defaultConfig: FinagleClientConfig = FinagleClientConfig()

  // Config for tests
  def noRetryConfig: FinagleClientConfig = FinagleClientConfig(retry = Retry.withBackOff().withMaxRetry(0))

  def defaultInitClient: Http.Client => Http.Client = { x: Http.Client =>
    x.withSessionQualifier.noFailureAccrual
  }
  def defaultRetry: RetryContext = {
    HttpClient.defaultHttpClientRetry[http.Request, http.Response]
  }

  def newClient(hostAndPort: String, config: FinagleClientConfig = defaultConfig): FinagleClient = {
    new FinagleClient(address = ServerAddress(hostAndPort), config)
  }
  def newSyncClient(
      hostAndPort: String,
      config: FinagleClientConfig = defaultConfig
  ): FinagleSyncClient = {
    new FinagleClient(address = ServerAddress(hostAndPort), config).syncClient
  }
}
