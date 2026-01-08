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
package wvlet.airframe.http.netty

import io.netty.bootstrap.ServerBootstrap
import io.netty.buffer.PooledByteBufAllocator
import io.netty.channel.*
import io.netty.channel.epoll.{Epoll, EpollEventLoopGroup, EpollServerSocketChannel}
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.channel.unix.UnixChannelOption
import io.netty.handler.codec.http.*
import io.netty.handler.stream.ChunkedWriteHandler
import wvlet.airframe.codec.{MessageCodec, MessageCodecFactory}
import wvlet.airframe.control.ThreadUtil
import wvlet.airframe.http.HttpMessage.Response
import wvlet.airframe.http.client.{AsyncClient, SyncClient}
import wvlet.airframe.http.internal.{LogRotationHttpLogger, RPCResponseFilter}
import wvlet.airframe.http.router.{ControllerProvider, HttpRequestDispatcher}
import wvlet.airframe.http.{HttpMessage, *}
import wvlet.airframe.rx.Rx
import wvlet.airframe.surface.Surface
import wvlet.airframe.{Design, Session}
import wvlet.log.LogSupport
import wvlet.log.io.IOUtil

import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.{Executors, TimeUnit}
import javax.annotation.PostConstruct
import scala.collection.immutable.ListMap
import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success, Try}

case class NettyServerConfig(
    name: String = "default",
    serverPort: Option[Int] = None,
    controllerProvider: ControllerProvider = ControllerProvider.defaultControllerProvider,
    router: Router = Router.empty,
    useEpoll: Boolean = true,
    httpLoggerConfig: HttpLoggerConfig = HttpLoggerConfig(logFileName = "log/http_server.json"),
    httpLoggerProvider: HttpLoggerConfig => HttpLogger = { (config: HttpLoggerConfig) =>
      new LogRotationHttpLogger(config)
    },
    private val customCodecFactory: MessageCodecFactory = MessageCodecFactory.defaultFactory,
    // Thread manager for handling Future[_] responses
    executionContext: ExecutionContext = {
      // Using the global thread pool causes an issue in sbt's layered class loader #918
      // So need to use the local daemon thread pool
      ExecutionContext.fromExecutorService(
        Executors.newCachedThreadPool(ThreadUtil.newDaemonThreadFactory("airframe-netty"))
      )
    },
    // Graceful shutdown configuration for Kubernetes SIGTERM handling
    // The quiet period ensures no new tasks are submitted before shutdown
    shutdownQuietPeriodSeconds: Long = 2,
    // Maximum time to wait for graceful shutdown completion
    shutdownTimeoutSeconds: Long = 30,
    // Whether to register a JVM shutdown hook for SIGTERM/SIGINT handling
    registerShutdownHook: Boolean = false
) {
  lazy val port = serverPort.getOrElse(IOUtil.unusedPort)

  /**
    * Get the codec factory with map output enabled for JSON serialization
    */
  def codecFactory: MessageCodecFactory = customCodecFactory.withMapOutput

  private[netty] def canUseEpoll: Boolean = {
    useEpoll && Epoll.isAvailable
  }

  def withName(name: String): NettyServerConfig = {
    this.copy(name = name)
  }
  def withPort(port: Int): NettyServerConfig = {
    this.copy(serverPort = Some(port))
  }
  def withRouter(rxRouter: RxRouter): NettyServerConfig = {
    this.copy(router = Router.fromRxRouter(rxRouter))
  }
  def withHttpLoggerConfig(f: HttpLoggerConfig => HttpLoggerConfig): NettyServerConfig = {
    this.copy(httpLoggerConfig = f(httpLoggerConfig))
  }
  def withHttpLogger(loggerProvider: HttpLoggerConfig => HttpLogger): NettyServerConfig = {
    this.copy(httpLoggerProvider = loggerProvider)
  }
  def withExtraLogEntries(f: () => Map[String, Any]): NettyServerConfig = {
    withHttpLoggerConfig(_.withExtraEntries(f))
  }
  def noLogging: NettyServerConfig = {
    this.copy(
      httpLoggerProvider = HttpLogger.emptyLogger(_)
    )
  }

  def withCodecFactory(factory: MessageCodecFactory): NettyServerConfig = {
    this.copy(customCodecFactory = factory)
  }

  def withCustomCodec(p: PartialFunction[Surface, MessageCodec[_]]): NettyServerConfig = {
    this.copy(customCodecFactory = customCodecFactory.withCodecs(p))
  }

  def withCustomCodec(m: Map[Surface, MessageCodec[_]]): NettyServerConfig = {
    this.copy(customCodecFactory = customCodecFactory.withCodecs(m))
  }

  /**
    * Set the quiet period for graceful shutdown. During this period, the server will reject new connections while
    * waiting for in-flight requests to complete.
    * @param seconds
    *   quiet period in seconds (default: 2)
    */
  def withShutdownQuietPeriod(seconds: Long): NettyServerConfig = {
    this.copy(shutdownQuietPeriodSeconds = seconds)
  }

  /**
    * Set the maximum time to wait for graceful shutdown completion.
    * @param seconds
    *   timeout in seconds (default: 30)
    */
  def withShutdownTimeout(seconds: Long): NettyServerConfig = {
    this.copy(shutdownTimeoutSeconds = seconds)
  }

  /**
    * Configure graceful shutdown parameters for Kubernetes environments.
    * @param quietPeriodSeconds
    *   quiet period before shutdown (default: 2)
    * @param timeoutSeconds
    *   maximum time to wait for shutdown (default: 30)
    */
  def withGracefulShutdown(quietPeriodSeconds: Long = 2, timeoutSeconds: Long = 30): NettyServerConfig = {
    this.copy(
      shutdownQuietPeriodSeconds = quietPeriodSeconds,
      shutdownTimeoutSeconds = timeoutSeconds
    )
  }

  /**
    * Enable automatic shutdown hook registration. When enabled, the server will automatically register a JVM shutdown
    * hook to handle SIGTERM/SIGINT signals gracefully. This is useful for Kubernetes deployments where pods receive
    * SIGTERM signals during termination.
    */
  def withShutdownHook: NettyServerConfig = {
    this.copy(registerShutdownHook = true)
  }

  /**
    * Disable automatic shutdown hook registration (default behavior).
    */
  def noShutdownHook: NettyServerConfig = {
    this.copy(registerShutdownHook = false)
  }

  def newServer(session: Session): NettyServer = {
    val s = new NettyServer(this, session)
    s.start
    s
  }

  /**
    * Create a new DI design for instanciating the Netty server with this config
    * @return
    */
  def design: Design = {
    Design.newDesign
      .bind[NettyServer].toProvider { (s: Session) => newServer(s) }
      .bind[HttpServer].to[NettyServer]
  }

  def designWithSyncClient: Design = {
    design
      .bind[SyncClient].toProvider { (server: HttpServer) =>
        Http.client.newSyncClient(server.localAddress)
      }
  }

  def designWithAsyncClient: Design = {
    design
      .bind[AsyncClient].toProvider { (server: HttpServer) =>
        Http.client.newAsyncClient(server.localAddress)
      }
  }

  def start[U](body: NettyServer => U): U = {
    this.design.run[NettyServer, U] { server =>
      body(server)
    }
  }

  def newHttpLogger: HttpLogger = {
    val config = httpLoggerConfig
      .withExtraEntries(() => ListMap("server_name" -> name))
      .withCodecFactory(codecFactory)
    httpLoggerProvider(config)
  }
}

class NettyServer(config: NettyServerConfig, session: Session) extends HttpServer with LogSupport {

  private val httpLogger: HttpLogger  = config.newHttpLogger
  private val rpcFilter: RxHttpFilter = new RPCResponseFilter(httpLogger)

  private val bossGroup = {
    val tf = ThreadUtil.newDaemonThreadFactory("airframe-netty-boss")
    if (config.canUseEpoll) {
      new EpollEventLoopGroup(1, tf)
    } else {
      new NioEventLoopGroup(1, tf)
    }
  }
  private val workerGroup = {
    val tf         = ThreadUtil.newDaemonThreadFactory("airframe-netty-worker")
    val numWorkers = math.max(4, (Runtime.getRuntime.availableProcessors().toDouble / 3).ceil.toInt)
    if (config.canUseEpoll) {
      new EpollEventLoopGroup(numWorkers, tf)
    } else {
      new NioEventLoopGroup(numWorkers, tf)
    }
  }
  private var channelFuture: Option[Channel] = None

  override def localAddress: String = s"localhost:${config.port}"

  private def attachContextFilter = new RxHttpFilter {
    override def apply(request: HttpMessage.Request, next: RxHttpEndpoint): Rx[Response] = {
      val context = new NettyRPCContext(request)
      wvlet.airframe.http.Compat.attachRPCContext(context)
      next(request)
        .tapOn { case _ => wvlet.airframe.http.Compat.detachRPCContext(context) }
    }
  }

  private val started = new AtomicBoolean(false)
  private val stopped = new AtomicBoolean(false)

  // Shutdown hook thread for SIGTERM/SIGINT handling
  @volatile private var shutdownHook: Option[Thread] = None

  @PostConstruct
  def start: Unit = {
    if (stopped.get()) {
      throw new IllegalStateException(s"Server ${config.name} is already closed")
    }

    if (started.compareAndSet(false, true)) {
      startInternal
      registerShutdownHookIfNeeded()
    }
  }

  private def registerShutdownHookIfNeeded(): Unit = {
    if (config.registerShutdownHook) {
      val hook = new Thread(() => {
        info(s"Received shutdown signal for ${config.name} server")
        stop()
      }, s"airframe-netty-shutdown-${config.name}")
      shutdownHook = Some(hook)
      Runtime.getRuntime.addShutdownHook(hook)
      debug(s"Registered shutdown hook for ${config.name} server")
    }
  }

  private def unregisterShutdownHook(): Unit = {
    shutdownHook.foreach { hook =>
      Try(Runtime.getRuntime.removeShutdownHook(hook)) match {
        case Success(_) =>
          debug(s"Unregistered shutdown hook for ${config.name} server")
        case Failure(_: IllegalStateException) =>
          // JVM is already shutting down, hook cannot be removed
          ()
        case Failure(e) =>
          warn(s"Failed to unregister shutdown hook: ${e.getMessage}")
      }
      shutdownHook = None
    }
  }

  private def startInternal: Unit = {
    info(s"Starting ${config.name} server at ${localAddress}")
    val b = new ServerBootstrap()
    b.group(bossGroup, workerGroup)

    if (config.useEpoll && Epoll.isAvailable) {
      b.channel(classOf[EpollServerSocketChannel])
      b.option(UnixChannelOption.SO_REUSEPORT, Boolean.box(true))
    } else {
      b.channel(classOf[NioServerSocketChannel])
    }
    b.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, Int.box(TimeUnit.SECONDS.toMillis(30000).toInt))
    b.option(ChannelOption.SO_REUSEADDR, Boolean.box(true))
    b.option(ChannelOption.SO_BACKLOG, Int.box(1024))

    // For performance enhancement
    b.childOption(ChannelOption.TCP_NODELAY, Boolean.box(true))
    b.childOption(ChannelOption.SO_KEEPALIVE, Boolean.box(true))
    b.childOption(ChannelOption.SO_SNDBUF, Int.box(5 * 1024 * 1024 / 2)) // 2.5MB
    b.childOption(ChannelOption.SO_RCVBUF, Int.box(128 * 1024))          // 128KB
    b.childOption(ChannelOption.WRITE_BUFFER_WATER_MARK, WriteBufferWaterMark.DEFAULT)
    b.childOption(ChannelOption.AUTO_READ, Boolean.box(true))
    b.childOption(ChannelOption.AUTO_CLOSE, Boolean.box(true))

    val allocator = PooledByteBufAllocator.DEFAULT
    b.option(ChannelOption.ALLOCATOR, allocator)
    b.childOption(ChannelOption.ALLOCATOR, allocator)

    b.childHandler(new ChannelInitializer[Channel] {
      private val dispatcher = {
        NettyBackend
          .rxFilterAdapter(
            attachContextFilter
              .andThen(rpcFilter)
          )
          .andThen(
            HttpRequestDispatcher.newDispatcher(
              session = session,
              config.router,
              config.controllerProvider,
              NettyBackend,
              new NettyResponseHandler(config.codecFactory),
              config.codecFactory,
              config.executionContext
            )
          )
      }

      override def initChannel(ch: Channel): Unit = {
        val pipeline = ch.pipeline()
        pipeline.addLast(
          new HttpServerCodec(
            4096,      // the same with Netty's default MAX_INITIAL_LINE_LENGTH
            64 * 1024, // Upto ALB's max request header size
            // No need to limit the chunk size in Netty. Ref https://github.com/twitter/finagle/commit/990c8650366e5374ea062c753a4628c5971fc40e
            Int.MaxValue,
            true // validate headers
          )
        )
        pipeline.addLast(new HttpServerKeepAliveHandler())
        pipeline.addLast(new HttpObjectAggregator(Int.MaxValue))
        pipeline.addLast(new HttpContentCompressor())
        pipeline.addLast(new HttpServerExpectContinueHandler)
        pipeline.addLast(new ChunkedWriteHandler())
        pipeline.addLast(new NettyRequestHandler(config, dispatcher))
      }
    })

    channelFuture = Some(b.bind(config.port).sync().channel())
  }

  override def stop(): Unit = {
    if (stopped.compareAndSet(false, true)) {
      info(
        s"Stopping ${config.name} server at ${localAddress} " +
          s"(graceful shutdown: quietPeriod=${config.shutdownQuietPeriodSeconds}s, timeout=${config.shutdownTimeoutSeconds}s)"
      )

      // Unregister shutdown hook to prevent double-stop during JVM shutdown
      unregisterShutdownHook()

      // Close the server channel first to stop accepting new connections
      channelFuture.foreach(_.close().await(config.shutdownTimeoutSeconds, TimeUnit.SECONDS))

      // Gracefully shutdown worker group first to complete in-flight requests
      val workerFuture = workerGroup.shutdownGracefully(
        config.shutdownQuietPeriodSeconds,
        config.shutdownTimeoutSeconds,
        TimeUnit.SECONDS
      )
      workerFuture.await(config.shutdownTimeoutSeconds, TimeUnit.SECONDS)

      // Then shutdown boss group
      val bossFuture = bossGroup.shutdownGracefully(
        config.shutdownQuietPeriodSeconds,
        config.shutdownTimeoutSeconds,
        TimeUnit.SECONDS
      )
      bossFuture.await(config.shutdownTimeoutSeconds, TimeUnit.SECONDS)

      // Close the HTTP logger
      httpLogger.close()

      info(s"${config.name} server stopped")
    }
  }

  /**
    * Await and block until the server terminates. If the server is already terminated (via close()), this method
    * returns immediately.
    */
  override def awaitTermination(): Unit = {
    channelFuture.foreach(_.closeFuture().sync())
  }
}
