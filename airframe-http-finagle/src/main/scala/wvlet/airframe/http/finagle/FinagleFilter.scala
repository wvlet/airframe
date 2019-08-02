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

import com.twitter.finagle.http.{Request, Response}
import com.twitter.util.Future
import wvlet.airframe.http.{HttpContext, HttpFilter}
import wvlet.log.LogSupport

import scala.util.control.NonFatal

abstract class FinagleFilter extends HttpFilter[Request, Response, Future] {
  override def wrapException(e: Throwable): Future[Response] = {
    Future.exception(e)
  }

  override def andThen(nextFilter: HttpFilter[Request, Response, Future]): FinagleFilter = {
    FinagleFilter.FinagleAndThen(this, nextFilter)
  }

  override def andThen(context: HttpContext[Request, Response, Future]): HttpContext[Request, Response, Future] = {
    FinagleFilter.AndThenHttpContext(this, context)
  }

}

object FinagleFilter {

  case object Identity extends FinagleFilter {
    override def apply(request: Request, context: HttpContext[Request, Response, Future]): Future[Response] = {
      rescue(context(request))
    }
    override def andThen(nextFilter: HttpFilter[Request, Response, Future]): FinagleFilter = {
      WrappedFilter(nextFilter)
    }
  }

  private case class WrappedFilter(filter: HttpFilter[Request, Response, Future]) extends FinagleFilter {
    def apply(request: Request, context: HttpContext[Request, Response, Future]): Future[Response] = {
      rescue(filter.apply(request, context))
    }
  }

  private case class FinagleAndThen(prev: FinagleFilter, next: HttpFilter[Request, Response, Future])
      extends FinagleFilter
      with LogSupport {
    override def apply(request: Request, context: HttpContext[Request, Response, Future]): Future[Response] = {
      rescue(prev.apply(request, next.andThen(context)))
    }
  }

  private def rescue(body: => Future[Response]): Future[Response] = {
    try {
      body
    } catch {
      case NonFatal(e) => Future.exception(e)
    }
  }

  private case class AndThenHttpContext(filter: FinagleFilter, context: HttpContext[Request, Response, Future])
      extends HttpContext[Request, Response, Future] {
    override def apply(request: Request): Future[Response] = {
      rescue {
        filter.apply(request, new WrappedHttpContext(context))
      }
    }
  }

  private class WrappedHttpContext(context: HttpContext[Request, Response, Future])
      extends HttpContext[Request, Response, Future] {
    override def apply(request: Request): Future[Response] = {
      rescue {
        context.apply(request)
      }
    }
  }

}
