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
import com.twitter.finagle.{Service, SimpleFilter}
import com.twitter.util.Future
import wvlet.airframe.Session
import wvlet.airframe.codec.MessageCodecFactory
import wvlet.airframe.http.router.HttpRequestDispatcher
import wvlet.airframe.http._

/**
  * An wrapper of HttpFilter for Finagle backend implementation
  */
abstract class FinagleFilter extends HttpFilter[Request, Response, Future] {
  override def backend: HttpBackend[Request, Response, Future] = FinagleBackend
}

class FinagleRouter(session: Session, private[finagle] val config: FinagleServerConfig)
    extends SimpleFilter[Request, Response] {
  private val dispatcher =
    HttpRequestDispatcher.newDispatcher(
      session,
      config.router,
      config.controllerProvider,
      FinagleBackend,
      config.responseHandler,
      MessageCodecFactory.defaultFactory.orElse(MessageCodecFactory.newFactory(config.customCodec))
    )

  override def apply(request: Request, service: Service[Request, Response]): Future[Response] = {
    dispatcher.apply(
      request,
      FinagleBackend.newContext { (request: Request) =>
        service(request)
      }
    )
  }
}

/**
  * Adapter for chaining Http.Filter and FinagleFilter
  */
class FinagleFilterAdapter(filter: Http.Filter) extends FinagleFilter {
  override def apply(request: Request, context: Context): Future[Response] = {
    implicit val ec = HttpBackend.DefaultBackend.executionContext
    val sf: scala.concurrent.Future[Response] =
      filter
        .apply(request.toHttpRequest, new FinagleContextAdapter(context))
        .map(resp => convertToFinagleResponse(resp))

    FinagleBackend.toFuture(sf, ec)
  }
}

/**
  * Adapter for running FinagleContext from Http.Filter implementation
  *
  * @param finagleContext
  */
class FinagleContextAdapter(finagleContext: FinagleBackend.Context) extends Http.Context {

  override def apply(request: HttpMessage.Request): concurrent.Future[HttpMessage.Response] = {
    val finagleRequest = finagle.convertToFinagleRequest(request)
    val futureResponse = finagleContext(finagleRequest).map { resp =>
      resp.toHttpResponse
    }
    FinagleBackend.toScalaFuture(futureResponse)
  }

  override def setThreadLocal[A](key: String, value: A): Unit = {
    FinagleBackend.setThreadLocal[A](key, value)
  }

  override def getThreadLocal[A](key: String): Option[A] = {
    FinagleBackend.getThreadLocal[A](key)
  }
}
