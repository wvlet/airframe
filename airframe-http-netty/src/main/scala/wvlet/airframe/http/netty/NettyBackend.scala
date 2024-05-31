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
import wvlet.airframe.http.*
import wvlet.airframe.rx.Rx
import wvlet.log.LogSupport

import scala.collection.mutable
import scala.concurrent.{Await, ExecutionContext, Future, Promise}
import scala.util.{Failure, Success}

object NettyBackend extends HttpBackend[Request, Response, Rx] with TLS with LogSupport { self =>
  private val rxBackend = new RxNettyBackend

  override protected implicit val httpRequestAdapter: HttpRequestAdapter[Request] =
    wvlet.airframe.http.HttpMessage.HttpMessageRequestAdapter

  override def name: String = "netty"

  override def newResponse(status: HttpStatus, content: String): Response = {
    Http.response(status).withContent(content)
  }

  override def toFuture[A](a: A): Rx[A] = {
    Rx.single(a)
  }

  override def toFuture[A](a: Future[A], ex: ExecutionContext): Rx[A] = {
    val v = Await.result(a, scala.concurrent.duration.Duration.Inf)
    Rx.single(v)
  }

  override def toScalaFuture[A](a: Rx[A]): Future[A] = {
    val promise: Promise[A] = Promise()
    val rx = a.transform {
      case Success(x)  => promise.success(x)
      case Failure(ex) => promise.failure(ex)
    }
    rx.run { effect => }
    promise.future
  }

  override def filterAdapter[M[_]](filter: HttpFilter[_, _, M]): NettyBackend.Filter = {
    filter.asInstanceOf[NettyBackend.Filter]
  }

  override def rxFilterAdapter(filter: RxHttpFilter): NettyBackend.Filter = {
    new NettyBackend.Filter {
      override protected def backend: HttpBackend[Request, Response, Rx] = self
      override def apply(request: Request, context: NettyBackend.Context): Rx[Response] = {
        filter(
          request,
          new RxHttpEndpoint {
            override def apply(request: Request): Rx[Response] = {
              context(request)
            }
          }
        )
      }
    }
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
    f.toRx.map(body)
  }

  override def withThreadLocalStore(request: => Rx[Response]): Rx[Response] = {
    //
    request
  }

  override def setThreadLocal[A](key: String, value: A): Unit = {
    setTLS(key, value)
  }

  override def getThreadLocal[A](key: String): Option[A] = {
    getTLS(key).map(_.asInstanceOf[A])
  }
}
