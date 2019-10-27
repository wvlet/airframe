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

import com.twitter.finagle.{Service, SimpleFilter}
import com.twitter.finagle.http.{Request, Response}
import com.twitter.util.Future
import wvlet.airframe.http.{ControllerProvider, HttpContext, HttpRequestDispatcher, ResponseHandler, Router}
import wvlet.airframe.http.HttpFilter.HttpFilterFactory
import wvlet.airframe.http.finagle.FinagleFilter.FinagleFilterFactory

/**
  * An wrapper of HttpFilter for Finagle backend implementation
  */
abstract class FinagleFilter extends FinagleFilterFactory.HttpFilterBase

object FinagleFilter {

  object FinagleFilterFactory extends HttpFilterFactory[Request, Response, Future] {
    override protected def wrapException(e: Throwable): Future[Response] = {
      Future.exception(e)
    }
    override def toFuture[A](a: A): Future[A] = Future.value(a)
    override def isFutureType(cl: Class[_]): Boolean = {
      classOf[Future[_]].isAssignableFrom(cl)
    }
    override def isRawResponseType(cl: Class[_]): Boolean = {
      classOf[Response].isAssignableFrom(cl)
    }
    override def mapF[A, B](f: Future[A], body: A => B): Future[B] = {
      f.map(body)
    }
    override def newFilter(
        body: (Request, HttpContext[Request, Response, Future]) => Future[Response]): FinagleFilterFactory.Filter = {
      new HttpFilterBase {
        override def apply(request: Request, context: HttpContext[Request, Response, Future]): Future[Response] = {
          body(request, context)
        }
      }
    }
  }

  val Identity = FinagleFilterFactory.Identity
}

class FinagleRouter(config: FinagleServerConfig,
                    controllerProvider: ControllerProvider,
                    responseHandler: ResponseHandler[Request, Response])
    extends SimpleFilter[Request, Response] {

  private val dispatcher =
    HttpRequestDispatcher.newDispatcher(config.router, controllerProvider, FinagleFilterFactory, responseHandler)

  override def apply(request: Request, service: Service[Request, Response]): Future[Response] = {
    dispatcher.apply(request, new HttpContext[Request, Response, Future] {
      override def apply(request: Request): Future[Response] = {
        service(request)
      }
    })
  }
}
