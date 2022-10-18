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
package wvlet.airframe.http.netty

import wvlet.airframe.http.HttpMessage.{Request, Response}
import wvlet.airframe.http.{Http, HttpBackend, HttpRequestAdapter, HttpStatus}
import wvlet.airframe.rx.Rx

import scala.collection.mutable
import scala.concurrent.{ExecutionContext, Future, Promise}

object NettyBackend extends HttpBackend[Request, Response, Rx] {
  override protected implicit val httpRequestAdapter: HttpRequestAdapter[Request] =
    wvlet.airframe.http.HttpMessage.HttpMessageRequestAdapter

  override def name: String = "netty"

  override def newResponse(status: HttpStatus, content: String): Response = {
    Http.response(status).withContent(content)
  }

  override def toFuture[A](a: A): Rx[A] = {
    Rx.single(a)
  }

  override def toFuture[A](a: Future[A], e: ExecutionContext): Rx[A] = {
    Rx.future(a)(e)
  }

  override def toScalaFuture[A](a: Rx[A]): Future[A] = {
    val promise: Promise[A] = Promise()
    a.toRxStream
      .map { x =>
        promise.success(x)
      }
      .recover { case e: Throwable => promise.failure(e) }
    promise.future
  }

  override def wrapException(e: Throwable): Rx[Response] = {
    Rx.exception(e)
  }

  override def isFutureType(x: Class[_]): Boolean = {
    classOf[Rx[_]].isAssignableFrom(x)
  }

  override def isRawResponseType(x: Class[_]): Boolean = {
    classOf[Response].isAssignableFrom(x)
  }

  override def mapF[A, B](f: Rx[A], body: A => B): Rx[B] = {
    f.toRxStream.map(body)
  }

  private lazy val tls =
    ThreadLocal.withInitial[collection.mutable.Map[String, Any]](() => mutable.Map.empty[String, Any])

  private def storage: collection.mutable.Map[String, Any] = tls.get()

  override def withThreadLocalStore(request: => Rx[Response]): Rx[Response] = {
    //
    request
  }

  override def setThreadLocal[A](key: String, value: A): Unit = {
    storage.put(key, value)
  }

  override def getThreadLocal[A](key: String): Option[A] = {
    storage.get(key).asInstanceOf[Option[A]]
  }
}
