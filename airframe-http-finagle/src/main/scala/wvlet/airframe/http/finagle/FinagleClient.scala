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
import wvlet.airframe.Design
import wvlet.airframe.codec.{MessageCodec, MessageCodecFactory}
import wvlet.airframe.control.Retry.RetryContext
import wvlet.airframe.http._
import wvlet.airframe.http.HttpResponseBodyCodec
import wvlet.airframe.surface.Surface
import wvlet.log.LogSupport

import scala.reflect.runtime.{universe => ru}
import scala.util.control.NonFatal

case class FinagleClientConfig(
    initClient: Http.Client => Http.Client = FinagleClient.defaultInitClient,
    requestFilter: http.Request => http.Request = identity,
    timeout: Duration = Duration(90, TimeUnit.SECONDS),
    retryContext: RetryContext = FinagleClient.defaultRetryContext,
    codecFactory: MessageCodecFactory = MessageCodecFactory.defaultFactoryForJSON
) {
  def withInitializer(initClient: Http.Client => Http.Client): FinagleClientConfig = {
    this.copy(initClient = initClient)
  }
  def withRetryContext(retryContext: RetryContext): FinagleClientConfig = {
    this.copy(retryContext = retryContext)
  }

  /**
    * Customize the retry policy
    */
  def withRetry(retryContextFilter: RetryContext => RetryContext): FinagleClientConfig = {
    this.copy(retryContext = retryContextFilter(retryContext))
  }
  def withTimeout(timeout: Duration): FinagleClientConfig = {
    this.copy(timeout = timeout)
  }
  def withRequestFilter(requestFilter: http.Request => http.Request): FinagleClientConfig = {
    this.copy(requestFilter = requestFilter)
  }

  def noRetry: FinagleClientConfig = {
    this.copy(retryContext = retryContext.noRetry)
  }

  def withMaxRetry(maxRetry: Int): FinagleClientConfig = {
    this.copy(retryContext = retryContext.withMaxRetry(maxRetry))
  }

  def withBackOff(
      initialIntervalMillis: Int = 100,
      maxIntervalMillis: Int = 15000,
      multiplier: Double = 1.5
  ): FinagleClientConfig = {
    withRetry(retryContext =>
      retryContext
        .withBackOff(initialIntervalMillis, maxIntervalMillis, multiplier)
    )
  }

  def withJitter(
      initialIntervalMillis: Int = 100,
      maxIntervalMillis: Int = 15000,
      multiplier: Double = 1.5
  ): FinagleClientConfig = {
    withRetry(retryContext =>
      retryContext
        .withJitter(initialIntervalMillis, maxIntervalMillis, multiplier)
    )
  }

  def withCodecFactory(newCodecFactory: MessageCodecFactory): FinagleClientConfig = {
    this.copy(codecFactory = newCodecFactory)
  }

  def asyncClientDesign: Design = {
    Design.newDesign
      .bind[FinagleClient]
      .toProvider { (server: FinagleServer) =>
        this.newClient(server.localAddress)
      }
  }

  def syncClientDesign: Design = {
    Design.newDesign
      .bind[FinagleSyncClient]
      .toProvider { (server: FinagleServer) =>
        this.newSyncClient(server.localAddress)
      }
  }

  def newClient(hostAndPort: String): FinagleClient = {
    FinagleClient.newClient(hostAndPort, this)
  }

  def newSyncClient(hostAndPort: String): FinagleSyncClient = {
    FinagleClient.newSyncClient(hostAndPort, this)
  }
}

class FinagleClient(address: ServerAddress, config: FinagleClientConfig)
    extends HttpClient[Future, http.Request, http.Response]
    with LogSupport {

  // Use the bridged scheduler by default to avoid blocking at Await.result in SyncClient.
  // The forkjoin scheduler was unstable in CI
  Option(System.getProperty("com.twitter.finagle.exp.scheduler")).getOrElse {
    System.setProperty("com.twitter.finagle.exp.scheduler", "bridged")
  }

  private[this] val client = {
    val retryFilter                = new FinagleRetryFilter(config.retryContext)
    var finagleClient: Http.Client = config.initClient(Http.client)

    address.scheme match {
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
      send(req, requestFilter).rescue { case e: HttpClientException =>
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
  protected def newRequest(method: String, path: String): Request = {
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

  // make sure using Map output
  private val codecFactory  = config.codecFactory.withMapOutput
  private val responseCodec = new HttpResponseBodyCodec[Response]

  private def convert[A: ru.TypeTag](response: Future[Response]): Future[A] = {
    if (implicitly[ru.TypeTag[A]] == ru.typeTag[Response]) {
      // Can return the response as is
      response.asInstanceOf[Future[A]]
    } else {
      // Need a conversion
      val codec = codecFactory.of[A]
      response
        .map { r =>
          val msgpack = responseCodec.toMsgPack(r)
          try {
            codec.unpack(msgpack)
          } catch {
            case NonFatal(e) =>
              val msg =
                s"Failed to parse the response body ${r}: ${r.contentString}"
              warn(msg)
              throw new HttpClientException(
                r,
                HttpStatus.ofCode(r.statusCode),
                msg,
                e
              )
          }
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

    val resourceSurface = Surface.of[ResourceRequest]
    val path            = HttpClient.buildResourceUri(resourcePath, resourceRequest, resourceSurface)
    convert[Resource](send(newRequest(HttpMethod.GET, path), requestFilter))
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
  override def postRaw[Resource: ru.TypeTag](
      resourcePath: String,
      resource: Resource,
      requestFilter: Request => Request = identity
  ): Future[http.Response] = {
    postOps[Resource, http.Response](resourcePath, resource, requestFilter)
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
  override def putRaw[Resource: ru.TypeTag](
      resourcePath: String,
      resource: Resource,
      requestFilter: Request => Request = identity
  ): Future[http.Response] = {
    putOps[Resource, http.Response](resourcePath, resource, requestFilter)
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
  override def deleteRaw(
      resourcePath: String,
      requestFilter: Request => Request = identity
  ): Future[http.Response] = {
    delete[http.Response](resourcePath, requestFilter)
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
  override def patchRaw[Resource: ru.TypeTag](
      resourcePath: String,
      resource: Resource,
      requestFilter: Request => Request = identity
  ): Future[http.Response] = {
    patchOps[Resource, http.Response](resourcePath, resource, requestFilter)
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
  */
object FinagleClient extends LogSupport {
  def defaultInitClient: Http.Client => Http.Client = { (x: Http.Client) =>
    x.withSessionQualifier.noFailureAccrual
  }
  def defaultRetryContext: RetryContext = {
    HttpClient.defaultHttpClientRetry[http.Request, http.Response]
  }
  def newClient(hostAndPort: String, config: FinagleClientConfig = FinagleClientConfig()): FinagleClient = {
    new FinagleClient(address = ServerAddress(hostAndPort), config)
  }
  def newSyncClient(
      hostAndPort: String,
      config: FinagleClientConfig = FinagleClientConfig()
  ): FinagleSyncClient = {
    new FinagleClient(address = ServerAddress(hostAndPort), config).syncClient
  }
}
