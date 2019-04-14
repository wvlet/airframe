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

import com.twitter.finagle.http.{Method, Request, Response}
import com.twitter.finagle.service.{ReqRep, ResponseClass, ResponseClassifier}
import com.twitter.finagle.{Http, http}
import com.twitter.util.{Await, Duration, Future, Return, Throw}
import wvlet.airframe.codec.MessageCodec
import wvlet.airframe.control.ResultClass
import wvlet.airframe.control.ResultClass.{Failed, Successful}
import wvlet.airframe.http.{
  HttpClient,
  HttpClientException,
  HttpMethod,
  HttpRequestAdapter,
  HttpResponseAdapter,
  HttpResponseCodec,
  ServerAddress
}

import scala.reflect.runtime.{universe => ru}

case class FinagleClientConfig(address: ServerAddress,
                               timeout: Duration = Duration(90, TimeUnit.SECONDS),
                               responseClassifier: ResponseClassifier = FinagleClient.defaultResponseClassifier)

class FinagleClient(config: FinagleClientConfig) extends HttpClient[Future, http.Request, http.Response] {
  private val client =
    Http.client
      .withResponseClassifier(config.responseClassifier)
      .newService(config.address.hostAndPort)

  override def send(req: http.Request): Future[http.Response] = {
    client(req)
  }

  def close: Unit = {
    client.close()
  }
  override protected def requestAdapter: HttpRequestAdapter[Request]    = FinagleHttpRequestAdapter
  override protected def responseAdapter: HttpResponseAdapter[Response] = FinagleHttpResponseAdapter

  override protected def newRequest(method: HttpMethod, path: String): Request = {
    // TODO add additional http headers
    Request(toFinagleHttpMethod(method), path)
  }

  override def await(req: Request): Response = {
    awaitF(send(req))
  }
  override protected def awaitF[A](f: Future[A]): A = {
    Await.result(f, config.timeout)
  }

  private val responseCodec = new HttpResponseCodec[Response]

  private def convert[A: ru.TypeTag](response: Future[Response]): Future[A] = {
    val codec = MessageCodec.of[A]
    response.map { r =>
      val msgpack = responseCodec.toMsgPack(r)
      codec.unpack(msgpack)
    }
  }

  override def get[A: ru.TypeTag](path: String): Future[A] = {
    convert[A](send(newRequest(HttpMethod.GET, path)))
  }
  override def post[A: ru.TypeTag, R: ru.TypeTag](path: String, data: A): Future[R] = {
    convert[R](send(newRequest(HttpMethod.POST, path)))
  }
  override def delete[R: ru.TypeTag](path: String): Future[R] = {
    convert[R](send(newRequest(HttpMethod.DELETE, path)))
  }
  override def delete[A: ru.TypeTag, R: ru.TypeTag](path: String, data: A): Future[R] = {
    convert[R](send(newRequest(HttpMethod.DELETE, path)))
  }
  override def put[R: ru.TypeTag](path: String): Future[R] = {
    convert[R](send(newRequest(HttpMethod.PUT, path)))
  }
  override def put[A: ru.TypeTag, R: ru.TypeTag](path: String, data: A): Future[R] = {
    convert[R](send(newRequest(HttpMethod.PUT, path)))
  }
}

/**
  *
  */
object FinagleClient {

  def newClient(hostAndPort: String): FinagleClient = {
    new FinagleClient(FinagleClientConfig(address = ServerAddress(hostAndPort)))
  }

  def baseResponseClassifier: ResponseClassifier = {
    case ReqRep(_, Return(r: Response)) =>
      toFinagleResponseClassifier(HttpClientException.defaultResponseClassifier(r))
    case ReqRep(_, Throw(ex)) =>
      toFinagleResponseClassifier(HttpClientException.defaultClientExceptionClassifier(ex))
  }

  def defaultResponseClassifier: ResponseClassifier = {
    ResponseClassifier.RetryOnChannelClosed orElse
      ResponseClassifier.RetryOnTimeout orElse
      ResponseClassifier.RetryOnWriteExceptions orElse
      baseResponseClassifier
  }

  private[finagle] def toFinagleResponseClassifier(cls: ResultClass): ResponseClass = {
    cls match {
      case Successful =>
        ResponseClass.Success
      case Failed(isRetryable) =>
        if (isRetryable) {
          ResponseClass.RetryableFailure
        } else {
          ResponseClass.NonRetryableFailure
        }
    }
  }
}
