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

import scala.language.higherKinds

trait HttpFilterType

/**
  * A filter interface to define actions for handling HTTP requests and responses
  */
trait HttpFilter[Req, Resp, F[_]] extends HttpFilterType { self =>

  protected def backend: HttpBackend[Req, Resp, F]

  // Wrap an exception and returns F[Exception]
  protected def wrapException(e: Throwable): F[Resp] = wrapException(e)

  /**
    * Implementations of HttpFilter must wrap an exception occurred in the filter.apply(request, context) with F[_]
    */
  protected def rescue(body: => F[Resp]): F[Resp] = backend.rescue(body)

  // Implementation to process the request. If this filter doesn't return any response, pass the request to the context(request)
  def apply(request: Req, context: HttpContext[Req, Resp, F]): F[Resp]

  // Add another filter
  def andThen(nextFilter: HttpFilter[Req, Resp, F]): HttpFilter[Req, Resp, F] =
    new HttpFilter.AndThen[Req, Resp, F](backend, this, nextFilter)

  // End the filter chain with the given HttpContext
  def andThen(context: HttpContext[Req, Resp, F]): HttpContext[Req, Resp, F] = context.prependFilter(this)
}

object HttpFilter {

  def newFilter[Req, Resp, F[_]](
      baseBackend: HttpBackend[Req, Resp, F],
      body: (Req, HttpContext[Req, Resp, F]) => F[Resp]
  ): HttpFilter[Req, Resp, F] = new HttpFilter[Req, Resp, F] {
    override protected def backend: HttpBackend[Req, Resp, F]                     = baseBackend
    override def apply(request: Req, context: HttpContext[Req, Resp, F]): F[Resp] = body(request, context)
  }

  def identity[Req, Resp, F[_]](backend: HttpBackend[Req, Resp, F]) = new Identity(backend)

  private[http] class AndThen[Req, Resp, F[_]](
      protected val backend: HttpBackend[Req, Resp, F],
      prev: HttpFilter[Req, Resp, F],
      next: HttpFilter[Req, Resp, F]
  ) extends HttpFilter[Req, Resp, F] {
    override def apply(request: Req, context: HttpContext[Req, Resp, F]): F[Resp] = {
      rescue(prev.apply(request, next.andThen(context)))
    }
  }

  private[http] class Identity[Req, Resp, F[_]](protected val backend: HttpBackend[Req, Resp, F])
      extends HttpFilter[Req, Resp, F] {
    override def apply(request: Req, context: HttpContext[Req, Resp, F]): F[Resp] = {
      rescue(context(request))
    }
  }

  /**
    * Wraps the filter to properly return Future[Throwable] upon an error
    */
  private[http] class SafeFilter[Req, Resp, F[_]](
      protected val backend: HttpBackend[Req, Resp, F],
      filter: HttpFilter[Req, Resp, F]
  ) extends HttpFilter[Req, Resp, F] {
    override def apply(request: Req, context: HttpContext[Req, Resp, F]): F[Resp] = {
      rescue(filter.apply(request, context))
    }
  }
}
