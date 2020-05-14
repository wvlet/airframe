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
import wvlet.airframe.http.HttpMessage.{Request, Response}

import scala.concurrent.Future
import scala.language.higherKinds

/***
  * Used for passing the subsequent actions to HttpFilter and for defining the leaf action of request processing chain.
  */
trait HttpContext[Req, Resp, F[_]] {
  protected def backend: HttpBackend[Req, Resp, F]

  /**
    * Process the preceding filters and get the resulting Future[Response]
    */
  def apply(request: Req): F[Resp]

  // Prepare a thread-local context parameter holder that can be used inside the body code block
  def withThreadLocalStore(body: => F[Resp]): F[Resp] = {
    backend.withThreadLocalStore(body)
  }

  /**
    * Set a thread local parameter
    */
  def setThreadLocal[A](key: String, value: A): Unit = {
    backend.setThreadLocal(key, value)
  }

  /**
    * Get a thread local parameter
    */
  def getThreadLocal[A](key: String): Option[A] = {
    backend.getThreadLocal(key)
  }
}

object HttpContext {

  def newContext[Req, Resp, F[_]](
      baseBackend: HttpBackend[Req, Resp, F],
      body: Req => F[Resp]
  ): HttpContext[Req, Resp, F] =
    new HttpContext[Req, Resp, F] {
      override protected def backend: HttpBackend[Req, Resp, F] = baseBackend
      override def apply(request: Req): F[Resp] = {
        backend.rescue {
          body(request)
        }
      }
    }

  private[http] class FilterAndThenContext[Req, Resp, F[_]](
      protected val backend: HttpBackend[Req, Resp, F],
      filter: HttpFilter[Req, Resp, F],
      context: HttpContext[Req, Resp, F]
  ) extends HttpContext[Req, Resp, F] {
    override def apply(request: Req): F[Resp] = {
      backend.rescue {
        filter.apply(request, new SafeHttpContext(backend, context))
      }
    }
  }

  /**
    * Wrapping Context execution with try-catch to return Future[Throwable] upon an error
    */
  private class SafeHttpContext[Req, Resp, F[_]](
      protected val backend: HttpBackend[Req, Resp, F],
      context: HttpContext[Req, Resp, F]
  ) extends HttpContext[Req, Resp, F] {
    override def apply(request: Req): F[Resp] = {
      backend.rescue {
        context.apply(request)
      }
    }
  }

  /**
    * Mock HttpContext for testing
    */
  private[http] def mockContext: HttpContext[Request, Response, Future] = {
    new HttpContext[Request, Response, Future] {
      override protected def backend: HttpBackend[
        Request,
        Response,
        Future
      ] = ???

      /**
        * Process the preceding filters and get the resulting Future[Response]
        */
      override def apply(
          request: Request
      ): Future[Response] = ???

      override def setThreadLocal[A](key: String, value: A): Unit = {
        // no-op
      }
      override def getThreadLocal[A](key: String): Option[A] = None
    }
  }
}
