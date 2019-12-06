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

import wvlet.airframe.http.router.{HttpEndpointExecutionContextBase, ResponseHandler, RouteMatch}

import scala.concurrent.ExecutionContext
import scala.language.higherKinds
import scala.util.control.NonFatal

/**
  * A base interface to implement http-server specific implementation
  */
trait HttpBackend[Req, Resp, F[_]] { self =>
  type Filter  = HttpFilter[Req, Resp, F]
  type Context = HttpContext[Req, Resp, F]

  protected implicit val httpRequestAdapter: HttpRequestAdapter[Req]

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

  def newFilter(body: (Req, HttpContext[Req, Resp, F]) => F[Resp]): Filter = {
    HttpFilter.newFilter[Req, Resp, F](self, body)
  }

  // Prepare a thread-local holder for passing parameter values
  def withThreadLocalStore(request: => F[Resp]): F[Resp]

  // Set a thread-local context parameter value
  def setThreadLocal[A](key: String, value: A): Unit

  // Get a thread-local context parameter
  def getThreadLocal[A](key: String): Option[A]

  // Create a new default context that process the given request
  def newContext(body: Req => F[Resp]): HttpContext[Req, Resp, F] = {
    new HttpContext[Req, Resp, F] {
      override protected def backend: HttpBackend[Req, Resp, F] = self
      override def apply(request: Req): F[Resp] = {
        body(request)
      }
    }
  }

  // Create a new context for the matched route with its controller
  def newControllerContext(
      routeMatch: RouteMatch,
      responseHandler: ResponseHandler[Req, Resp],
      controller: Any
  ): HttpEndpointExecutionContextBase[Req, Resp, F] = {
    new HttpEndpointExecutionContextBase[Req, Resp, F](this, routeMatch, controller, responseHandler)
  }
}
