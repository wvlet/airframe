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
import io.netty.channel.{ChannelFutureListener, ChannelHandlerContext, ChannelInboundHandlerAdapter}
import io.netty.handler.codec.http.*
import wvlet.airframe.http.HttpMessage.{Request, Response}
import wvlet.airframe.http.internal.HttpLogs
import wvlet.airframe.http.{Http, HttpHeader, HttpLogger, HttpStatus, RPCException, RPCStatus, ServerSentEvent}
import wvlet.airframe.rx.{OnCompletion, OnError, OnNext, Rx, RxRunner}
import wvlet.log.LogSupport

import java.util.concurrent.{SynchronousQueue, ThreadFactory, ThreadPoolExecutor, TimeUnit}
import java.util.concurrent.atomic.AtomicInteger
import scala.collection.immutable.ListMap
import NettyRequestHandler.toNettyResponse

/**
  * Handles fully-assembled HttpMessage.Request objects from NettyBodyHandler. The request body is already available as
  * a Message (either ByteArrayMessage for small bodies or InputStreamMessage for large bodies backed by a temp file).
  */
class NettyRequestHandler(config: NettyServerConfig, dispatcher: NettyBackend.Filter, httpStreamLogger: HttpLogger)
    extends ChannelInboundHandlerAdapter
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

  override def channelRead(ctx: ChannelHandlerContext, msg: Any): Unit = {
    msg match {
      case req: Request =>
        handleRequest(ctx, req)
      case _ =>
        ctx.fireChannelRead(msg)
    }
  }

  private def handleRequest(ctx: ChannelHandlerContext, req: Request): Unit = {
    try {
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
          writeResponse(ctx, nettyResponse)

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
          writeResponse(ctx, nettyResponse)
        case OnCompletion =>
      }
    } catch {
      case e: RPCException =>
        writeResponse(ctx, toNettyResponse(e.toResponse))
    } finally {
      // Clean up temp file if body was file-backed
      NettyBodyHandler.cleanupTempFile(ctx)
      // Need to clean up the TLS in case the same thread is reused for the next request
      NettyBackend.clearThreadLocal()
    }
  }

  private def writeResponse(ctx: ChannelHandlerContext, resp: DefaultHttpResponse): Unit = {
    val isEventStream =
      Option(resp.headers())
        .flatMap(h => Option(h.get(HttpHeader.ContentType)))
        .exists(_.contains("text/event-stream"))

    // HTTP/1.1 defaults to keep-alive. Close only on error responses (non-successful status).
    val keepAlive: Boolean =
      HttpStatus.ofCode(resp.status().code()).isSuccessful || isEventStream

    if (!keepAlive) {
      resp.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE)
    }
    val f = ctx.writeAndFlush(resp)
    if (!keepAlive) {
      f.addListener(ChannelFutureListener.CLOSE)
    }
  }

}

object NettyRequestHandler extends LogSupport {

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
