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

import io.netty.buffer.Unpooled
import io.netty.channel.{ChannelFutureListener, ChannelHandlerContext, SimpleChannelInboundHandler}
import io.netty.handler.codec.http.{
  DefaultFullHttpResponse,
  DefaultHttpResponse,
  FullHttpRequest,
  HttpResponseStatus,
  HttpUtil,
  HttpVersion
}
import wvlet.airframe.Session
import wvlet.airframe.codec.MessageCodecFactory
import wvlet.airframe.http.HttpMessage.{Request, Response}
import wvlet.airframe.http.router.HttpRequestDispatcher
import wvlet.airframe.http.{Http, HttpMethod, HttpStatus}
import wvlet.airframe.rx.{OnCompletion, OnError, OnNext, Rx, RxRunner}
import wvlet.log.LogSupport

import scala.jdk.CollectionConverters._

class NetthRequestHandler(config: NettyServerConfig, session: Session)
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
