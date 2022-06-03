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
package wvlet.airframe.http.client

import wvlet.airframe.http.HttpMessage.{Request, Response}

/**
  * Http client request and response interceptor interface.
  *
  * This can be used for client-side logging, request/response rewriting, caching, etc.
  */
trait ClientFilter {
  import ClientFilter._
  def chain(req: Request, context: ClientContext): Response
  def andThen(next: ClientFilter): ClientFilter = {
    this match {
      case ClientFilter.identity =>
        next
      case _ =>
        new ClientFilter.AndThen(this, next)
    }
  }
  def andThen(context: ClientContext): ClientContext = {
    new ClientFilter.FilterAndThen(this, context)
  }
}


object ClientFilter {
  object identity extends ClientFilter {
    override def chain(req: Request, context: ClientContext): Response ={
      context.apply(req)
    }
  }

  private class AndThen(prev: ClientFilter, next:ClientFilter) extends ClientFilter {
    override def chain(req: Request, context: ClientContext): Response = {
      prev.chain(req, next.andThen(context))
    }
  }

  private class FilterAndThen(filter: ClientFilter, nextContext: ClientContext) extends ClientContext {
    override def apply(req: Request): Response = {
      filter.chain(req, nextContext)
    }
  }
}

trait ClientContext {
  def apply(req: Request): Response
}
