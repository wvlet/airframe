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
import java.lang.reflect.InvocationTargetException

import com.twitter.finagle.http.{Request, Response, Status}
import com.twitter.finagle.{Http, ListeningServer, Service, SimpleFilter}
import com.twitter.util.{Await, Future}
import javax.annotation.{PostConstruct, PreDestroy}
import wvlet.airframe._
import wvlet.airframe.control.MultipleExceptions
import wvlet.airframe.http.finagle.FinagleServer.FinagleService
import wvlet.airframe.http.{ControllerProvider, ResponseHandler, Router}
import wvlet.log.{LogSupport, Logger}
import wvlet.log.io.IOUtil

import scala.annotation.tailrec
import scala.util.control.NonFatal

case class FinagleServerConfig(name: String = "default", port: Int = IOUtil.unusedPort, router: Router = Router.empty)

/**
  *
  */
class FinagleServer(
    finagleConfig: FinagleServerConfig,
    finagleService: FinagleService,
    initServer: Http.Server => Http.Server = identity
) extends LogSupport
    with AutoCloseable {
  protected[this] var server: Option[ListeningServer] = None

  def localAddress = s"localhost:${port}"
  def port: Int    = finagleConfig.port

  @PostConstruct
  def start: Unit = {
    synchronized {
      if (server.isEmpty) {
        info(s"Starting ${finagleConfig.name} server at http://localhost:${finagleConfig.port}")
        val customServer = initServer(Http.Server())
        server = Some(customServer.serve(s":${finagleConfig.port}", finagleService))
      }
    }
  }

  def stop = {
    synchronized {
      if (server.isDefined) {
        info(s"Stopping ${finagleConfig.name} server at http://localhost:${finagleConfig.port}")
        server.map(x => Await.result(x.close))
        server = None
      } else {
        None
      }
    }
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
    * A simple error handler for wrapping exceptions as InternalServerError (500).
    * We do not return the exception as is because it may contain internal information.
    */
  def defaultErrorHandler = new SimpleFilter[Request, Response] {
    override def apply(request: Request, service: Service[Request, Response]): Future[Response] = {
      service(request).rescue {
        case e: Throwable =>
          // Resolve the cause of the exception
          @tailrec
          def getCause(x: Throwable): Throwable = {
            x match {
              case i: InvocationTargetException if i.getTargetException != null =>
                getCause(i.getTargetException)
              case _ =>
                x
            }
          }

          val ex = getCause(e)
          logger.warn(ex)
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

/**
  * A factory to create new finagle server
  */
trait FinagleServerFactory extends AutoCloseable with LogSupport {

  private var createdServers = List.empty[FinagleServer]

  protected val controllerProvider = bind[ControllerProvider]
  protected val responseHandler    = bind[ResponseHandler[Request, Response]]

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

  def newFinagleServer(config: FinagleServerConfig): FinagleServer = {
    val finagleRouter = new FinagleRouter(config, controllerProvider, responseHandler)
    val server =
      new FinagleServer(finagleConfig = config, finagleService = newService(finagleRouter), initServer = initServer)

    synchronized {
      createdServers = server :: createdServers
    }
    server.start
    server
  }

  def newFinagleServer(name: String, port: Int, router: Router): FinagleServer = {
    newFinagleServer(FinagleServerConfig(name = name, port = port, router = router))
  }

  override def close(): Unit = {
    debug(s"Closing FinagleServerFactory")
    val ex = Seq.newBuilder[Throwable]
    for (server <- createdServers) {
      try {
        server.close()
      } catch {
        case NonFatal(e) =>
          ex += e
      }
    }
    createdServers = List.empty

    val exceptions = ex.result()
    if (exceptions.nonEmpty) {
      if (exceptions.size == 1) {
        throw exceptions.head
      } else {
        throw MultipleExceptions(exceptions)
      }
    }
  }
}
