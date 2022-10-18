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
package wvlet.airframe.http

import scala.concurrent.{ExecutionContext, ExecutionContextExecutor, Future}
import scala.language.higherKinds
import scala.util.control.NonFatal
import scala.language.higherKinds

/**
  * A base interface to implement http-server specific implementation
  */
trait HttpBackend[Req, Resp, F[_]] {
  self =>

  type Filter  = HttpFilter[Req, Resp, F]
  type Context = HttpContext[Req, Resp, F]

  protected implicit val httpRequestAdapter: HttpRequestAdapter[Req]

  def name: String
  def newResponse(status: HttpStatus, content: String = ""): Resp

  def toFuture[A](a: A): F[A]
  // Convert Scala's Future into the this backend's Future
  def toFuture[A](a: scala.concurrent.Future[A], e: ExecutionContext): F[A]
  def toScalaFuture[A](a: F[A]): scala.concurrent.Future[A]
  def wrapException(e: Throwable): F[Resp]
  def rescue(body: => F[Resp]): F[Resp] = {
    try {
      body
    } catch {
      case NonFatal(e) => wrapException(e)
    }
  }

  def isFutureType(x: Class[_]): Boolean
  def isScalaFutureType(x: Class[_]): Boolean = {
    classOf[scala.concurrent.Future[_]].isAssignableFrom(x)
  }
  // Returns true if the given class is the natively supported response type in this backend
  def isRawResponseType(x: Class[_]): Boolean

  // Map Future[A] into Future[B]
  def mapF[A, B](f: F[A], body: A => B): F[B]

  // Create a new Filter for this backend
  def newFilter(body: (Req, HttpContext[Req, Resp, F]) => F[Resp]): Filter = {
    HttpFilter.newFilter[Req, Resp, F](self, body)
  }
  // Create a new default filter just for processing preceding filters
  def defaultFilter: Filter = HttpFilter.defaultFilter(self)

  // Wrap the given filter to a backend-specific filter
  def filterAdapter[M[_]](filter: HttpFilter[_, _, M]): Filter = ???

  // Create a new default context that process the given request
  def newContext(body: Req => F[Resp]): Context =
    HttpContext.newContext[Req, Resp, F](self, body)

  // Prepare a thread-local holder for passing parameter values
  def withThreadLocalStore(request: => F[Resp]): F[Resp]

  // Set a thread-local context parameter value for a pre-defined key
  def setThreadLocalServerException[A](value: A): Unit =
    setThreadLocal(HttpBackend.TLS_KEY_SERVER_EXCEPTION, value)

  // Set a thread-local context parameter value
  def setThreadLocal[A](key: String, value: A): Unit

  // Get a thread-local context parameter
  def getThreadLocal[A](key: String): Option[A]
}

object HttpBackend {
  // Pre-defined keys for the thread-local storage
  private[http] val TLS_KEY_RPC              = "rpc"
  private[http] val TLS_KEY_SERVER_EXCEPTION = "server_exception"

  private[http] val BACKEND_FINAGLE = "finagle"
  private[http] val BACKEND_DEFAULT = "default"

  object DefaultBackend extends HttpBackend[HttpMessage.Request, HttpMessage.Response, scala.concurrent.Future] {
    // TODO: Should we customize execution Context?
    private[http] implicit lazy val executionContext: ExecutionContext = compat.defaultExecutionContext

    override protected implicit val httpRequestAdapter: HttpRequestAdapter[HttpMessage.Request] =
      HttpMessage.HttpMessageRequestAdapter

    override def name: String = BACKEND_DEFAULT
    override def newResponse(status: HttpStatus, content: String): HttpMessage.Response = {
      Http.response(status).withContent(content)
    }
    override def toFuture[A](a: A): Future[A]                              = Future(a)
    override def toFuture[A](a: Future[A], e: ExecutionContext): Future[A] = a
    override def toScalaFuture[A](a: Future[A]): Future[A]                 = a
    override def wrapException(e: Throwable): Future[HttpMessage.Response] =
      Future.failed(e)
    override def isFutureType(x: Class[_]): Boolean = {
      classOf[Future[_]].isAssignableFrom(x)
    }
    override def isRawResponseType(x: Class[_]): Boolean = {
      classOf[HttpMessage.Response].isAssignableFrom(x)
    }
    override def mapF[A, B](f: Future[A], body: A => B): Future[B] = {
      f.map(body)
    }
    override def withThreadLocalStore(request: => Future[HttpMessage.Response]): Future[HttpMessage.Response] = ???
    override def setThreadLocal[A](key: String, value: A): Unit = {
      // no-op
    }
    override def getThreadLocal[A](key: String): Option[A] = {
      None
    }
  }

}
