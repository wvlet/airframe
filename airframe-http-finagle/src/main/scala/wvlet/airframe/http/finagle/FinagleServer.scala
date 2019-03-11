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
import com.twitter.finagle.http.{Request, Response, Status}
import com.twitter.finagle.{Http, ListeningServer, Service, SimpleFilter}
import com.twitter.util.{Await, Future}
import javax.annotation.{PostConstruct, PreDestroy}
import wvlet.airframe.http.{ControllerProvider, ResponseHandler, Route, Router}
import wvlet.airframe.http.finagle.FinagleServer.FinagleService
import wvlet.log.LogSupport
import wvlet.airframe._

case class FinagleServerConfig(port: Int)

/**
  *
  */
class FinagleServer(finagleConfig: FinagleServerConfig,
                    finagleService: FinagleService,
                    initServer: Http.Server => Http.Server = { x =>
                      x
                    })
    extends LogSupport
    with AutoCloseable {
  protected[this] var server: Option[ListeningServer] = None

  def localAddress = s"localhost:${port}"
  def port: Int    = finagleConfig.port

  @PostConstruct
  def start {
    info(s"Starting a server at http://localhost:${finagleConfig.port}")
    val customServer = initServer(Http.Server())
    server = Some(customServer.serve(s":${finagleConfig.port}", finagleService))
  }

  @PreDestroy
  def stop = {
    info(s"Stopping the server http://localhost:${finagleConfig.port}")
    server.map(_.close())
  }

  override def close(): Unit = {
    stop
  }

  def waitServerTermination: Unit = {
    server.map(s => Await.ready(s))
  }
}

object FinagleServer extends LogSupport {
  type FinagleService = Service[Request, Response]

  def defaultService(router: FinagleRouter): FinagleService = {
    FinagleServer.defaultRequestLogger andThen
      FinagleServer.defaultErrorHandler andThen
      router andThen
      FinagleServer.notFound
  }

  /**
    * A simple error handler for wrapping exceptions as InternalServerError (500)
    */
  def defaultErrorHandler = new SimpleFilter[Request, Response] {
    override def apply(request: Request, service: Service[Request, Response]): Future[Response] = {
      service(request).rescue {
        case e: Throwable =>
          logger.warn(e.getMessage)
          logger.trace(e)
          Future.value(Response(Status.InternalServerError))
      }
    }
  }

  /**
    * Simple logger for logging http requests and responses to stderr
    */
  def defaultRequestLogger = new SimpleFilter[Request, Response] {
    override def apply(request: Request, service: Service[Request, Response]): Future[Response] = {
      logger.trace(request)
      service(request).map { response =>
        logger.trace(response)
        response
      }
    }
  }

  /**
    * A fallback service if FinagleRouter cannot find any matching endpoint
    */
  def notFound: Service[Request, Response] = new Service[Request, Response] {
    override def apply(request: Request): Future[Response] = {
      Future.value(Response(Status.NotFound))
    }
  }
}

trait FinagleServerFactory {
  private val controllerProvider = bind[ControllerProvider]
  private val responseHandler    = bind[ResponseHandler[Request, Response]]

  /**
    * Override this method to customize finagle service filters
    */
  protected def newService(finagleRouter: FinagleRouter) = FinagleServer.defaultService(finagleRouter)

  /**
    * Override this method to customize Finagle Server configuration.
    */
  protected def initServer(server: Http.Server): Http.Server = {
    // Do nothing by default
    server
  }

  def newFinagleServer(port: Int, router: Router): FinagleServer = {
    val finagleRouter = new FinagleRouter(router, controllerProvider, responseHandler)
    new FinagleServer(finagleConfig = FinagleServerConfig(port = port),
                      finagleService = newService(finagleRouter),
                      initServer = initServer)
  }
}
