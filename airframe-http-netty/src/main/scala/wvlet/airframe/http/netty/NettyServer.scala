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
import io.netty.channel._
import io.netty.channel.epoll.{Epoll, EpollEventLoopGroup, EpollServerSocketChannel}
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.channel.unix.UnixChannelOption
import io.netty.handler.codec.http._
import io.netty.handler.stream.ChunkedWriteHandler
import wvlet.airframe.codec.MessageCodecFactory
import wvlet.airframe.control.ThreadUtil
import wvlet.airframe.http._
import wvlet.airframe.http.client.SyncClient
import wvlet.airframe.http.router.{ControllerProvider, HttpRequestDispatcher}
import wvlet.airframe.{Design, Session}
import wvlet.log.LogSupport
import wvlet.log.io.IOUtil

import java.util.concurrent.TimeUnit
import javax.annotation.PostConstruct

case class NettyServerConfig(
    serverPort: Option[Int] = None,
    controllerProvider: ControllerProvider = ControllerProvider.defaultControllerProvider,
    router: Router = Router.empty,
    useEpoll: Boolean = true
) {
  lazy val port = serverPort.getOrElse(IOUtil.unusedPort)

  private[netty] def canUseEpoll: Boolean = {
    useEpoll && Epoll.isAvailable
  }

  def withRouter(router: Router): NettyServerConfig = {
    this.copy(router = router)
  }
  def newServer(session: Session): NettyServer = {
    val s = new NettyServer(this, session)
    s.start
    s
  }

  def design: Design = {
    Design.newDesign
      .bind[NettyServerConfig].toInstance(this)
      .bind[NettyServer].toSingleton
  }

  def designWithSyncClient: Design = {
    design
      .bind[SyncClient].toProvider { (server: NettyServer) =>
        Http.client.newSyncClient(server.localAddress)
      }
  }
}

class NettyServer(config: NettyServerConfig, session: Session) extends AutoCloseable with LogSupport {

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

  val localAddress: String = s"localhost:${config.port}"

  @PostConstruct
  def start: Unit = {
    info(s"Starting server at ${localAddress}")
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
        HttpRequestDispatcher.newDispatcher(
          session = session,
          config.router,
          config.controllerProvider,
          NettyBackend,
          new NettyResponseHandler,
          MessageCodecFactory.defaultFactoryForJSON
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
        pipeline.addLast(new NetthRequestHandler(config, dispatcher))
      }
    })

    channelFuture = Some(b.bind(config.port).sync().channel())
  }

  override def close(): Unit = {
    info(s"Closing server at ${localAddress}")
    workerGroup.shutdownGracefully(0, 0, TimeUnit.SECONDS)
    bossGroup.shutdownGracefully(0, 0, TimeUnit.SECONDS)
    channelFuture.foreach(_.close().await(1, TimeUnit.SECONDS))
  }
}
