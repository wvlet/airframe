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
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.channel._
import io.netty.handler.codec.http._
import io.netty.handler.stream.ChunkedWriteHandler
import wvlet.log.LogSupport
import wvlet.log.io.IOUtil

case class NettyServerConfig(serverPort: Option[Int] = None) {
  lazy val port = serverPort.getOrElse(IOUtil.unusedPort)

  def newServer: NettyServer = {
    val s = new NettyServer(this)
    s.start
    s
  }
}

class NettyServer(config: NettyServerConfig) extends AutoCloseable with LogSupport {
  private val bossGroup                            = new NioEventLoopGroup(1)
  private val workerGroup                          = new NioEventLoopGroup()
  private var channelFuture: Option[ChannelFuture] = None

  def localAddress: String = s"localhost:${config.port}"

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
        pipeline.addLast(new HttpRequestHandler())
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

class HttpRequestHandler extends SimpleChannelInboundHandler[FullHttpRequest] with LogSupport {
  override def channelRead0(ctx: ChannelHandlerContext, msg: FullHttpRequest): Unit = {
    debug(s"request: ${msg}")

    // TODO Add RPC handlers
    val response = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK)
    // TODO Add various headers
    debug(response)
    ctx.writeAndFlush(response)
    ctx.close()
  }
}
