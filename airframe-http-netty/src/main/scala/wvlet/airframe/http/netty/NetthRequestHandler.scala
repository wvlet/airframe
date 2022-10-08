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
import io.netty.handler.codec.http._
import wvlet.airframe.http.HttpMessage.{Request, Response}
import wvlet.airframe.http.{Http, HttpMethod, HttpStatus}
import wvlet.airframe.rx.{OnCompletion, OnError, OnNext, Rx, RxRunner}
import wvlet.log.LogSupport

import scala.jdk.CollectionConverters._

class NetthRequestHandler(config: NettyServerConfig, dispatcher: NettyBackend.Filter)
    extends SimpleChannelInboundHandler[FullHttpRequest]
    with LogSupport {

  override def exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable): Unit = {
    warn(cause)
    ctx.close()
  }

  override def channelReadComplete(ctx: ChannelHandlerContext): Unit = {
    ctx.flush()
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

  private def writeResponse(req: HttpRequest, ctx: ChannelHandlerContext, resp: DefaultHttpResponse): Unit = {
    val keepAlive = HttpUtil.isKeepAlive(req)
    if (keepAlive) {
      if (!req.protocolVersion().isKeepAliveDefault) {
        resp.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE)
      }
    } else {
      resp.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE)
    }
    val f = ctx.write(resp)
    if (!keepAlive) {
      f.addListener(ChannelFutureListener.CLOSE)
    }
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
    val h = r.headers()
    response.header.entries.foreach { e =>
      h.set(e.key, e.value)
    }
    r
  }

}
