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
import scala.util.control.NonFatal

trait HttpFilterType

/**
  * A filter interface to define actions for handling HTTP requests and responses
  */
trait HttpFilter[Req, Resp, F[_]] extends HttpFilterType { self =>

  // Wrap an exception and returns F[Exception]
  protected def wrapException(e: Throwable): F[Resp]

  /**
    * Implementations of HttpFilter must wrap an exception occurred in the filter.apply(request, context) with F[_]
    */
  protected def rescue(body: => F[Resp]): F[Resp] = {
    try {
      body
    } catch {
      case NonFatal(e) => wrapException(e)
    }
  }

  // Implementation to process the request. If this filter doesn't return any response, pass the request to the context(request)
  def apply(request: Req, context: HttpContext[Req, Resp, F]): F[Resp]

  // Add another filter
  def andThen(nextFilter: HttpFilter[Req, Resp, F]): HttpFilter[Req, Resp, F]

  // End the filter chain with the given HttpContext
  def andThen(context: HttpContext[Req, Resp, F]): HttpContext[Req, Resp, F]
}

object HttpFilter {

  /**
    * A base class for generating filters for Finagle or other HTTP server backend
    * @tparam Req
    * @tparam Resp
    * @tparam F
    */
  trait HttpFilterFactory[Req, Resp, F[_]] { factory =>
    type Filter  = HttpFilter[Req, Resp, F]
    type Context = HttpContext[Req, Resp, F]

    protected def rescue(body: => F[Resp]): F[Resp] = {
      try {
        body
      } catch {
        case NonFatal(e) => wrapException(e)
      }
    }
    def wrapException(e: Throwable): F[Resp]
    def newFilter(body: (Req, HttpContext[Req, Resp, F]) => F[Resp]): Filter

    abstract class HttpFilterBase extends Filter {
      override protected def wrapException(e: Throwable): F[Resp] = factory.wrapException(e)
      override def andThen(nextFilter: Filter): Filter = {
        AndThen(this, nextFilter)
      }
      override def andThen(context: Context): Context = {
        context.prependFilter(this)
      }
    }

    case object Identity extends HttpFilterBase {
      override def apply(request: Req, context: Context): F[Resp] = {
        this.rescue(context(request))
      }
      override def andThen(nextFilter: Filter): Filter = {
        new WrappedFilter(nextFilter)
      }
    }

    class WrappedFilter(filter: Filter) extends HttpFilterBase {
      override def apply(request: Req, context: Context): F[Resp] = {
        this.rescue(filter.apply(request, context))
      }
    }

    case class AndThen(prev: Filter, next: Filter) extends HttpFilterBase {
      override def apply(request: Req, context: Context): F[Resp] = {
        rescue(prev.apply(request, next.andThen(context)))
      }
    }
  }
}
