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

import io.netty.buffer.{ByteBufUtil, Unpooled}
import io.netty.channel.{Channel, ChannelHandlerContext, SimpleChannelInboundHandler}
import io.netty.handler.codec.http.websocketx.*
import wvlet.airframe.http.HttpMessage.Request
import wvlet.airframe.http.{RxHttpFilter, WebSocketContext, WebSocketHandler}
import wvlet.log.LogSupport

import java.util.concurrent.atomic.AtomicBoolean
import scala.util.control.NonFatal

/**
  * A WebSocket route registered on the Netty server. An incoming HTTP upgrade request whose path matches [[path]] is
  * upgraded to a WebSocket connection, after passing through [[filter]] (so auth/logging/metrics filters apply to the
  * handshake). A fresh [[WebSocketHandler]] is created per connection via [[handlerFactory]].
  */
case class WebSocketRoute(
    path: String,
    handlerFactory: Request => WebSocketHandler,
    filter: RxHttpFilter = RxHttpFilter.identity
)

/**
  * A [[WebSocketContext]] backed by a Netty [[Channel]]. Netty's `writeAndFlush` is thread-safe, so send/close may be
  * called from any thread.
  */
private[netty] class NettyWebSocketContext(
    channel: Channel,
    override val request: Request,
    private[netty] val handshaker: WebSocketServerHandshaker
) extends WebSocketContext {

  override def send(text: String): Unit = {
    channel.writeAndFlush(new TextWebSocketFrame(text))
  }

  override def send(data: Array[Byte]): Unit = {
    channel.writeAndFlush(new BinaryWebSocketFrame(Unpooled.wrappedBuffer(data)))
  }

  override def close(): Unit = {
    close(WebSocketCloseStatus.NORMAL_CLOSURE.code(), WebSocketCloseStatus.NORMAL_CLOSURE.reasonText())
  }

  override def close(statusCode: Int, reason: String): Unit = {
    handshaker.close(channel, new CloseWebSocketFrame(statusCode, reason))
  }
}

/**
  * A Netty inbound handler that bridges WebSocket frames to a user-provided [[WebSocketHandler]]. It is installed into
  * the channel pipeline after a successful handshake (see [[NettyRequestHandler]]).
  *
  * Control frames (close/ping/pong) are handled here rather than by Netty's `WebSocketServerProtocolHandler` on
  * purpose: that handler performs its own handshake for a fixed path and cannot gate the upgrade on an
  * [[RxHttpFilter]], which is the whole point of the filter-aware upgrade in [[NettyRequestHandler]].
  */
private[netty] class NettyWebSocketHandler(handler: WebSocketHandler, wsContext: NettyWebSocketContext)
    extends SimpleChannelInboundHandler[WebSocketFrame]
    with LogSupport {

  // Ensure onClose is delivered exactly once (either via a Close frame or channel inactivation)
  private val closeNotified = new AtomicBoolean(false)

  /**
    * Notify the handler that the connection is open. Called by [[NettyRequestHandler]] once the handshake completes.
    */
  private[netty] def notifyOpen(): Unit = {
    safeInvoke(handler.onOpen(wsContext))
  }

  override def channelRead0(ctx: ChannelHandlerContext, frame: WebSocketFrame): Unit = {
    frame match {
      case t: TextWebSocketFrame =>
        safeInvoke(handler.onTextMessage(wsContext, t.text()))
      case b: BinaryWebSocketFrame =>
        safeInvoke(handler.onBinaryMessage(wsContext, ByteBufUtil.getBytes(b.content())))
      case c: CloseWebSocketFrame =>
        notifyClose()
        // Echo the close frame back and close the connection (retain to balance the auto-release)
        wsContext.handshaker.close(ctx.channel(), c.retain())
      case _: PingWebSocketFrame =>
        // Respond to ping with a pong carrying the same payload
        ctx.writeAndFlush(new PongWebSocketFrame(frame.content().retain()))
      case _: PongWebSocketFrame =>
      // Ignore unsolicited pongs
      case other =>
        debug(s"Ignoring unsupported WebSocket frame: ${other.getClass.getName}")
    }
  }

  override def channelInactive(ctx: ChannelHandlerContext): Unit = {
    notifyClose()
    super.channelInactive(ctx)
  }

  override def exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable): Unit = {
    if (NettyRequestHandler.isBenignIOException(cause)) {
      debug(cause)
    } else {
      safeOnError(cause)
    }
    ctx.close()
  }

  private def notifyClose(): Unit = {
    if (closeNotified.compareAndSet(false, true)) {
      try {
        handler.onClose(wsContext)
      } catch {
        case NonFatal(e) => warn(e)
      }
    }
  }

  // Invoke a user callback, routing any non-fatal exception to onError
  private def safeInvoke(body: => Unit): Unit = {
    try {
      body
    } catch {
      case NonFatal(e) => safeOnError(e)
    }
  }

  private def safeOnError(e: Throwable): Unit = {
    try {
      handler.onError(wsContext, e)
    } catch {
      case NonFatal(e2) => warn(e2)
    }
  }
}
