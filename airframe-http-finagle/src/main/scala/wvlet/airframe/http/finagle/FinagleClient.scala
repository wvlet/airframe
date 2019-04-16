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
import com.twitter.finagle.service.{ReqRep, ResponseClass, ResponseClassifier}
import com.twitter.finagle.{Http, http}
import com.twitter.util._
import wvlet.airframe.codec.{MessageCodec, MessageCodecFactory}
import wvlet.airframe.control.ResultClass.{Failed, Succeeded}
import wvlet.airframe.control.{ResultClass, Retry}
import wvlet.airframe.http._

import scala.reflect.runtime.{universe => ru}

case class FinagleClientConfig(address: ServerAddress,
                               timeout: Duration = Duration(90, TimeUnit.SECONDS),
                               responseClassifier: ResponseClassifier = FinagleClient.defaultResponseClassifier)

class FinagleClient(config: FinagleClientConfig) extends HttpClient[Future, http.Request, http.Response] {
  private val client =
    Http.client
      .withResponseClassifier(config.responseClassifier)
      .newService(config.address.hostAndPort)

  override protected val retryer = HttpClient.defaultHttpClientRetryer

  override protected def sendImpl(req: http.Request): Future[http.Response] = {
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
    Await.result(f, config.timeout)
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
object FinagleClient {

  def newClient(hostAndPort: String): FinagleClient = {
    new FinagleClient(FinagleClientConfig(address = ServerAddress(hostAndPort)))
  }
  def newSyncClient(hostAndPort: String) = {
    new FinagleClient(FinagleClientConfig(address = ServerAddress(hostAndPort))).syncClient
  }

  def baseResponseClassifier: ResponseClassifier = {
    case ReqRep(_, Return(r: Response)) =>
      toFinagleResponseClassifier(HttpClientException.classifyHttpResponse(r))
    case ReqRep(_, Throw(ex)) =>
      toFinagleResponseClassifier(HttpClientException.classifyExecutionFailure(ex))
  }

  def defaultResponseClassifier: ResponseClassifier = {
    ResponseClassifier.RetryOnChannelClosed orElse
      ResponseClassifier.RetryOnTimeout orElse
      ResponseClassifier.RetryOnWriteExceptions orElse
      baseResponseClassifier
  }

  private[finagle] def toFinagleResponseClassifier(cls: ResultClass): ResponseClass = {
    cls match {
      case Succeeded =>
        ResponseClass.Success
      case Failed(isRetryable, _) =>
        if (isRetryable) {
          ResponseClass.RetryableFailure
        } else {
          ResponseClass.NonRetryableFailure
        }
    }
  }
}
