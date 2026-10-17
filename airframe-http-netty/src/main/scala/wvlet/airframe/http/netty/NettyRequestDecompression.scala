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
import io.netty.channel.embedded.EmbeddedChannel
import io.netty.channel.{ChannelFutureListener, ChannelHandlerContext, ChannelInboundHandlerAdapter}
import io.netty.handler.codec.compression.DecompressionException
import io.netty.handler.codec.http.*
import io.netty.util.ReferenceCountUtil
import wvlet.log.LogSupport

import java.nio.charset.StandardCharsets

/**
  * Decodes request bodies sent with `Content-Encoding: gzip` or `deflate` and bounds the decoded size of such requests.
  *
  * Netty's [[HttpContentDecompressor]] pushes decoded chunks downstream as they are inflated, and its `maxAllocation`
  * option only caps a single decompression buffer. So the cumulative bound is enforced by a separate handler placed
  * right after the decompressor. Uncompressed requests are not counted, so this does not impose a new global body
  * limit.
  *
  * A new instance must be created per channel, and both [[decompressor]] and [[limiter]] must be added to the pipeline
  * in this order, before HttpObjectAggregator.
  */
private[netty] class NettyRequestDecompression(maxDecompressedRequestSize: Int) {
  require(maxDecompressedRequestSize > 0, "maxDecompressedRequestSize must be positive")

  // Whether the current request body is being decoded. Set by the decompressor before it forwards the request headers
  private var decoding: Boolean = false

  /**
    * Decoder limited to gzip and deflate. Other encodings (e.g., br, zstd, snappy) are passed through as raw bytes so
    * that the behavior does not depend on optional native libraries in the classpath.
    */
  val decompressor: HttpContentDecompressor = new HttpContentDecompressor(false, 0) {
    override protected def newContentDecoder(contentEncoding: String): EmbeddedChannel = {
      val decoder =
        if (NettyRequestDecompression.isSupportedEncoding(contentEncoding)) {
          super.newContentDecoder(contentEncoding)
        } else {
          null
        }
      decoding = decoder != null
      decoder
    }
  }

  val limiter: ChannelInboundHandlerAdapter = new ChannelInboundHandlerAdapter with LogSupport {
    private var decodedBytes: Long = 0L
    private var rejected: Boolean  = false

    override def channelRead(ctx: ChannelHandlerContext, msg: Any): Unit = {
      if (rejected) {
        // The connection is being closed. Drop the rest of the rejected request
        ReferenceCountUtil.release(msg)
      } else {
        if (msg.isInstanceOf[HttpMessage]) {
          decodedBytes = 0L
        }
        msg match {
          case c: HttpContent if decoding =>
            decodedBytes += c.content().readableBytes()
            if (decodedBytes > maxDecompressedRequestSize) {
              ReferenceCountUtil.release(msg)
              reject(
                ctx,
                HttpResponseStatus.REQUEST_ENTITY_TOO_LARGE,
                s"Decompressed request body exceeds ${maxDecompressedRequestSize} bytes"
              )
            } else {
              ctx.fireChannelRead(msg)
            }
          case _ =>
            ctx.fireChannelRead(msg)
        }
      }
    }

    override def exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable): Unit = {
      if (NettyRequestDecompression.isDecompressionFailure(cause)) {
        if (!rejected) {
          reject(ctx, HttpResponseStatus.BAD_REQUEST, "Malformed compressed request body")
        }
      } else {
        ctx.fireExceptionCaught(cause)
      }
    }

    private def reject(ctx: ChannelHandlerContext, status: HttpResponseStatus, message: String): Unit = {
      rejected = true
      // Stop reading so that the decompressor does not keep inflating the rest of the rejected body
      ctx.channel().config().setAutoRead(false)
      debug(s"Rejecting a request from ${ctx.channel().remoteAddress()}: ${status} ${message}")
      val resp = new DefaultFullHttpResponse(
        HttpVersion.HTTP_1_1,
        status,
        Unpooled.copiedBuffer(message, StandardCharsets.UTF_8)
      )
      resp.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain; charset=utf-8")
      resp.headers().setInt(HttpHeaderNames.CONTENT_LENGTH, resp.content().readableBytes())
      resp.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE)
      ctx.writeAndFlush(resp).addListener(ChannelFutureListener.CLOSE)
    }
  }
}

private[netty] object NettyRequestDecompression {
  private def isSupportedEncoding(contentEncoding: String): Boolean = {
    contentEncoding != null && {
      val e = contentEncoding.trim
      HttpHeaderValues.GZIP.contentEqualsIgnoreCase(e) ||
      HttpHeaderValues.X_GZIP.contentEqualsIgnoreCase(e) ||
      HttpHeaderValues.DEFLATE.contentEqualsIgnoreCase(e) ||
      HttpHeaderValues.X_DEFLATE.contentEqualsIgnoreCase(e)
    }
  }

  private def isDecompressionFailure(cause: Throwable): Boolean = {
    Iterator.iterate(cause)(_.getCause).takeWhile(_ != null).take(10).exists(_.isInstanceOf[DecompressionException])
  }
}
