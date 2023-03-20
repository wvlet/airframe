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
import wvlet.airframe.rx.Rx

import scala.util.control.NonFatal

/**
  * An [[RxFilter]] is a filter for receiving the response from the endpoin via `endpoint.apply(request)`, and
  * transforming it into another `Rx[Response]`.
  */
trait RxFilter {

  /**
    * Apply a filter before sending the request to the endpoint, and handle its response before returning the client.
    *
    * To implement your own filter, override this method.
    *
    * @param request
    * @param endpoint
    * @return
    */
  def apply(request: Request, endpoint: RxEndpoint): Rx[Response]

  /**
    * Chain to the next filter.
    * @param nextFilter
    * @return
    */
  def andThen(nextFilter: RxFilter): RxFilter = {
    new RxFilter.AndThen(this, nextFilter)
  }

  /**
    * Bridge this filter to the endpoint.
    * @param endpoint
    * @return
    */
  def andThen(endpoint: RxEndpoint): RxEndpoint = {
    new RxFilter.FilterAndThenEndpoint(this, endpoint)
  }
}

object RxFilter {

  private class FilterAndThenEndpoint(filter: RxFilter, nextService: RxEndpoint) extends RxEndpoint {
    override def backend: RxHttpBackend = nextService.backend
    override def apply(request: Request): Rx[Response] = {
      try {
        filter.apply(request, nextService)
      } catch {
        case NonFatal(e) => Rx.exception(e)
      }
    }
  }

  private class AndThen(prev: RxFilter, next: RxFilter) extends RxFilter {
    override def apply(request: Request, endpoint: RxEndpoint): Rx[Response] = {
      try {
        prev.apply(request, next.andThen(endpoint))
      } catch {
        case NonFatal(e) => Rx.exception(e)
      }
    }
  }
}

/**
  * [[RxEndpoint]] is a terminal for processing requests and returns `Rx[Response]`.
  */
trait RxEndpoint {
  private[http] def backend: RxHttpBackend

  /**
    * @param request
    * @return
    */
  def apply(request: Request): Rx[Response]

  /**
    * Set a thread-local parameter
    */
  def setThreadLocal[A](key: String, value: A): Unit = {
    backend.setThreadLocal(key, value)
  }

  /**
    * Get a thread-local parameter
    */
  def getThreadLocal[A](key: String): Option[A] = {
    backend.getThreadLocal(key)
  }
}
