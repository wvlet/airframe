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
package wvlet.airframe.benchmark.netty_simple

import io.netty.bootstrap.ServerBootstrap
import io.netty.buffer.Unpooled
import io.netty.channel.{
  Channel,
  ChannelFutureListener,
  ChannelHandlerContext,
  ChannelInitializer,
  ChannelOption,
  SimpleChannelInboundHandler
}
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.handler.codec.http.{
  DefaultFullHttpResponse,
  DefaultHttpResponse,
  FullHttpRequest,
  HttpContentCompressor,
  HttpHeaderNames,
  HttpHeaderValues,
  HttpObject,
  HttpObjectAggregator,
  HttpRequest,
  HttpResponseStatus,
  HttpServerCodec,
  HttpServerExpectContinueHandler,
  HttpServerKeepAliveHandler,
  HttpUtil,
  HttpVersion
}
import io.netty.handler.stream.ChunkedWriteHandler
import org.openjdk.jmh.annotations.{Benchmark, BenchmarkMode, Mode, OutputTimeUnit, Scope, Setup, State, TearDown}
import org.openjdk.jmh.infra.Blackhole
import wvlet.airframe.http.Http
import wvlet.airframe.http.client.SyncClient
import wvlet.log.io.IOUtil

import java.util.concurrent.TimeUnit

object NettyHttp {

  class Server(port: Int) {
    private val bossGroup   = new NioEventLoopGroup(1)
    private val workerGroup = new NioEventLoopGroup(4)

    private var ch: Channel = _

    def start: Unit = {
      val b = new ServerBootstrap()
      b.group(bossGroup)
      b.channel(classOf[NioServerSocketChannel])
      b.childOption(ChannelOption.SO_KEEPALIVE, Boolean.box(true))
      b.childOption(ChannelOption.TCP_NODELAY, Boolean.box(true))

      b.childHandler(new ChannelInitializer[Channel] {
        override def initChannel(ch: Channel): Unit = {
          val pipeline = ch.pipeline()
          pipeline.addLast(new HttpServerCodec()) // 4096, 8192, Int.MaxValue, false))
          pipeline.addLast(new HttpObjectAggregator(Int.MaxValue))
          pipeline.addLast(new HttpContentCompressor())
          pipeline.addLast(new HttpServerExpectContinueHandler)
          pipeline.addLast(new HttpServerKeepAliveHandler())
          pipeline.addLast(new ChunkedWriteHandler())
          pipeline.addLast(new SimpleResponseHandler())
        }
      })
      ch = b.bind(port).sync().channel()
    }

    def close: Unit = {
      bossGroup.shutdownGracefully()
      workerGroup.shutdownGracefully()
      Option(ch).foreach(_.close().sync())
    }

  }

  class SimpleResponseHandler extends SimpleChannelInboundHandler[HttpObject] {
    override def channelReadComplete(ctx: ChannelHandlerContext): Unit = {
      ctx.flush()
    }

    private val content = "Hello".getBytes

    override def channelRead0(ctx: ChannelHandlerContext, msg: HttpObject): Unit = {
      msg match {
        case req: HttpRequest =>
          val keepAlive = HttpUtil.isKeepAlive(req)
          val response =
            new DefaultFullHttpResponse(req.protocolVersion, HttpResponseStatus.OK, Unpooled.wrappedBuffer(content))
          response
            .headers().set(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.TEXT_PLAIN)
            .setInt(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes())

          if (keepAlive) {
            if (!req.protocolVersion().isKeepAliveDefault) {
              response.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE)
            }
          } else {
            response.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE)
          }
          val f = ctx.write(response)
          if (!keepAlive) {
            f.addListener(ChannelFutureListener.CLOSE)
          }

        case _ =>
      }
    }

    override def exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable): Unit = {
      cause.printStackTrace()
      ctx.close()
    }
  }
}

@State(Scope.Benchmark)
@BenchmarkMode(Array(Mode.Throughput))
@OutputTimeUnit(TimeUnit.SECONDS)
class NettyHttp {
  import NettyHttp._

  private val port   = IOUtil.unusedPort
  private val server = new Server(port)

  private var client: SyncClient = _

  @Setup
  def setup: Unit = {
    server.start
    client = Http.client.newSyncClient(s"localhost:${port}")
  }

  @TearDown
  def teardown: Unit = {
    server.close
    client.close()
  }

  @Benchmark
  def rpcSync(blackhole: Blackhole): Unit = {
    blackhole.consume(client.send(Http.POST("/").withJson("""{"name":"Netty"}""")))
  }
}
