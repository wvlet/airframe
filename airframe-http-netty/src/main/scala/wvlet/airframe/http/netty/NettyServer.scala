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
import io.netty.handler.codec.http._
import io.netty.handler.stream.ChunkedWriteHandler
import wvlet.airframe.codec.{JSONCodec, MessageCodec, MessageCodecFactory}
import wvlet.airframe.http.HttpBackend.DefaultBackend
import wvlet.airframe.http.HttpMessage.{ByteArrayMessage, EmptyMessage, Request, Response, StringMessage}
import wvlet.airframe.{Design, Session}
import wvlet.airframe.http.client.SyncClient
import wvlet.airframe.http.router.{ControllerProvider, HttpRequestDispatcher, ResponseHandler, Route}
import wvlet.airframe.http.{Http, HttpStatus, Router}
import wvlet.airframe.msgpack.spi.MsgPack
import wvlet.airframe.surface.{Primitive, Surface}
import wvlet.log.LogSupport
import wvlet.log.io.IOUtil

import scala.jdk.CollectionConverters._
import java.util.function.Consumer
import javax.annotation.PostConstruct
import scala.concurrent.Future
import scala.util.{Failure, Success}

case class NettyServerConfig(
    serverPort: Option[Int] = None,
    controllerProvider: ControllerProvider = ControllerProvider.defaultControllerProvider,
    router: Router = Router.empty
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
  private val bossGroup                            = new NioEventLoopGroup(1)
  private val workerGroup                          = new NioEventLoopGroup()
  private var channelFuture: Option[ChannelFuture] = None

  def localAddress: String = s"localhost:${config.port}"

  @PostConstruct
  def start: Unit = {
    info(s"Starting server at ${localAddress}")
    val b = new ServerBootstrap()
    b.group(bossGroup, workerGroup)
    b.channel(classOf[NioServerSocketChannel])
    b.childOption(ChannelOption.SO_KEEPALIVE, Boolean.box(true))
    b.childHandler(new ChannelInitializer[SocketChannel] {
      override def initChannel(ch: SocketChannel): Unit = {
        val pipeline = ch.pipeline()
        pipeline.addLast(new HttpServerCodec())
        pipeline.addLast(new HttpObjectAggregator(65536))
        pipeline.addLast(new ChunkedWriteHandler())
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
    // TODO Use Rx-based backend
    DefaultBackend,
    new NettyResponseHandler,
    MessageCodecFactory.defaultFactoryForJSON
  )

  private implicit val executionContext = DefaultBackend.executionContext

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

    val futureResponse = dispatcher.apply(
      req,
      DefaultBackend.newContext { (request: Request) =>
        Future.apply(Http.response(HttpStatus.NotFound_404))
      }
    )

    futureResponse.onComplete {
      case Success(response) =>
        val nettyResponse = toNettyResponse(response)
        ctx.write(nettyResponse)
        ctx.flush()
        ctx.close()
      case Failure(ex) =>
        warn(ex)
        ctx.close()
    }
  }

  private def toNettyResponse(response: Response): DefaultHttpResponse = {
    val r = if (response.message.isEmpty) {
      new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.valueOf(response.statusCode))
    } else {
      val buf = Unpooled.wrappedBuffer(response.message.toContentBytes)
      new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.valueOf(response.statusCode), buf)
    }
    response.header.entries.foreach { e =>
      r.headers().set(e.value, e.key)
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
