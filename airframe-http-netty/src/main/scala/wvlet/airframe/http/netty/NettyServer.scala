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
import wvlet.airframe.http.{HttpMessage, *}
import wvlet.airframe.http.client.{AsyncClient, SyncClient}
import wvlet.airframe.http.internal.{LogRotationHttpLogger, RPCLoggingFilter, RPCResponseFilter}
import wvlet.airframe.http.router.{ControllerProvider, HttpRequestDispatcher}
import wvlet.airframe.rx.Rx
import wvlet.airframe.surface.Surface
import wvlet.airframe.{Design, Session}
import wvlet.log.LogSupport
import wvlet.log.io.IOUtil

import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import javax.annotation.PostConstruct
import scala.collection.immutable.ListMap
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
    loggingFilter: HttpLogger => RxHttpFilter = { new RPCLoggingFilter(_) },
    customCodec: PartialFunction[Surface, MessageCodec[_]] = PartialFunction.empty
) {
  lazy val port = serverPort.getOrElse(IOUtil.unusedPort)

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
  def noLogging: NettyServerConfig = {
    this.copy(
      loggingFilter = { _ => RxHttpFilter.identity },
      httpLoggerProvider = HttpLogger.emptyLogger(_)
    )
  }

  def withCustomCodec(p: PartialFunction[Surface, MessageCodec[_]]): NettyServerConfig = {
    this.copy(customCodec = p)
  }
  def withCustomCodec(m: Map[Surface, MessageCodec[_]]): NettyServerConfig = {
    this.copy(customCodec = customCodec.orElse {
      case s: Surface if m.contains(s) => m(s)
    })
  }

  def newServer(session: Session): NettyServer = {
    val s = new NettyServer(this, session)
    s.start
    s
  }
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
    httpLoggerProvider(httpLoggerConfig.addExtraTags(ListMap("server_name" -> name)))
  }
}

class NettyServer(config: NettyServerConfig, session: Session) extends HttpServer with LogSupport {

  private val httpLogger: HttpLogger      = config.newHttpLogger
  private val loggingFilter: RxHttpFilter = config.loggingFilter(httpLogger)

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
      next(request).toRx
        // TODO use transformTry
        .transformRx { v =>
          wvlet.airframe.http.Compat.detachRPCContext(context)
          v match {
            case Success(v) =>
              Rx.single(v)
            case Failure(e) =>
              Rx.exception(e)
          }
        }
    }
  }

  private val started = new AtomicBoolean(false)
  private val stopped = new AtomicBoolean(false)

  @PostConstruct
  def start: Unit = {
    if (stopped.get()) {
      throw new IllegalStateException(s"Server ${config.name} is already closed")
    }

    if (started.compareAndSet(false, true)) {
      startInternal
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
    // b.channel(classOf[LocalServerChannel])
    b.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, Int.box(TimeUnit.SECONDS.toMillis(1).toInt))
    b.option(ChannelOption.SO_REUSEADDR, Boolean.box(true))
    b.option(ChannelOption.SO_BACKLOG, Int.box(1024))
    b.childOption(ChannelOption.SO_KEEPALIVE, Boolean.box(true))
    b.childOption(ChannelOption.TCP_NODELAY, Boolean.box(true))
    b.childOption(ChannelOption.WRITE_BUFFER_WATER_MARK, WriteBufferWaterMark.DEFAULT)

    // b.option(ChannelOption.AUTO_READ, Boolean.box(false))

    // b.childOption(ChannelOption.SO_SNDBUF, Int.box(1024 * 1024))
    // b.childOption(ChannelOption.SO_RCVBUF, Int.box(32 * 1024))

    val allocator = PooledByteBufAllocator.DEFAULT
    b.option(ChannelOption.ALLOCATOR, allocator)
    b.childOption(ChannelOption.ALLOCATOR, allocator)

    b.childHandler(new ChannelInitializer[Channel] {
      // private[this] val activeConnections = new AtomicLong()
      // private val maxConnections          = 50
      //      private val closeListener = new ChannelFutureListener {
      //        override def operationComplete(future: ChannelFuture): Unit = {
      //          activeConnections.decrementAndGet()
      //        }
      //      }

      private val dispatcher = {
        NettyBackend
          .rxFilterAdapter(
            attachContextFilter
              .andThen(loggingFilter)
              .andThen(RPCResponseFilter)
          )
          .andThen(
            HttpRequestDispatcher.newDispatcher(
              session = session,
              config.router,
              config.controllerProvider,
              NettyBackend,
              new NettyResponseHandler,
              // Set a custom codec and use JSON map output
              MessageCodecFactory.defaultFactoryForJSON.withCodecs(config.customCodec)
            )
          )
      }

      override def initChannel(ch: Channel): Unit = {
        //        val numConns = activeConnections.incrementAndGet()
        //        if (numConns > maxConnections) {
        //          activeConnections.decrementAndGet()
        //          ch.close()
        //        } else {
        //          ch.closeFuture().addListener(closeListener)

        val pipeline = ch.pipeline()

        // pipeline.addLast(new IdleStateHandler(1, 1, 60, TimeUnit.SECONDS))
        pipeline.addLast(new HttpServerCodec()) // 4096, 8192, Int.MaxValue, false))
        // pipeline.addLast(new HttpServerKeepAliveHandler())
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
      info(s"Stopping ${config.name} server at ${localAddress}")
      workerGroup.shutdownGracefully(0, 0, TimeUnit.SECONDS)
      bossGroup.shutdownGracefully(0, 0, TimeUnit.SECONDS)
      httpLogger.close()
      channelFuture.foreach(_.close().await(1, TimeUnit.SECONDS))
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
