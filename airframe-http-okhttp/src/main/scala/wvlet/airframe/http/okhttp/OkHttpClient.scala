package wvlet.airframe.http.okhttp

import java.time.Duration

import okhttp3.{HttpUrl, Request, RequestBody, Response}
import wvlet.airframe.codec.{MessageCodec, MessageCodecFactory}
import wvlet.airframe.http.HttpClient.urlEncode
import wvlet.airframe.http._
import wvlet.airframe.http.router.HttpResponseCodec
import wvlet.airframe.json.JSON.{JSONArray, JSONObject}
import wvlet.log.LogSupport

import scala.reflect.runtime.{universe => ru}
import scala.util.Try

case class OkHttpClientConfig(
  requestFilter: Request.Builder => Request.Builder = identity,
  timeout: Duration = Duration.ofSeconds(90)
)

class OkHttpClient(address: ServerAddress, config: OkHttpClientConfig)
  extends HttpClient[Try, Request.Builder, Response]
    with LogSupport {
  private[this] val client = {
    new okhttp3.OkHttpClient.Builder()
      .readTimeout(config.timeout)
      .build()
  }

  override def send(req: Request.Builder, requestFilter: Request.Builder => Request.Builder): Try[Response] = {
    // Apply the common filter in the config first, the apply the additional filter
    val request = requestFilter(config.requestFilter(req)).build()
    Try(client.newCall(request).execute())
  }

  private def toRawUnsafe(resp: HttpResponse[_]): Response = {
    resp.asInstanceOf[HttpResponse[Response]].toRaw
  }

  override def sendSafe(req: Request.Builder, requestFilter: Request.Builder => Request.Builder): Try[Response] = {
    send(req, requestFilter).recover {
      case e: HttpClientException => toRawUnsafe(e.response)
    }
  }

  override private[http] def awaitF[A](f: Try[A]): A = {
    val r = f.get
    trace(r)
    r
  }

  private val codecFactory  = MessageCodecFactory.defaultFactoryForJSON
  private val responseCodec = new HttpResponseCodec[Response]

  private def convert[A: ru.TypeTag](response: Try[Response]): Try[A] = {
    if (implicitly[ru.TypeTag[A]] == ru.typeTag[Response]) {
      // Can return the response as is
      response.asInstanceOf[Try[A]]
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

  private def newRequestBuilder(path: String): Request.Builder = {
    val httpUrl = HttpUrl
      .get(address.hostAndPort).newBuilder()
      .encodedPath(path)
      .build()
    new Request.Builder().url(httpUrl)
  }

  override def get[Resource: ru.TypeTag](
      resourcePath: String,
      requestFilter: Request.Builder => Request.Builder): Try[Resource] = {
    convert[Resource](send(newRequestBuilder(resourcePath), requestFilter))
  }

  override def getResource[ResourceRequest: ru.TypeTag, Resource: ru.TypeTag](
      resourcePath: String,
      resourceRequest: ResourceRequest,
      requestFilter: Request.Builder => Request.Builder): Try[Resource] = {
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

    convert[Resource](send(newRequestBuilder(pathWithQueryParam.result()), requestFilter))
  }

  override def list[OperationResponse: ru.TypeTag](
      resourcePath: String,
      requestFilter: Request.Builder => Request.Builder): Try[OperationResponse] = {
    convert[OperationResponse](send(newRequestBuilder(resourcePath), requestFilter))
  }

  override def post[Resource: ru.TypeTag](
      resourcePath: String,
      resource: Resource,
      requestFilter: Request.Builder => Request.Builder): Try[Resource] = {
    val r = newRequestBuilder(resourcePath)
      .post(RequestBody.create(ContentTypeJson, toJson(resource)))
    convert[Resource](send(r, requestFilter))
  }

  override def postRaw[Resource: ru.TypeTag](
      resourcePath: String,
      resource: Resource,
      requestFilter: Request.Builder => Request.Builder): Try[Response] = {
    postOps[Resource, Response](resourcePath, resource, requestFilter)
  }

  override def postOps[Resource: ru.TypeTag, OperationResponse: ru.TypeTag](
      resourcePath: String,
      resource: Resource,
      requestFilter: Request.Builder => Request.Builder): Try[OperationResponse] = {
    val r = newRequestBuilder(resourcePath)
      .post(RequestBody.create(ContentTypeJson, toJson(resource)))
    convert[OperationResponse](send(r, requestFilter))
  }

  override def put[Resource: ru.TypeTag](
      resourcePath: String,
      resource: Resource,
      requestFilter: Request.Builder => Request.Builder): Try[Resource] = {
    val r = newRequestBuilder(resourcePath)
      .put(RequestBody.create(ContentTypeJson, toJson(resource)))
    convert[Resource](send(r, requestFilter))
  }

  override def putRaw[Resource: ru.TypeTag](
      resourcePath: String,
      resource: Resource,
      requestFilter: Request.Builder => Request.Builder): Try[Response] = {
    putOps[Resource, Response](resourcePath, resource, requestFilter)
  }

  override def putOps[Resource: ru.TypeTag, OperationResponse: ru.TypeTag](
      resourcePath: String,
      resource: Resource,
      requestFilter: Request.Builder => Request.Builder): Try[OperationResponse] = {
    val r = newRequestBuilder(resourcePath)
      .put(RequestBody.create(ContentTypeJson, toJson(resource)))
    convert[OperationResponse](send(r, requestFilter))
  }

  override def delete[OperationResponse: ru.TypeTag](
      resourcePath: String,
      requestFilter: Request.Builder => Request.Builder): Try[OperationResponse] = {
    val r = newRequestBuilder(resourcePath)
      .delete()
    convert[OperationResponse](send(r, requestFilter))
  }

  override def deleteRaw(
      resourcePath: String,
      requestFilter: Request.Builder => Request.Builder): Try[Response] = {
    delete[Response](resourcePath, requestFilter)
  }

  override def deleteOps[Resource: ru.TypeTag, OperationResponse: ru.TypeTag](
      resourcePath: String,
      resource: Resource,
      requestFilter: Request.Builder => Request.Builder): Try[OperationResponse] = {
    val r = newRequestBuilder(resourcePath)
      .delete(RequestBody.create(ContentTypeJson, toJson(resource)))
    convert[OperationResponse](send(r, requestFilter))
  }

  override def patch[Resource: ru.TypeTag](
      resourcePath: String,
      resource: Resource,
      requestFilter: Request.Builder => Request.Builder): Try[Resource] = {
    val r = newRequestBuilder(resourcePath)
      .patch(RequestBody.create(ContentTypeJson, toJson(resource)))
    convert[Resource](send(r, requestFilter))
  }

  override def patchRaw[Resource: ru.TypeTag](
      resourcePath: String,
      resource: Resource,
      requestFilter: Request.Builder => Request.Builder): Try[Response] = {
    patchOps[Resource, Response](resourcePath, resource, requestFilter)
  }

  override def patchOps[Resource: ru.TypeTag, OperationResponse: ru.TypeTag](
      resourcePath: String,
      resource: Resource,
      requestFilter: Request.Builder => Request.Builder): Try[OperationResponse] = {
    val r = newRequestBuilder(resourcePath)
      .patch(RequestBody.create(ContentTypeJson, toJson(resource)))
    convert[OperationResponse](send(r, requestFilter))
  }

  def close(): Unit = {}

}

object OkHttpClient {
  def newOkHttpClient(hostAndPort: String, config: OkHttpClientConfig = OkHttpClientConfig()): OkHttpClient = {
    new OkHttpClient(address = ServerAddress(hostAndPort), config)
  }
  def newClient(hostAndPort: String, config: OkHttpClientConfig = OkHttpClientConfig()): HttpSyncClient[Try, Request.Builder, Response] = {
    new OkHttpClient(address = ServerAddress(hostAndPort), config).syncClient
  }
}
