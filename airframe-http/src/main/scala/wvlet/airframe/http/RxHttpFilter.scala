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
import wvlet.airframe.rx.{OnNext, Rx}
import wvlet.log.LogSupport

import scala.util.control.NonFatal

/**
  * An [[RxHttpFilter]] is a filter for receiving the response from the endpoint via `endpoint.apply(request)`, and
  * transforming it into another `Rx[Response]`.
  */
trait RxHttpFilter extends HttpFilterType {

  /**
    * Apply a filter before sending the request to the endpoint, and handle its response before returning the client.
    *
    * To implement your own filter, override this method.
    *
    * @param request
    * @param next
    * @return
    */
  def apply(request: Request, next: RxHttpEndpoint): Rx[Response]

  /**
    * Chain to the next filter.
    * @param nextFilter
    * @return
    */
  def andThen(nextFilter: RxHttpFilter): RxHttpFilter = {
    new RxHttpFilter.AndThen(this, nextFilter)
  }

  /**
    * Bridge this filter to the endpoint.
    * @param endpoint
    * @return
    */
  def andThen(endpoint: RxHttpEndpoint): RxHttpEndpoint = {
    new RxHttpFilter.FilterAndThenEndpoint(this, endpoint)
  }

  /**
    * A handy method to create RxHttpEndpoint from a given function.
    * @param body
    * @return
    */
  def andThen(body: Request => Rx[Response]): RxHttpEndpoint = andThen(new RxHttpEndpoint {
    override def apply(request: Request): Rx[Response] = {
      try {
        body(request)
      } catch {
        case NonFatal(e) =>
          Rx.exception(e)
      }
    }
  })

}

object RxHttpFilter {
  object identity extends RxHttpFilter {
    override def apply(request: Request, next: RxHttpEndpoint): Rx[Response] = {
      next(request)
    }
    override def andThen(nextFilter: RxHttpFilter): RxHttpFilter   = nextFilter
    override def andThen(endpoint: RxHttpEndpoint): RxHttpEndpoint = endpoint
  }

  private class FilterAndThenEndpoint(filter: RxHttpFilter, next: RxHttpEndpoint)
      extends RxHttpEndpoint
      with LogSupport {
    override def apply(request: Request): Rx[Response] = {
      try {
        val ret = filter.apply(request, next)
        ret
      } catch {
        case NonFatal(e) =>
          Rx.exception(e)
      }
    }
  }

  private class AndThen(prev: RxHttpFilter, next: RxHttpFilter) extends RxHttpFilter {
    override def apply(request: Request, nextEndpoint: RxHttpEndpoint): Rx[Response] = {
      try {
        prev.apply(request, next.andThen(nextEndpoint))
      } catch {
        case NonFatal(e) => Rx.exception(e)
      }
    }
  }
}
