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

  // Add another filter:
  def andThen(nextFilter: HttpFilter[Req, Resp, F]): HttpFilter[Req, Resp, F]

  // End the filter chain with the given HttpContext
  def andThen(context: HttpContext[Req, Resp, F]): HttpContext[Req, Resp, F]
}

/***
  * Used for passing the subsequent actions to HttpFilter
  */
trait HttpContext[Req, Resp, F[_]] {
  def apply(request: Req): F[Resp]
}
