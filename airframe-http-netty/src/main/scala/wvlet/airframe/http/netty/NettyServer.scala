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
import io.netty.buffer.{ByteBuf, Unpooled}
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.channel._
import io.netty.channel.epoll.{Epoll, EpollEventLoopGroup, EpollServerSocketChannel}
import io.netty.channel.local.LocalServerChannel
import io.netty.channel.unix.UnixChannelOption
import io.netty.handler.codec.http._
import io.netty.handler.stream.ChunkedWriteHandler
import io.netty.util.ReferenceCountUtil
import wvlet.airframe.codec.{JSONCodec, MessageCodec, MessageCodecFactory}
import wvlet.airframe.http.HttpBackend.DefaultBackend
import wvlet.airframe.http.HttpMessage.{ByteArrayMessage, EmptyMessage, Request, Response, StringMessage}
import wvlet.airframe.{Design, Session}
import wvlet.airframe.http.client.SyncClient
import wvlet.airframe.http.router.{ControllerProvider, HttpRequestDispatcher, ResponseHandler, Route}
import wvlet.airframe.http.{Http, HttpBackend, HttpRequestAdapter, HttpStatus, Router}
import wvlet.airframe.msgpack.spi.MsgPack
import wvlet.airframe.rx.{OnCompletion, OnError, OnNext, Rx, RxRunner}
import wvlet.airframe.surface.{Primitive, Surface}
import wvlet.log.LogSupport
import wvlet.log.io.IOUtil

import scala.jdk.CollectionConverters._
import java.util.function.Consumer
import javax.annotation.PostConstruct
import scala.collection.mutable
import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.util.{Failure, Success}

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

  def localAddress: String = s"localhost:${config.port}"

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
    b.option(ChannelOption.SO_BACKLOG, Int.box(1000))
    b.childOption(ChannelOption.SO_KEEPALIVE, Boolean.box(true))
    b.childOption(ChannelOption.TCP_NODELAY, Boolean.box(true))
    b.childHandler(new ChannelInitializer[SocketChannel] {
      override def initChannel(ch: SocketChannel): Unit = {
        val pipeline = ch.pipeline()
        pipeline.addLast(new HttpServerCodec())
        pipeline.addLast(new HttpContentCompressor())
        pipeline.addLast(new ChunkedWriteHandler())
        pipeline.addLast(new HttpServerKeepAliveHandler())
        pipeline.addLast(new HttpObjectAggregator(65536))
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

  override def channelRead0(ctx: ChannelHandlerContext, msg: FullHttpRequest): Unit = {
    var req: wvlet.airframe.http.HttpMessage.Request = msg.method() match {
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
    val buf             = new Array[Byte](requestBodySize)
    requestBody.getBytes(requestBody.readerIndex(), buf)
    req = req.withContent(buf)

    val rxResponse: Rx[Response] = dispatcher.apply(
      req,
      NettyBackend.newContext { (request: Request) =>
        Rx.single(Http.response(HttpStatus.NotFound_404))
      }
    )

    RxRunner.run(rxResponse) {
      case OnNext(v) =>
        val nettyResponse = toNettyResponse(v.asInstanceOf[Response])
        writeResponse(msg, ctx, nettyResponse)
      case OnError(ex) =>
        warn(ex)
        val resp = new DefaultHttpResponse(
          HttpVersion.HTTP_1_1,
          HttpResponseStatus.valueOf(HttpStatus.InternalServerError_500.code)
        )
        writeResponse(msg, ctx, resp)
      case OnCompletion =>
    }
  }

  private def writeResponse(req: FullHttpRequest, ctx: ChannelHandlerContext, resp: DefaultHttpResponse): Unit = {
//    val isKeepAlive = HttpUtil.isKeepAlive(req)
//    if (isKeepAlive) {
//      resp.headers.set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE)
//    }
    ctx.writeAndFlush(resp)
    ctx.close()
  }

  private def toNettyResponse(response: Response): DefaultHttpResponse = {
    val r = if (response.message.isEmpty) {
      new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.valueOf(response.statusCode))
    } else {
      val buf = Unpooled.wrappedBuffer(response.message.toContentBytes)
      new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.valueOf(response.statusCode), buf)
    }
    response.header.entries.foreach { e =>
      r.headers().set(e.key, e.value)
    }
    r
  }

}

class NettyResponseHandler extends ResponseHandler[Request, Response] with LogSupport {
  private def codecFactory = MessageCodecFactory.defaultFactoryForJSON

  override def toHttpResponse[A](route: Route, request: Request, responseSurface: Surface, a: A): Response = {
    a match {
      case null =>
        newResponse(route, request, responseSurface)
      case r: Response =>
        r
      case b: MsgPack if request.acceptsMsgPack =>
        newResponse(route, request, responseSurface).withContentTypeMsgPack
          .withContent(b)
      case s: String if !request.acceptsMsgPack =>
        newResponse(route, request, responseSurface)
          .withContent(s)
      case _ =>
        val rs = codecFactory.of(responseSurface)
        val msgpack: Array[Byte] = rs match {
          case m: MessageCodec[_] =>
            m.asInstanceOf[MessageCodec[A]].toMsgPack(a)
          case _ =>
            throw new IllegalArgumentException(s"Unknown codec: ${rs}")
        }

        // Return application/x-msgpack content type
        if (request.acceptsMsgPack) {
          newResponse(route, request, responseSurface).withContentTypeMsgPack
            .withContent(msgpack)
        } else {
          val json = JSONCodec.unpackMsgPack(msgpack)
          json match {
            case Some(j) =>
              newResponse(route, request, responseSurface)
                .withJson(json.get)
            case None =>
              Http.response(HttpStatus.InternalServerError_500)
          }
        }
    }
  }

  private def newResponse(route: Route, request: Request, responseSurface: Surface): Response = {
    if (responseSurface == Primitive.Unit) {
      request.method match {
        case wvlet.airframe.http.HttpMethod.POST if route.isRPC =>
          // For RPC, return 200 even for POST
          Http.response(HttpStatus.Ok_200)
        case wvlet.airframe.http.HttpMethod.POST | wvlet.airframe.http.HttpMethod.PUT =>
          Http.response(HttpStatus.Created_201)
        case wvlet.airframe.http.HttpMethod.DELETE =>
          Http.response(HttpStatus.NoContent_204)
        case _ =>
          Http.response(HttpStatus.Ok_200)
      }
    } else {
      Http.response(HttpStatus.Ok_200)
    }
  }
}

object NettyBackend extends HttpBackend[Request, Response, Rx] {
  override protected implicit val httpRequestAdapter: HttpRequestAdapter[Request] =
    wvlet.airframe.http.HttpMessage.HttpMessageRequestAdapter

  override def name: String = "netty"

  override def newResponse(status: HttpStatus, content: String): Response = {
    Http.response(status).withContent(content)
  }

  override def toFuture[A](a: A): Rx[A] = {
    Rx.single(a)
  }

  override def toFuture[A](a: Future[A], e: ExecutionContext): Rx[A] = {
    Rx.future(a)(e)
  }

  override def toScalaFuture[A](a: Rx[A]): Future[A] = {
    ???
    val promise: Promise[A] = Promise()
    a.toRxStream
      .map { x =>
        promise.success(x)
      }
      .recover { case e: Throwable => promise.failure(e) }
    promise.future
  }

  override def wrapException(e: Throwable): Rx[Response] = {
    Rx.exception(e)
  }

  override def isFutureType(x: Class[_]): Boolean = {
    classOf[Rx[_]].isAssignableFrom(x)
  }

  override def isRawResponseType(x: Class[_]): Boolean = {
    classOf[Response].isAssignableFrom(x)
  }

  override def mapF[A, B](f: Rx[A], body: A => B): Rx[B] = {
    f.toRxStream.map(body)
  }

  private lazy val tls =
    ThreadLocal.withInitial[collection.mutable.Map[String, Any]](() => mutable.Map.empty[String, Any])

  private def storage: collection.mutable.Map[String, Any] = tls.get()

  override def withThreadLocalStore(request: => Rx[Response]): Rx[Response] = {
    //
    request
  }

  override def setThreadLocal[A](key: String, value: A): Unit = {
    storage.put(key, value)
  }

  override def getThreadLocal[A](key: String): Option[A] = {
    storage.get(key).asInstanceOf[Option[A]]
  }
}
