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
  * [[RxContext]] is a service interface for processing request and returning `Rx[Response]`.
  */
trait RxContext {
  private[http] def backend: HttpBackend[Request, Response, Rx]

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

/**
  * An [[RxFilter]] is a filter for receiving the response from the context service via `context.apply(request)`, and
  * transforming it into another `Rx[Response]`.
  */
trait RxFilter {

  /**
    * Implement this method to create your own filter.
    * @param request
    * @param context
    * @return
    */
  def apply(request: Request, context: RxContext): Rx[Response]

  /**
    * Chain to the next filter.
    * @param nextFilter
    * @return
    */
  def andThen(nextFilter: RxFilter): RxFilter = {
    new RxFilter.AndThen(this, nextFilter)
  }

  /**
    * Terminates the filter at the context.
    * @param context
    * @return
    */
  def andThen(context: RxContext): RxContext = {
    new RxFilter.FilterAndThenContext(this, context)
  }
}

object RxFilter {

  private class FilterAndThenContext(filter: RxFilter, context: RxContext) extends RxContext {
    override def backend: HttpBackend[Request, Response, Rx] = context.backend
    override def apply(request: Request): Rx[Response] = {
      try {
        filter.apply(request, context)
      } catch {
        case NonFatal(e) => Rx.exception(e)
      }
    }
  }

  private class AndThen(prev: RxFilter, next: RxFilter) extends RxFilter {
    override def apply(request: Request, context: RxContext): Rx[Response] = {
      try {
        prev.apply(request, next.andThen(context))
      } catch {
        case NonFatal(e) => Rx.exception(e)
      }
    }
  }
}
