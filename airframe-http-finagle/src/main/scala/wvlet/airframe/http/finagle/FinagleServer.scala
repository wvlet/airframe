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
import com.twitter.finagle._
import com.twitter.finagle.http.{Request, Response, Status}
import com.twitter.finagle.stats.StatsReceiver
import com.twitter.finagle.tracing.Tracer
import com.twitter.util.{Await, Future}

import javax.annotation.PostConstruct
import wvlet.airframe._
import wvlet.airframe.codec.MessageCodec
import wvlet.airframe.control.MultipleExceptions
import wvlet.airframe.http.finagle.FinagleServer.FinagleService
import wvlet.airframe.http.finagle.filter.HttpAccessLogFilter
import wvlet.airframe.http.router.{ControllerProvider, ResponseHandler}
import wvlet.airframe.http.{HttpBackend, HttpHeader, HttpMessage, HttpServerException, RPCContext, RPCException, Router}
import wvlet.airframe.surface.Surface
import wvlet.log.LogSupport
import wvlet.log.io.IOUtil

import scala.annotation.tailrec
import scala.collection.parallel.immutable.ParVector
import scala.concurrent.ExecutionException
import scala.util.Try
import scala.util.control.NonFatal

case class FinagleServerConfig(
    name: String = "default",
    serverPort: Option[Int] = None,
    router: Router = Router.empty,
    serverInitializer: Http.Server => Http.Server = identity,
    customCodec: PartialFunction[Surface, MessageCodec[_]] = PartialFunction.empty,
    controllerProvider: ControllerProvider = ControllerProvider.defaultControllerProvider,
    tracer: Option[Tracer] = None,
    statsReceiver: Option[StatsReceiver] = None,
    // Filter for http request/response logging
    loggingFilter: Filter[Request, Response, Request, Response] = HttpAccessLogFilter.default,
    // Filter for handling errors
    errorFilter: Filter[Request, Response, Request, Response] = FinagleServer.defaultErrorFilter,
    // A top-level filter applied before routing requests
    beforeRoutingFilter: Filter[Request, Response, Request, Response] = Filter.identity,
    // Service called when no matching route is found
    fallbackService: Service[Request, Response] = FinagleServer.notFound
) {
  // Lazily acquire an unused port to avoid conflicts between multiple servers
  lazy val port = serverPort.getOrElse(IOUtil.unusedPort)

  def withName(name: String): FinagleServerConfig = {
    this.copy(name = name)
  }
  def withPort(port: Int): FinagleServerConfig = {
    this.copy(serverPort = Some(port))
  }
  def withRouter(router: Router): FinagleServerConfig = {
    this.copy(router = router)
  }
  def withCustomCodec(p: PartialFunction[Surface, MessageCodec[_]]): FinagleServerConfig = {
    this.copy(customCodec = p)
  }
  def withCustomCodec(m: Map[Surface, MessageCodec[_]]): FinagleServerConfig = {
    this.copy(customCodec = customCodec.orElse {
      case s: Surface if m.contains(s) => m(s)
    })
  }
  def withControllerProvider(c: ControllerProvider): FinagleServerConfig = {
    this.copy(controllerProvider = c)
  }
  def withTracer(t: Tracer): FinagleServerConfig = {
    this.copy(tracer = Some(t))
  }
  def noTracer: FinagleServerConfig = {
    this.copy(tracer = None)
  }
  def withStatsReceiver(statsReceiver: StatsReceiver): FinagleServerConfig = {
    this.copy(statsReceiver = Some(statsReceiver))
  }
  def noStatsReceiver: FinagleServerConfig = {
    this.copy(statsReceiver = None)
  }
  def withServerInitializer(init: Http.Server => Http.Server): FinagleServerConfig = {
    this.copy(serverInitializer = init)
  }

  def withLoggingFilter(filter: Filter[Request, Response, Request, Response]): FinagleServerConfig = {
    this.copy(loggingFilter = filter)
  }
  def noLoggingFilter: FinagleServerConfig = {
    // Even if the default logger is disabled, add a trace request/response logger for production investigation purpose
    this.copy(loggingFilter = HttpAccessLogFilter.traceLoggingFilter)
  }
  def withErrorFilter(filter: Filter[Request, Response, Request, Response]): FinagleServerConfig = {
    this.copy(errorFilter = filter)
  }
  def withBeforeRoutingFilter(filter: Filter[Request, Response, Request, Response]): FinagleServerConfig = {
    this.copy(beforeRoutingFilter = filter)
  }
  def noBeforeRoutingFilter: FinagleServerConfig = {
    this.copy(beforeRoutingFilter = Filter.identity)
  }
  def withFallbackService(service: Service[Request, Response]): FinagleServerConfig = {
    this.copy(fallbackService = service)
  }

  def responseHandler: ResponseHandler[Request, Response] = new FinagleResponseHandler(customCodec)

  // Initialize Finagle server using this config
  private[finagle] def initServer(server: Http.Server): Http.Server = {
    var s = serverInitializer(server)
    for (x <- tracer) {
      s = s.withTracer(x)
    }
    for (x <- statsReceiver) {
      s = s.withStatsReceiver(x)
    }
    s
  }

  private[finagle] def newService(session: Session): FinagleService = {
    val finagleRouter = new FinagleRouter(session, this)

    // Build Finagle filters
    val service =
      FinagleServer.threadLocalStorageFilter andThen
        loggingFilter andThen
        errorFilter andThen
        beforeRoutingFilter andThen
        finagleRouter andThen
        fallbackService

    service
  }

  def design: Design = {
    finagleDefaultDesign
      .bind[FinagleServerConfig].toInstance(this)
  }

  /**
    * Create a design for this server and a FinacleSyncClient design. This is useful for testing
    */
  def designWithSyncClient: Design = {
    design + Finagle.client.syncClientDesign
  }

  def newFinagleServer(session: Session): FinagleServer = {
    new FinagleServer(finagleConfig = this, newService(session))
  }

  /**
    * Start the server and execute the code block. After finishing the code block, it will stop the server.
    *
    * If you want to keep running the server, call server.waitServerTermination inside the code block.
    */
  def start[U](body: FinagleServer => U): U = {
    newFinagleServerDesign(this).run[FinagleServer, U] { server =>
      body(server)
    }
  }
}

/**
  */
class FinagleServer(finagleConfig: FinagleServerConfig, finagleService: FinagleService)
    extends LogSupport
    with AutoCloseable {
  protected[this] var server: Option[ListeningServer] = None

  def port: Int            = finagleConfig.port
  def localAddress: String = s"localhost:${port}"

  @PostConstruct
  def start: Unit = {
    synchronized {
      if (server.isEmpty) {
        info(s"Starting ${finagleConfig.name} server at http://localhost:${port}")
        val customServer = finagleConfig.initServer(Http.Server())
        server = Some(customServer.serve(s":${port}", finagleService))
      }
    }
  }

  def stop = {
    synchronized {
      if (server.isDefined) {
        info(s"Stopping ${finagleConfig.name} server at http://localhost:${port}")
        server.map(x => Await.result(x.close()))
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

  /**
    * Find the root cause of the exception from wrapped exception classes
    */
  @tailrec
  private[finagle] def findCause(e: Throwable): Throwable = {
    e match {
      case i: InvocationTargetException if i.getTargetException != null =>
        findCause(i.getTargetException)
      case ee: ExecutionException if ee.getCause != null =>
        findCause(ee.getCause)
      case _ =>
        e
    }
  }

  /**
    * The default error handler to return HttpServerException with its error code and content body (if defined). For
    * other types of errors, it wraps exceptions as InternalServerError (500). For such unknown errors, we do not return
    * the exception as is because it may contain internal information.
    */
  def defaultErrorFilter: SimpleFilter[Request, Response] =
    new SimpleFilter[Request, Response] {
      override def apply(request: Request, service: Service[Request, Response]): Future[Response] = {
        service(request).rescue { case e: Throwable =>
          val ex = findCause(e)
          FinagleBackend.setThreadLocal(HttpBackend.TLS_KEY_SERVER_EXCEPTION, ex)
          ex match {
            case e: HttpServerException =>
              logger.warn(s"${request} failed: ${e.getMessage}", findCause(e.getCause))
              // HttpServerException is a properly handled exception, so convert it to an error response
              Future.value(convertToFinagleResponse(e.toResponse))
            case e: RPCException =>
              logger.warn(s"RPC request ${request} failed: ${e.getMessage}", findCause(e.getCause))
              var resp = wvlet.airframe.http.Http
                .response(e.status.httpStatus)
                // Add RPC status header to handle errors in clients
                .addHeader(HttpHeader.xAirframeRPCStatus, e.status.code.toString)

              try {
                // Embed RPCError into the response body
                if (request.acceptsJson) {
                  resp = resp.withJson(e.toJson)
                } else {
                  // Use MessagePack encoding by default
                  resp = resp.withMsgPack(e.toMsgPack)
                }
              } catch {
                case ex: Throwable =>
                  // Show warning
                  logger.warn(s"Failed to serialize RPCException: ${e}", ex)
              }
              Future.value(convertToFinagleResponse(resp))
            case other =>
              logger.warn(s"${request} failed: ${other}", other)
              // Just return internal server failure with 500 response code
              Future.value(Response(Status.InternalServerError))
          }
        }
      }
    }

  def threadLocalStorageFilter: SimpleFilter[Request, Response] =
    new SimpleFilter[Request, Response] {
      override def apply(request: Request, service: Service[Request, Response]): Future[Response] = {
        FinagleBackend.withThreadLocalStore {
          wvlet.airframe.http.Compat.attachRPCContext(FinagleRPCContext(request))
          service(request)
        }
      }
    }

  /**
    * Simple logger for logging http requests and responses to stderr
    */
  def defaultRequestLogger: SimpleFilter[Request, Response] =
    new SimpleFilter[Request, Response] {
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
  def notFound: Service[Request, Response] =
    new Service[Request, Response] {
      override def apply(request: Request): Future[Response] = {
        Future.value(Response(Status.NotFound))
      }
    }
}

/**
  * A factory to create new finagle server
  */
class FinagleServerFactory(session: Session) extends AutoCloseable with LogSupport {
  private var createdServers = List.empty[FinagleServer]

  /**
    * Override this method to customize Finagle Server configuration.
    */
  @deprecated(message = "Use FinagleServerConfig.withServerInitializer", since = "19.11.0")
  protected def initServer(server: Http.Server): Http.Server = {
    // Do nothing by default
    server
  }

  def newFinagleServer(config: FinagleServerConfig): FinagleServer = {
    val baseInitializer = config.serverInitializer
    val server =
      config
        .withServerInitializer { baseInitializer.andThen(initServer) }
        .newFinagleServer(session)
    synchronized {
      createdServers = server :: createdServers
    }
    server.start
    server
  }

  def newFinagleServer(name: String, port: Int, router: Router): FinagleServer = {
    newFinagleServer(
      FinagleServerConfig()
        .withName(name)
        .withPort(port)
        .withRouter(router)
    )
  }

  /**
    * Block until all servers created by this factory terminate
    */
  def awaitTermination: Unit = {
    val b = ParVector.newBuilder[FinagleServer]
    b ++= createdServers
    b.result().foreach(_.waitServerTermination)
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
