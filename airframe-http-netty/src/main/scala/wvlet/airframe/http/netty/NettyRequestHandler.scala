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
import io.netty.channel.{ChannelFuture, ChannelFutureListener, ChannelHandlerContext, SimpleChannelInboundHandler}
import io.netty.handler.codec.http.*
import io.netty.handler.codec.http.websocketx.{WebSocketServerHandshaker, WebSocketServerHandshakerFactory}
import io.netty.util.concurrent.EventExecutorGroup
import wvlet.airframe.http.HttpMessage.{Request, Response}
import wvlet.airframe.http.internal.{HttpLogs, RPCResponseFilter}
import wvlet.airframe.http.{
  Http,
  HttpHeader,
  HttpLogger,
  HttpMethod,
  HttpServerException,
  HttpStatus,
  RPCException,
  RPCStatus,
  RxHttpFilter,
  ServerAddress,
  ServerSentEvent
}
import wvlet.airframe.rx.{Cancelable, OnCompletion, OnError, OnNext, Rx, RxRunner}
import wvlet.log.LogSupport

import java.net.InetSocketAddress
import java.util.concurrent.{SynchronousQueue, ThreadFactory, ThreadPoolExecutor, TimeUnit}
import java.util.concurrent.atomic.AtomicInteger
import scala.collection.immutable.ListMap
import scala.jdk.CollectionConverters.*
import scala.util.control.NonFatal
import NettyRequestHandler.toNettyResponse

import java.io.ByteArrayOutputStream

class NettyRequestHandler(
    config: NettyServerConfig,
    dispatcher: NettyBackend.Filter,
    httpStreamLogger: HttpLogger,
    // Filter applied to the WebSocket upgrade request (e.g. installs the RPC context) before the route's own filter
    wsUpgradeFilter: RxHttpFilter,
    // Executor for offloading WebSocket frame callbacks off the event loop, when handlerExecutorThreads is configured
    wsHandlerExecutor: Option[EventExecutorGroup]
) extends SimpleChannelInboundHandler[FullHttpRequest]
    with LogSupport {

  override def exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable): Unit = {
    if (NettyRequestHandler.isBenignIOException(cause)) {
      // Log benign I/O errors at DEBUG level to reduce log noise
      debug(cause)
    } else {
      warn(cause)
    }
    ctx.close()
  }

  override def channelReadComplete(ctx: ChannelHandlerContext): Unit = {
    ctx.flush()
  }

  override def channelRead0(ctx: ChannelHandlerContext, msg: FullHttpRequest): Unit = {
    try {
      val req = NettyRequestHandler.toAirframeRequest(ctx, msg)
      webSocketRouteFor(req, msg) match {
        case Some(route) =>
          handleWebSocketUpgrade(ctx, msg, req, route)
        case None =>
          dispatchHttp(ctx, msg, req)
      }
    } catch {
      case e: RPCException =>
        writeResponse(msg, ctx, toNettyResponse(e.toResponse))
    } finally {
      // Need to clean up the TLS in case the same thread is reused for the next request
      NettyBackend.clearThreadLocal()
    }
  }

  /**
    * Find a registered WebSocket route matching this request, only if it is a WebSocket upgrade request.
    */
  private def webSocketRouteFor(req: Request, msg: FullHttpRequest): Option[WebSocketRoute] = {
    if (config.webSocketRoutes.isEmpty || !NettyRequestHandler.isWebSocketUpgrade(msg)) {
      None
    } else {
      config.webSocketRoutes.find(_.path == req.path)
    }
  }

  /**
    * Run the upgrade request through the route's filter chain and, if it is allowed, perform the WebSocket handshake.
    * The filter chain lets auth/logging/metrics filters apply to the handshake; a non-2xx response rejects the upgrade.
    */
  private def handleWebSocketUpgrade(
      ctx: ChannelHandlerContext,
      msg: FullHttpRequest,
      req: Request,
      route: WebSocketRoute
  ): Unit = {
    // The handshake may run later (async filter and/or event loop hop), so keep the request alive
    msg.retain()
    // Act on the first terminal outcome only: guards against a filter that emits multiple responses (double
    // handshake/release) or completes without emitting one (leaking the retained request)
    val handled = new java.util.concurrent.atomic.AtomicBoolean(false)
    // Run the upgrade request through the filter chain, terminating in a 200 marker that signals "upgrade allowed"
    val filtered: Rx[Response] =
      wsUpgradeFilter
        .andThen(route.filter).andThen { (_: Request) => Rx.single(Http.response(HttpStatus.Ok_200)) }
        .apply(req)
    RxRunner.run(filtered) {
      case OnNext(v) =>
        if (handled.compareAndSet(false, true)) {
          val resp = v.asInstanceOf[Response]
          if (resp.status.isSuccessful) {
            doWebSocketHandshake(ctx, msg, req, route)
          } else {
            // A filter rejected the upgrade (e.g. failed auth); return its response without upgrading
            writeResponse(msg, ctx, toNettyResponse(resp))
            msg.release()
          }
        }
      case OnError(ex) =>
        if (handled.compareAndSet(false, true)) {
          writeResponse(
            msg,
            ctx,
            toNettyResponse(RPCStatus.INTERNAL_ERROR_I0.newException(ex.getMessage, ex).toResponse)
          )
          msg.release()
        }
      case OnCompletion =>
        // The filter chain completed without emitting a response: release the retained request and close the
        // connection so the client does not hang waiting for a handshake response that will never come.
        if (handled.compareAndSet(false, true)) {
          msg.release()
          ctx.close()
        }
    }
  }

  /**
    * Perform the Netty WebSocket handshake on the channel's event loop and wire the connection to a fresh
    * [[NettyWebSocketHandler]]. Pipeline mutation must happen on the event loop thread.
    */
  private def doWebSocketHandshake(
      ctx: ChannelHandlerContext,
      msg: FullHttpRequest,
      req: Request,
      route: WebSocketRoute
  ): Unit = {
    ctx.channel().eventLoop().execute { () =>
      try {
        val location  = NettyRequestHandler.webSocketLocation(msg)
        val wsFactory = new WebSocketServerHandshakerFactory(location, null, true, config.webSocketMaxFrameSize)
        val handshaker: WebSocketServerHandshaker = wsFactory.newHandshaker(msg)
        if (handshaker == null) {
          WebSocketServerHandshakerFactory.sendUnsupportedVersionResponse(ctx.channel())
        } else {
          val wsContext   = new NettyWebSocketContext(ctx.channel(), req, handshaker)
          val userHandler = route.handlerFactory(req)
          val wsHandler   = new NettyWebSocketHandler(userHandler, wsContext)
          val future      = handshaker.handshake(ctx.channel(), msg)
          // The handshake reconfigures the pipeline with WebSocket codecs and drops the HTTP aggregator/codec.
          // Install the frame handler and remove this HTTP request handler. Offload frame callbacks to the handler
          // executor when configured, so blocking user callbacks do not stall the event loop (matching HTTP handlers).
          val pipeline = ctx.pipeline()
          wsHandlerExecutor match {
            case Some(executor) => pipeline.addLast(executor, "wsHandler", wsHandler)
            case None           => pipeline.addLast("wsHandler", wsHandler)
          }
          pipeline.remove(NettyRequestHandler.this)
          // Notify onOpen synchronously on the event loop so it always precedes any inbound frame (the event loop
          // will not process the next read until this task returns). Outbound writes from onOpen are still queued
          // after the handshake response, so frame ordering on the wire is preserved.
          wsHandler.notifyOpen()
          future.addListener { (f: ChannelFuture) =>
            // A failed handshake-response write is typically a benign client disconnect; close the channel and let
            // channelInactive deliver onClose rather than surfacing a noisy onError.
            if (!f.isSuccess) {
              ctx.close()
            }
          }
        }
      } catch {
        case NonFatal(e) =>
          warn("Failed to perform WebSocket handshake", e)
          ctx.close()
      } finally {
        msg.release()
      }
    }
  }

  private def dispatchHttp(ctx: ChannelHandlerContext, msg: FullHttpRequest, req: Request): Unit = {
    // Dispatch the request and get an async response, Rx[Response]
    val rxResponse: Rx[Response] = dispatcher.apply(
      req,
      NettyBackend.newContext { (request: Request) =>
        Rx.single(Http.response(HttpStatus.NotFound_404))
      }
    )

    RxRunner.run(rxResponse) {
      case OnNext(v) =>
        val resp          = v.asInstanceOf[Response]
        val nettyResponse = toNettyResponse(resp)
        writeResponse(msg, ctx, nettyResponse)

        if (resp.isContentTypeEventStream && resp.message.isEmpty) {
          // Capture request context and timing before handing off to SSE executor thread
          val streamStartTime = System.currentTimeMillis()
          val streamStartNano = System.nanoTime()
          val requestMethod   = req.method.toString
          val requestPath     = req.path
          val requestUri      = req.uri
          val remoteAddr      = req.remoteAddress.map(_.hostAndPort)
          val responseStatus  = resp.status
          val eventCounter    = new AtomicInteger(0)

          def writeStreamLog(status: HttpStatus, error: Option[Throwable]): Unit = {
            val m = ListMap.newBuilder[String, Any]
            m ++= HttpLogs.unixTimeLogs(streamStartTime)
            m += "method" -> requestMethod
            m += "path"   -> requestPath
            m += "uri"    -> requestUri
            remoteAddr.foreach(a => m += "remote_address" -> a)
            m ++= HttpLogs.durationLogs(streamStartTime, streamStartNano)
            m += "event_count"      -> eventCounter.get()
            m += "status_code"      -> status.code
            m += "status_code_name" -> status.reason
            error.foreach { e =>
              m += "error_message" -> e.getMessage
              m += "exception"     -> e
            }
            httpStreamLogger.write(m.result())
          }

          // Run SSE stream consumption in a separate thread to avoid blocking the Netty worker.
          // ctx.writeAndFlush() is thread-safe in Netty and can be called from any thread.
          try {
            NettyRequestHandler.sseExecutor.execute { () =>
              RxRunner.run(resp.events) {
                case OnNext(e: ServerSentEvent) =>
                  eventCounter.incrementAndGet()
                  val event = e.toContent
                  val buf   = Unpooled.copiedBuffer(event.getBytes("UTF-8"))
                  ctx.writeAndFlush(new DefaultHttpContent(buf))
                case OnError(e) =>
                  writeStreamLog(HttpStatus.InternalServerError_500, Some(e))
                  if (ctx.channel().isActive) {
                    ctx
                      .writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT)
                      .addListener(ChannelFutureListener.CLOSE)
                  }
                case _ =>
                  writeStreamLog(responseStatus, None)
                  val f = ctx.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT)
                  f.addListener(ChannelFutureListener.CLOSE)
              }
            }
          } catch {
            case e: java.util.concurrent.RejectedExecutionException =>
              warn(s"SSE executor is saturated; closing stream", e)
              writeStreamLog(HttpStatus.ServiceUnavailable_503, Some(e))
              ctx
                .writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT)
                .addListener(ChannelFutureListener.CLOSE)
          }
        }
      case OnError(ex) =>
        // This path manages unhandled exceptions
        val resp          = RPCStatus.INTERNAL_ERROR_I0.newException(ex.getMessage, ex).toResponse
        val nettyResponse = toNettyResponse(resp)
        writeResponse(msg, ctx, nettyResponse)
      case OnCompletion =>
    }
  }

  private def writeResponse(req: HttpRequest, ctx: ChannelHandlerContext, resp: DefaultHttpResponse): Unit = {
    val isEventStream =
      Option(resp.headers())
        .flatMap(h => Option(h.get(HttpHeader.ContentType)))
        .exists(_.contains("text/event-stream"))

    val keepAlive: Boolean =
      HttpStatus.ofCode(resp.status().code()).isSuccessful && (HttpUtil.isKeepAlive(req) || isEventStream)

    if (keepAlive) {
      if (!req.protocolVersion().isKeepAliveDefault) {
        resp.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE)
      }
    } else {
      resp.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE)
    }
    val f = ctx.writeAndFlush(resp)
    if (!keepAlive) {
      f.addListener(ChannelFutureListener.CLOSE)
    }
  }

}

object NettyRequestHandler extends LogSupport {

  /**
    * Convert a Netty [[FullHttpRequest]] into an Airframe [[Request]]. Shared by the HTTP dispatch and WebSocket
    * upgrade paths.
    */
  private[netty] def toAirframeRequest(ctx: ChannelHandlerContext, msg: FullHttpRequest): Request = {
    var req: Request = msg.method().name().toUpperCase match {
      case HttpMethod.GET     => Http.GET(msg.uri())
      case HttpMethod.POST    => Http.POST(msg.uri())
      case HttpMethod.PUT     => Http.PUT(msg.uri())
      case HttpMethod.DELETE  => Http.DELETE(msg.uri())
      case HttpMethod.PATCH   => Http.PATCH(msg.uri())
      case HttpMethod.TRACE   => Http.request(wvlet.airframe.http.HttpMethod.TRACE, msg.uri())
      case HttpMethod.OPTIONS => Http.request(wvlet.airframe.http.HttpMethod.OPTIONS, msg.uri())
      case HttpMethod.HEAD    => Http.request(wvlet.airframe.http.HttpMethod.HEAD, msg.uri())
      case _ =>
        throw RPCStatus.INVALID_REQUEST_U1.newException(s"Unsupported HTTP method: ${msg.method()}")
    }

    // Set remote address for logging purpose
    ctx.channel().remoteAddress() match {
      case x: InetSocketAddress =>
        // TODO This address might be IPv6
        req = req.withRemoteAddress(ServerAddress(s"${x.getHostString}:${x.getPort}"))
      case _ =>
    }

    // Read request headers
    msg.headers().names().asScala.foreach { x =>
      req = req.withHeader(x, msg.headers().get(x))
    }

    // Read request body
    var bodyBuf: ByteArrayOutputStream = null
    val requestBody                    = msg.content()
    while (requestBody.isReadable) {
      // the returned size is greater than 0 when isReadable = true
      val size = requestBody.readableBytes()
      if (bodyBuf == null) {
        bodyBuf = new ByteArrayOutputStream(size)
      }
      requestBody.readBytes(bodyBuf, size)
    }
    if (bodyBuf != null && bodyBuf.size() > 0) {
      req = req.withContent(bodyBuf.toByteArray)
    }
    req
  }

  /**
    * Check whether the request is a WebSocket upgrade request (Connection: Upgrade and Upgrade: websocket).
    */
  private[netty] def isWebSocketUpgrade(msg: HttpRequest): Boolean = {
    val headers = msg.headers()
    headers.containsValue(HttpHeaderNames.CONNECTION, HttpHeaderValues.UPGRADE, true) &&
    headers.containsValue(HttpHeaderNames.UPGRADE, HttpHeaderValues.WEBSOCKET, true)
  }

  /**
    * Build the WebSocket URL used by Netty's handshaker. TLS is not yet supported by this server, so the scheme is
    * ws://
    */
  private[netty] def webSocketLocation(req: HttpRequest): String = {
    val host = Option(req.headers().get(HttpHeaderNames.HOST)).getOrElse("localhost")
    s"ws://${host}${req.uri()}"
  }

  // Thread pool for SSE stream consumption to avoid blocking Netty worker threads.
  // Bounded to 64 threads to prevent unbounded creation under high SSE load.
  // Idle threads are reclaimed after 60 seconds. Daemon threads don't prevent JVM shutdown.
  private[netty] val sseExecutor = new ThreadPoolExecutor(
    0,
    64,
    60L,
    TimeUnit.SECONDS,
    new SynchronousQueue[Runnable](),
    new ThreadFactory {
      private val counter = new AtomicInteger(0)
      override def newThread(r: Runnable): Thread = {
        val t = new Thread(r, s"airframe-netty-sse-${counter.getAndIncrement()}")
        t.setDaemon(true)
        t
      }
    }
  )

  // "Connection reset" also matches "Connection reset by peer" via contains check
  private val benignIOExceptionMessages = Set(
    "Connection reset",
    "Broken pipe"
  )

  /**
    * Check if the exception is a benign I/O error that commonly occurs during normal operations, such as client
    * disconnections or network interruptions. These exceptions should be logged at DEBUG level. This method traverses
    * the entire exception cause chain to handle cases where benign I/O exceptions are wrapped by other exceptions.
    */
  def isBenignIOException(cause: Throwable): Boolean = {
    @scala.annotation.tailrec
    def loop(ex: Throwable): Boolean = {
      if (ex == null) {
        false
      } else {
        ex match {
          case _: io.netty.handler.codec.PrematureChannelClosureException =>
            true
          case _: java.nio.channels.ClosedChannelException =>
            true
          case e: java.io.IOException =>
            val msg = e.getMessage
            if (msg != null && benignIOExceptionMessages.exists(m => msg.contains(m))) {
              true
            } else {
              loop(ex.getCause)
            }
          case _ =>
            loop(ex.getCause)
        }
      }
    }
    loop(cause)
  }

  def toNettyResponse(response: Response): DefaultHttpResponse = {
    val r = if (response.isContentTypeEventStream && response.message.isEmpty) {
      val res = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.valueOf(response.statusCode))
      res.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/event-stream")
      res.headers().set(HttpHeaderNames.TRANSFER_ENCODING, HttpHeaderValues.CHUNKED)
      res.headers().set(HttpHeaderNames.CACHE_CONTROL, HttpHeaderValues.NO_CACHE)
      res.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE)
      res
    } else if (response.message.isEmpty) {
      val res = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.valueOf(response.statusCode))
      // Need to set the content length properly to return the response in Netty
      HttpUtil.setContentLength(res, 0)
      res
    } else {
      val contents = response.message.toContentBytes
      val buf      = Unpooled.wrappedBuffer(contents)
      val res = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.valueOf(response.statusCode), buf)
      HttpUtil.setContentLength(res, contents.size)
      res
    }
    val h = r.headers()
    response.header.entries.foreach { e =>
      h.set(e.key, e.value)
    }
    r
  }
}
