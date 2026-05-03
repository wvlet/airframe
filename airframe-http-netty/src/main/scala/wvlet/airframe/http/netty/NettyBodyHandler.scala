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

import io.netty.channel.{ChannelFutureListener, ChannelHandlerContext, ChannelInboundHandlerAdapter}
import io.netty.handler.codec.http.{HttpContent, HttpRequest, HttpUtil, LastHttpContent}
import io.netty.util.{AttributeKey, ReferenceCountUtil}
import wvlet.airframe.http.{Http, HttpMethod, InputStreamMessage, RPCException, RPCStatus, ServerAddress}
import wvlet.log.LogSupport

import java.io.{ByteArrayOutputStream, File, FileInputStream, FileOutputStream}
import java.net.InetSocketAddress
import java.nio.file.Files
import scala.jdk.CollectionConverters.*

/**
  * A Netty channel handler that replaces HttpObjectAggregator. It assembles incoming HTTP chunks into an
  * HttpMessage.Request, buffering small bodies in memory and spilling large bodies to a temp file to reduce heap usage.
  *
  * @param bodyBufferThresholdBytes
  *   Bodies larger than this threshold are written to a temp file instead of held in memory.
  */
class NettyBodyHandler(bodyBufferThresholdBytes: Long) extends ChannelInboundHandlerAdapter with LogSupport {
  import NettyBodyHandler.*

  override def channelRead(ctx: ChannelHandlerContext, msg: Any): Unit = {
    msg match {
      case httpRequest: HttpRequest =>
        try {
          handleHttpRequest(ctx, httpRequest)
        } catch {
          case e: RPCException =>
            ReferenceCountUtil.release(msg)
            val resp = NettyRequestHandler.toNettyResponse(e.toResponse)
            ctx.writeAndFlush(resp).addListener(ChannelFutureListener.CLOSE)
        }

      case content: HttpContent =>
        handleContent(ctx, content)

      case other =>
        ctx.fireChannelRead(other)
    }
  }

  private def handleHttpRequest(ctx: ChannelHandlerContext, httpRequest: HttpRequest): Unit = {
    // Start of a new request: build the airframe Request with method, URI, headers, remote address
    var req = httpRequest.method().name().toUpperCase match {
      case HttpMethod.GET     => Http.GET(httpRequest.uri())
      case HttpMethod.POST    => Http.POST(httpRequest.uri())
      case HttpMethod.PUT     => Http.PUT(httpRequest.uri())
      case HttpMethod.DELETE  => Http.DELETE(httpRequest.uri())
      case HttpMethod.PATCH   => Http.PATCH(httpRequest.uri())
      case HttpMethod.TRACE   => Http.request(wvlet.airframe.http.HttpMethod.TRACE, httpRequest.uri())
      case HttpMethod.OPTIONS => Http.request(wvlet.airframe.http.HttpMethod.OPTIONS, httpRequest.uri())
      case HttpMethod.HEAD    => Http.request(wvlet.airframe.http.HttpMethod.HEAD, httpRequest.uri())
      case _ =>
        throw RPCStatus.INVALID_REQUEST_U1.newException(s"Unsupported HTTP method: ${httpRequest.method()}")
    }

    // Set remote address
    ctx.channel().remoteAddress() match {
      case x: InetSocketAddress =>
        req = req.withRemoteAddress(ServerAddress(s"${x.getHostString}:${x.getPort}"))
      case _ =>
    }

    // Read request headers
    httpRequest.headers().names().asScala.foreach { name =>
      req = req.withHeader(name, httpRequest.headers().get(name))
    }

    // Determine buffering strategy based on Content-Length header
    val contentLength = HttpUtil.getContentLength(httpRequest, -1L)
    val useFile       = contentLength > bodyBufferThresholdBytes

    val state = if (useFile) {
      val tmpFile = Files.createTempFile("airframe-body-", ".tmp").toFile
      try {
        new RequestState(req, bodyBuf = null, fileBuf = new FileOutputStream(tmpFile), tmpFile = Some(tmpFile))
      } catch {
        case e: Exception =>
          tmpFile.delete()
          throw e
      }
    } else {
      new RequestState(req, bodyBuf = null, fileBuf = null, tmpFile = None)
    }

    ctx.channel().attr(REQUEST_STATE_KEY).set(state)

    // If this HttpRequest also carries content (e.g., non-chunked), process the content part
    if (httpRequest.isInstanceOf[HttpContent]) {
      handleContent(ctx, httpRequest.asInstanceOf[HttpContent])
    }
  }

  private def handleContent(ctx: ChannelHandlerContext, content: HttpContent): Unit = {
    val state = ctx.channel().attr(REQUEST_STATE_KEY).get()
    if (state == null) {
      ReferenceCountUtil.release(content)
      return
    }

    try {
      val buf = content.content()
      if (buf.isReadable) {
        val size = buf.readableBytes()

        if (state.fileBuf != null) {
          // File-backed path: write directly to file using Netty's internal pooled buffer
          buf.readBytes(state.fileBuf, size)
        } else {
          // In-memory path
          if (state.bodyBuf == null) {
            state.bodyBuf = new ByteArrayOutputStream(size)
          }
          buf.readBytes(state.bodyBuf, size)

          // Check if we should spill to file (Content-Length was unknown or chunked transfer)
          if (state.bodyBuf.size() > bodyBufferThresholdBytes) {
            val tmpFile = Files.createTempFile("airframe-body-", ".tmp").toFile
            try {
              val fos = new FileOutputStream(tmpFile)
              state.bodyBuf.writeTo(fos)
              state.fileBuf = fos
              state.tmpFile = Some(tmpFile)
              state.bodyBuf = null // Release in-memory buffer
            } catch {
              case e: Exception =>
                tmpFile.delete()
                throw e
            }
          }
        }
      }

      if (content.isInstanceOf[LastHttpContent]) {
        // Request is complete — build final HttpMessage.Request and fire downstream
        var req = state.request

        if (state.fileBuf != null) {
          state.fileBuf.close()
          state.tmpFile.foreach { tmpFile =>
            req = req.withContent(new InputStreamMessage(new FileInputStream(tmpFile)))
          }
        } else if (state.bodyBuf != null && state.bodyBuf.size() > 0) {
          req = req.withContent(state.bodyBuf.toByteArray)
        }

        // Store the temp file path for cleanup after response
        state.tmpFile.foreach { tmpFile =>
          ctx.channel().attr(TEMP_FILE_KEY).set(tmpFile)
        }

        ctx.channel().attr(REQUEST_STATE_KEY).set(null)
        ctx.fireChannelRead(req)
      }
    } finally {
      ReferenceCountUtil.release(content)
    }
  }

  override def exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable): Unit = {
    // Clean up any in-progress state
    val state = ctx.channel().attr(REQUEST_STATE_KEY).getAndSet(null)
    if (state != null) {
      if (state.fileBuf != null) {
        try { state.fileBuf.close() }
        catch { case _: Exception => }
      }
      state.tmpFile.foreach(_.delete())
    }
    ctx.fireExceptionCaught(cause)
  }
}

object NettyBodyHandler {
  private[netty] val REQUEST_STATE_KEY: AttributeKey[RequestState] =
    AttributeKey.valueOf("airframe.requestState")

  private[netty] val TEMP_FILE_KEY: AttributeKey[File] =
    AttributeKey.valueOf("airframe.tempFile")

  /**
    * Delete the temp file associated with the channel, if any.
    */
  def cleanupTempFile(ctx: ChannelHandlerContext): Unit = {
    val tmpFile = ctx.channel().attr(TEMP_FILE_KEY).getAndSet(null)
    if (tmpFile != null) {
      tmpFile.delete()
    }
  }

  private[netty] class RequestState(
      val request: wvlet.airframe.http.HttpMessage.Request,
      var bodyBuf: ByteArrayOutputStream,
      var fileBuf: FileOutputStream,
      var tmpFile: Option[File]
  )
}
