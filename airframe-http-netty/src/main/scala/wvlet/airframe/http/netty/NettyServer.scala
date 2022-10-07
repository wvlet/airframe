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
import io.netty.buffer.Unpooled
import io.netty.channel._
import io.netty.channel.epoll.{Epoll, EpollEventLoopGroup, EpollServerSocketChannel}
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.channel.unix.UnixChannelOption
import io.netty.handler.codec.http._
import io.netty.handler.stream.ChunkedWriteHandler
import wvlet.airframe.codec.MessageCodecFactory
import wvlet.airframe.http.HttpMessage.{Request, Response}
import wvlet.airframe.http.client.SyncClient
import wvlet.airframe.http.router.{ControllerProvider, HttpRequestDispatcher}
import wvlet.airframe.http.{HttpMethod, _}
import wvlet.airframe.rx.{OnCompletion, OnError, OnNext, Rx, RxRunner}
import wvlet.airframe.{Design, Session}
import wvlet.log.LogSupport
import wvlet.log.io.IOUtil

import javax.annotation.PostConstruct
import scala.jdk.CollectionConverters._

case class NettyServerConfig(
    serverPort: Option[Int] = None,
    controllerProvider: ControllerProvider = ControllerProvider.defaultControllerProvider,
    router: Router = Router.empty,
    useEpoll: Boolean = true
) {
  lazy val port = serverPort.getOrElse(IOUtil.unusedPort)

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
    if (config.useEpoll && Epoll.isAvailable) {
      new EpollEventLoopGroup(1)
    } else {
      new NioEventLoopGroup(1)
    }
  }
  private val workerGroup = {
    new NioEventLoopGroup(math.max(4, (Runtime.getRuntime.availableProcessors().toDouble / 3).ceil.toInt))
  }
  private var channelFuture: Option[ChannelFuture] = None

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
    b.option(ChannelOption.SO_REUSEADDR, Boolean.box(true))
    b.option(ChannelOption.SO_BACKLOG, Int.box(1024))
    b.childOption(ChannelOption.SO_KEEPALIVE, Boolean.box(true))
    b.childOption(ChannelOption.TCP_NODELAY, Boolean.box(true))
    b.childOption(ChannelOption.WRITE_BUFFER_WATER_MARK, WriteBufferWaterMark.DEFAULT)
    // b.childOption(ChannelOption.SO_SNDBUF, Int.box(1024 * 1024))
    // b.childOption(ChannelOption.SO_RCVBUF, Int.box(32 * 1024))
    b.childHandler(new ChannelInitializer[SocketChannel] {
      override def initChannel(ch: SocketChannel): Unit = {
        val pipeline = ch.pipeline()

        pipeline.addLast(new HttpServerCodec())
        pipeline.addLast(new HttpObjectAggregator(65536))
        pipeline.addLast(new HttpContentCompressor())
        pipeline.addLast(new ChunkedWriteHandler())
        // pipeline.addLast(new HttpServerExpectContinueHandler)
        pipeline.addLast(new HttpServerKeepAliveHandler())
        pipeline.addLast(new HttpRequestHandler(config, session))
      }
    })

    channelFuture = Some(b.bind(config.port).sync())
  }

  override def close(): Unit = {
    info(s"Closing server at ${localAddress}")
    workerGroup.shutdownGracefully()
    bossGroup.shutdownGracefully()
    channelFuture.foreach(_.channel().closeFuture().sync())
  }
}

class HttpRequestHandler(config: NettyServerConfig, session: Session)
    extends SimpleChannelInboundHandler[FullHttpRequest]
    with LogSupport {

  private val dispatcher = HttpRequestDispatcher.newDispatcher(
    session = session,
    config.router,
    config.controllerProvider,
    NettyBackend,
    new NettyResponseHandler,
    MessageCodecFactory.defaultFactoryForJSON
  )

  override def exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable): Unit = {
    warn(cause)
    ctx.close()
  }

  override def channelRead0(ctx: ChannelHandlerContext, msg: FullHttpRequest): Unit = {
    var req: wvlet.airframe.http.HttpMessage.Request = msg.method().name() match {
      case HttpMethod.GET     => Http.GET(msg.uri())
      case HttpMethod.POST    => Http.POST(msg.uri())
      case HttpMethod.PUT     => Http.PUT(msg.uri())
      case HttpMethod.DELETE  => Http.DELETE(msg.uri())
      case HttpMethod.PATCH   => Http.PATCH(msg.uri())
      case HttpMethod.TRACE   => Http.request(wvlet.airframe.http.HttpMethod.TRACE, msg.uri())
      case HttpMethod.OPTIONS => Http.request(wvlet.airframe.http.HttpMethod.OPTIONS, msg.uri())
      case _                  => ???
    }

    msg.headers().names().asScala.map { x =>
      req = req.withHeader(x, msg.headers().get(x))
    }
    val requestBody     = msg.content()
    val requestBodySize = requestBody.readableBytes()
    if (requestBodySize > 0) {
      val buf = new Array[Byte](requestBodySize)
      requestBody.getBytes(requestBody.readerIndex(), buf)
      req = req.withContent(buf)
    }

    val rxResponse: Rx[Response] = dispatcher.apply(
      req,
      NettyBackend.newContext { (request: Request) =>
        Rx.single(Http.response(HttpStatus.NotFound_404))
      }
    )

    RxRunner.run(rxResponse) {
      case OnNext(v) =>
        val nettyResponse = toNettyResponse(v.asInstanceOf[Response])
        writeResponse(ctx, nettyResponse)
      case OnError(ex) =>
        warn(ex)
        val resp = new DefaultHttpResponse(
          HttpVersion.HTTP_1_1,
          HttpResponseStatus.valueOf(HttpStatus.InternalServerError_500.code)
        )
        writeResponse(ctx, resp)
      case OnCompletion =>
    }
  }

  private def writeResponse(ctx: ChannelHandlerContext, resp: DefaultHttpResponse): Unit = {
    ctx.writeAndFlush(resp).addListener(ChannelFutureListener.CLOSE)
  }

  private def toNettyResponse(response: Response): DefaultHttpResponse = {
    val r = if (response.message.isEmpty) {
      new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.valueOf(response.statusCode))
    } else {
      val buf = Unpooled.wrappedBuffer(response.message.toContentBytes)
      val res = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.valueOf(response.statusCode), buf)
      HttpUtil.setContentLength(res, buf.readableBytes())
      res
    }
    response.header.entries.foreach { e =>
      r.headers().set(e.key, e.value)
    }
    r
  }

}
