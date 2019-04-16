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
import com.twitter.finagle.{Http, http}
import com.twitter.util._
import wvlet.airframe.codec.{MessageCodec, MessageCodecFactory}
import wvlet.airframe.control.Retry.RetryContext
import wvlet.airframe.http._
import wvlet.log.LogSupport

import scala.reflect.runtime.{universe => ru}

case class FinagleClientConfig(initClient: Http.Client => Http.Client = identity,
                               timeout: Duration = Duration(90, TimeUnit.SECONDS),
                               retryer: RetryContext = HttpClient.defaultHttpClientRetryer[http.Request, http.Response])

class FinagleClient(address: ServerAddress, config: FinagleClientConfig)
    extends HttpClient[Future, http.Request, http.Response]
    with LogSupport {

  private val client = {
    val retryFilter   = new FinagleRetryFilter(config.retryer)
    val finagleClient = config.initClient(Http.client).newService(address.hostAndPort)

    retryFilter andThen finagleClient
  }

  override def send(req: Request): Future[Response] = {
    client(req)
  }

  def close: Unit = {
    client.close()
  }

  private def newRequest(method: HttpMethod, path: String): Request = {
    // TODO add additional http headers
    Request(toFinagleHttpMethod(method), path)
  }

  override private[http] def awaitF[A](f: Future[A]): A = {
    val r = Await.result(f, config.timeout)
    trace(r)
    r
  }

  private val codecFactory  = MessageCodecFactory.defaultFactory.withObjectMapCodec
  private val responseCodec = new HttpResponseCodec[Response]

  private def convert[A: ru.TypeTag](response: Future[Response]): Future[A] = {
    val codec = MessageCodec.of[A]
    response.map { r =>
      val msgpack = responseCodec.toMsgPack(r)
      codec.unpack(msgpack)
    }
  }

  private def toJson[Resource: ru.TypeTag](resource: Resource): String = {
    val resourceCodec = codecFactory.of[Resource]
    // TODO: Support non-json content body
    val json = resourceCodec.toJson(resource)
    json
  }

  override def get[Resource: ru.TypeTag](resourcePath: String): Future[Resource] = {
    convert[Resource](send(newRequest(HttpMethod.GET, resourcePath)))
  }
  override def list[OperationResponse: ru.TypeTag](resourcePath: String): Future[OperationResponse] = {
    convert[OperationResponse](send(newRequest(HttpMethod.GET, resourcePath)))
  }

  override def post[Resource: ru.TypeTag](resourcePath: String, resource: Resource): Future[Resource] = {
    val r = newRequest(HttpMethod.POST, resourcePath)
    r.setContentTypeJson()
    r.setContentString(toJson(resource))
    convert[Resource](send(r))
  }
  override def post[Resource: ru.TypeTag, OperationResponse: ru.TypeTag](
      resourcePath: String,
      resource: Resource): Future[OperationResponse] = {
    val r = newRequest(HttpMethod.POST, resourcePath)
    r.setContentTypeJson()
    r.setContentString(toJson(resource))
    convert[OperationResponse](send(r))
  }

  override def put[Resource: ru.TypeTag](resourcePath: String, resource: Resource): Future[Resource] = {
    val r = newRequest(HttpMethod.PUT, resourcePath)
    r.setContentTypeJson()
    r.setContentString(toJson(resource))
    convert[Resource](send(r))
  }
  override def put[Resource: ru.TypeTag, OperationResponse: ru.TypeTag](
      resourcePath: String,
      resource: Resource): Future[OperationResponse] = {
    val r = newRequest(HttpMethod.PUT, resourcePath)
    r.setContentTypeJson()
    r.setContentString(toJson(resource))
    convert[OperationResponse](send(r))
  }

  override def delete[OperationResponse: ru.TypeTag](resourcePath: String): Future[OperationResponse] = {
    convert[OperationResponse](send(newRequest(HttpMethod.DELETE, resourcePath)))
  }

}

/**
  *
  */
object FinagleClient extends LogSupport {

  def defaultConfig: FinagleClientConfig = FinagleClientConfig(
    initClient = { x: Http.Client =>
      x.withSessionQualifier.noFailureAccrual
    }
  )

  def newClient(hostAndPort: String, config: FinagleClientConfig = defaultConfig): FinagleClient = {
    new FinagleClient(address = ServerAddress(hostAndPort), config)
  }
  def newSyncClient(
      hostAndPort: String,
      config: FinagleClientConfig = defaultConfig): HttpSyncClient[Future, http.Request, http.Response] = {
    new FinagleClient(address = ServerAddress(hostAndPort), config).syncClient
  }
}
