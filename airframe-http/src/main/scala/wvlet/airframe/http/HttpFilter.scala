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

import wvlet.airframe.http.HttpFilter.Identity

import scala.language.higherKinds

/**
  * A filter interface to define actions for handling HTTP requests and responses
  */
trait HttpFilter[Req, Resp, F[_]] { self =>
  def apply(request: Req, context: HttpContext[Req, Resp, F]): F[Resp]

  // Add another filter:
  def andThen(nextFilter: HttpFilter[Req, Resp, F]): HttpFilter[Req, Resp, F] = {
    HttpFilter.AndThen(this, nextFilter)
  }

  private[http] def andThen(context: HttpContext[Req, Resp, F]): HttpContext[Req, Resp, F] = {
    new HttpContext[Req, Resp, F] {
      override def apply(request: Req): F[Resp] = {
        self.apply(request, context)
      }
    }
  }
}

object HttpFilter {
  class Identity[Req, Resp, F[_]]() extends HttpFilter[Req, Resp, F] {
    override def apply(request: Req, context: HttpContext[Req, Resp, F]): F[Resp] = {
      context(request)
    }

    override def andThen(nextFilter: HttpFilter[Req, Resp, F]): HttpFilter[Req, Resp, F] = {
      nextFilter
    }
  }

  case class AndThen[Req, Resp, F[_]](prev: HttpFilter[Req, Resp, F], next: HttpFilter[Req, Resp, F])
      extends HttpFilter[Req, Resp, F] {
    override def apply(request: Req, context: HttpContext[Req, Resp, F]): F[Resp] = {
      prev.apply(request, next.andThen(context))
    }
  }
}

/***
  * Used for passing the subsequent actions to HttpFilter
  */
trait HttpContext[Req, Resp, F[_]] {
  def apply(request: Req): F[Resp]
}
