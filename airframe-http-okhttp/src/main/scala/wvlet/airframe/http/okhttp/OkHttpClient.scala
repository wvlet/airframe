package wvlet.airframe.http.okhttp

import java.time.Duration

import okhttp3.{HttpUrl, Request, RequestBody, Response}
import wvlet.airframe.codec.{MessageCodec, MessageCodecFactory}
import wvlet.airframe.control.Retry.RetryContext
import wvlet.airframe.http.HttpClient.urlEncode
import wvlet.airframe.http._
import wvlet.airframe.http.router.HttpResponseCodec
import wvlet.airframe.json.JSON.{JSONArray, JSONObject}
import wvlet.log.LogSupport

import scala.reflect.runtime.{universe => ru}
import scala.util.Try

case class OkHttpClientConfig(
    requestFilter: Request.Builder => Request.Builder = identity,
    timeout: Duration = Duration.ofSeconds(90),
    retryContext: RetryContext = OkHttpClient.defaultRetryContext
) {
  def noRetry: OkHttpClientConfig = {
    this.copy(retryContext = retryContext.noRetry)
  }

  def withMaxRetry(maxRetry: Int): OkHttpClientConfig = {
    this.copy(retryContext = retryContext.withMaxRetry(maxRetry))
  }

  def withBackOff(
      initialIntervalMillis: Int = 100,
      maxIntervalMillis: Int = 15000,
      multiplier: Double = 1.5
  ): OkHttpClientConfig = {
    this.copy(retryContext = retryContext.withBackOff(initialIntervalMillis, maxIntervalMillis, multiplier))
  }
}

class OkHttpClient(address: ServerAddress, config: OkHttpClientConfig)
    extends HttpSyncClient[Request.Builder, Response]
    with LogSupport {
  private[this] val client = {
    new okhttp3.OkHttpClient.Builder()
      .readTimeout(config.timeout)
      .addInterceptor(new OkHttpRetryInterceptor(config.retryContext))
      .build()
  }

  override def send(req: Request.Builder, requestFilter: Request.Builder => Request.Builder): Response = {
    // Apply the common filter in the config first, the apply the additional filter
    val request = requestFilter(config.requestFilter(req)).build()
    client.newCall(request).execute()
  }

  override def sendSafe(req: Request.Builder, requestFilter: Request.Builder => Request.Builder): Response = {
    Try(send(req, requestFilter)).recover {
      case e: HttpClientException =>
        e.response.asInstanceOf[HttpResponse[Response]].toRaw
    }.get
  }

  private val codecFactory  = MessageCodecFactory.defaultFactoryForJSON
  private val responseCodec = new HttpResponseCodec[Response]

  private def convert[A: ru.TypeTag](response: Response): A = {
    if (implicitly[ru.TypeTag[A]] == ru.typeTag[Response]) {
      // Can return the response as is
      response.asInstanceOf[A]
    } else {
      // Need a conversion
      val codec   = MessageCodec.of[A]
      val msgpack = responseCodec.toMsgPack(response)
      codec.unpack(msgpack)
    }
  }

  private def toJson[Resource: ru.TypeTag](resource: Resource): String = {
    val resourceCodec = codecFactory.of[Resource]
    // TODO: Support non-json content body
    val json = resourceCodec.toJson(resource)
    json
  }

  private def newRequestBuilder(path: String, queryParam: Option[String] = None): Request.Builder = {
    val httpUrl = HttpUrl
      .get(address.uri).newBuilder()
      .encodedPath(path)
      .encodedQuery(queryParam.orNull)
      .build()
    new Request.Builder().url(httpUrl)
  }

  override def get[Resource: ru.TypeTag](
      resourcePath: String,
      requestFilter: Request.Builder => Request.Builder
  ): Resource = {
    convert[Resource](send(newRequestBuilder(resourcePath), requestFilter))
  }

  override def getOps[Resource: ru.TypeTag, OperationResponse: ru.TypeTag](
      resourcePath: String,
      resource: Resource,
      requestFilter: Request.Builder => Request.Builder
  ): OperationResponse = {
    getResource[Resource, OperationResponse](resourcePath, resource, requestFilter)
  }

  override def getResource[ResourceRequest: ru.TypeTag, Resource: ru.TypeTag](
      resourcePath: String,
      resourceRequest: ResourceRequest,
      requestFilter: Request.Builder => Request.Builder
  ): Resource = {
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

    convert[Resource](send(newRequestBuilder(resourcePath, Some(queryParams.mkString("&"))), requestFilter))
  }

  override def list[OperationResponse: ru.TypeTag](
      resourcePath: String,
      requestFilter: Request.Builder => Request.Builder
  ): OperationResponse = {
    convert[OperationResponse](send(newRequestBuilder(resourcePath), requestFilter))
  }

  override def post[Resource: ru.TypeTag](
      resourcePath: String,
      resource: Resource,
      requestFilter: Request.Builder => Request.Builder
  ): Resource = {
    val r = newRequestBuilder(resourcePath)
      .post(RequestBody.create(ContentTypeJson, toJson(resource)))
    convert[Resource](send(r, requestFilter))
  }

  override def postRaw[Resource: ru.TypeTag](
      resourcePath: String,
      resource: Resource,
      requestFilter: Request.Builder => Request.Builder
  ): Response = {
    postOps[Resource, Response](resourcePath, resource, requestFilter)
  }

  override def postOps[Resource: ru.TypeTag, OperationResponse: ru.TypeTag](
      resourcePath: String,
      resource: Resource,
      requestFilter: Request.Builder => Request.Builder
  ): OperationResponse = {
    val r = newRequestBuilder(resourcePath)
      .post(RequestBody.create(ContentTypeJson, toJson(resource)))
    convert[OperationResponse](send(r, requestFilter))
  }

  override def put[Resource: ru.TypeTag](
      resourcePath: String,
      resource: Resource,
      requestFilter: Request.Builder => Request.Builder
  ): Resource = {
    val r = newRequestBuilder(resourcePath)
      .put(RequestBody.create(ContentTypeJson, toJson(resource)))
    convert[Resource](send(r, requestFilter))
  }

  override def putRaw[Resource: ru.TypeTag](
      resourcePath: String,
      resource: Resource,
      requestFilter: Request.Builder => Request.Builder
  ): Response = {
    putOps[Resource, Response](resourcePath, resource, requestFilter)
  }

  override def putOps[Resource: ru.TypeTag, OperationResponse: ru.TypeTag](
      resourcePath: String,
      resource: Resource,
      requestFilter: Request.Builder => Request.Builder
  ): OperationResponse = {
    val r = newRequestBuilder(resourcePath)
      .put(RequestBody.create(ContentTypeJson, toJson(resource)))
    convert[OperationResponse](send(r, requestFilter))
  }

  override def delete[OperationResponse: ru.TypeTag](
      resourcePath: String,
      requestFilter: Request.Builder => Request.Builder
  ): OperationResponse = {
    val r = newRequestBuilder(resourcePath)
      .delete()
    convert[OperationResponse](send(r, requestFilter))
  }

  override def deleteRaw(
      resourcePath: String,
      requestFilter: Request.Builder => Request.Builder
  ): Response = {
    delete[Response](resourcePath, requestFilter)
  }

  override def deleteOps[Resource: ru.TypeTag, OperationResponse: ru.TypeTag](
      resourcePath: String,
      resource: Resource,
      requestFilter: Request.Builder => Request.Builder
  ): OperationResponse = {
    val r = newRequestBuilder(resourcePath)
      .delete(RequestBody.create(ContentTypeJson, toJson(resource)))
    convert[OperationResponse](send(r, requestFilter))
  }

  override def patch[Resource: ru.TypeTag](
      resourcePath: String,
      resource: Resource,
      requestFilter: Request.Builder => Request.Builder
  ): Resource = {
    val r = newRequestBuilder(resourcePath)
      .patch(RequestBody.create(ContentTypeJson, toJson(resource)))
    convert[Resource](send(r, requestFilter))
  }

  override def patchRaw[Resource: ru.TypeTag](
      resourcePath: String,
      resource: Resource,
      requestFilter: Request.Builder => Request.Builder
  ): Response = {
    patchOps[Resource, Response](resourcePath, resource, requestFilter)
  }

  override def patchOps[Resource: ru.TypeTag, OperationResponse: ru.TypeTag](
      resourcePath: String,
      resource: Resource,
      requestFilter: Request.Builder => Request.Builder
  ): OperationResponse = {
    val r = newRequestBuilder(resourcePath)
      .patch(RequestBody.create(ContentTypeJson, toJson(resource)))
    convert[OperationResponse](send(r, requestFilter))
  }

  def close(): Unit = {}

}

object OkHttpClient {
  def defaultRetryContext: RetryContext = {
    HttpClient.defaultHttpClientRetry[Request, Response]
  }
  def newClient(
      hostAndPort: String,
      config: OkHttpClientConfig = OkHttpClientConfig()
  ): HttpSyncClient[Request.Builder, Response] = {
    new OkHttpClient(address = ServerAddress(hostAndPort), config)
  }
}
