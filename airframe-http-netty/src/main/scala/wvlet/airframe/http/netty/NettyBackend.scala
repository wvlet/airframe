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
import wvlet.airframe.http.{
  Http,
  HttpBackend,
  HttpFilter,
  HttpRequestAdapter,
  HttpStatus,
  RPCStatus,
  RxEndpoint,
  RxFilter,
  RxHttpBackend
}
import wvlet.airframe.rx.{Rx, RxStream}
import wvlet.log.LogSupport

import scala.collection.mutable
import scala.concurrent.{ExecutionContext, Future, Promise}

object NettyBackend extends HttpBackend[Request, Response, RxStream] with LogSupport { self =>
  private val rxBackend = new RxNettyBackend

  override protected implicit val httpRequestAdapter: HttpRequestAdapter[Request] =
    wvlet.airframe.http.HttpMessage.HttpMessageRequestAdapter

  override def name: String = "netty"

  override def newResponse(status: HttpStatus, content: String): Response = {
    Http.response(status).withContent(content)
  }

  def newRxEndpoint[U](body: Request => RxStream[Response], onClose: () => U = { () => }): RxEndpoint = new RxEndpoint {
    override private[http] def backend: RxHttpBackend = rxBackend
    override def apply(request: Request): RxStream[Response] = {
      body(request)
    }
    override def close(): Unit = {
      onClose()
    }
  }

  override def toFuture[A](a: A): RxStream[A] = {
    Rx.single(a)
  }

  override def toFuture[A](a: Future[A], e: ExecutionContext): RxStream[A] = {
    Rx.future(a)(e)
  }

  override def toScalaFuture[A](a: RxStream[A]): Future[A] = {
    val promise: Promise[A] = Promise()
    a.toRxStream
      .map { x =>
        promise.success(x)
      }
      .recover { case e: Throwable => promise.failure(e) }
    promise.future
  }

  override def filterAdapter[M[_]](filter: HttpFilter[_, _, M]): NettyBackend.Filter = {
    filter match {
      case f: NettyBackend.Filter => f
      case other =>
        throw RPCStatus.UNIMPLEMENTED_U8.newException(s"unsupported filter type: ${other.getClass}")
    }
  }

  override def rxFilterAdapter(filter: RxFilter): NettyBackend.Filter = {
    new NettyBackend.Filter {
      override protected def backend: HttpBackend[Request, Response, RxStream] = self
      override def apply(request: Request, context: NettyBackend.Context): RxStream[Response] = {
        filter(
          request,
          new RxEndpoint {
            override private[http] def backend: RxNettyBackend = rxBackend
            override def apply(request: Request): RxStream[Response] = {
              context(request)
            }
            override def close(): Unit = {}
          }
        )
      }
    }
  }

  override def wrapException(e: Throwable): RxStream[Response] = {
    Rx.exception(e)
  }

  override def isFutureType(x: Class[_]): Boolean = {
    classOf[Rx[_]].isAssignableFrom(x)
  }

  override def isRawResponseType(x: Class[_]): Boolean = {
    classOf[Response].isAssignableFrom(x)
  }

  override def mapF[A, B](f: RxStream[A], body: A => B): RxStream[B] = {
    f.map(body)
  }

  private lazy val tls =
    ThreadLocal.withInitial[collection.mutable.Map[String, Any]](() => mutable.Map.empty[String, Any])

  private def storage: collection.mutable.Map[String, Any] = tls.get()

  override def withThreadLocalStore(request: => RxStream[Response]): RxStream[Response] = {
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
