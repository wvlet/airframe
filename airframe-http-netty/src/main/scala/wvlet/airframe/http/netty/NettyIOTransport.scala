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

import io.netty.channel.{Channel, ChannelHandlerContext, ChannelInboundHandlerAdapter}

import java.util.concurrent.atomic.AtomicInteger

class NettyIOTransport(ch: Channel) extends ChannelInboundHandlerAdapter {

  private object ReadManager {
    // which hasn't been processed by the application yet, so rather than read
    // off the channel we drain the offer queue. This also applies back-pressure
    // at the tcp level. In order to proactively detect socket close events we
    // buffer up to 1 message because the TCP implementation only notifies the
    // channel of a close event when attempting to perform operations.
    private[this] val msgsNeeded = new AtomicInteger(0)

    // exposed for testing
    // private[netty] def getMsgsNeeded = msgsNeeded.get

    // Tell the channel that we want to read if we don't have offers already queued
    def readIfNeeded(): Unit = {
      if (!ch.config.isAutoRead) {
        if (msgsNeeded.get >= 0) ch.read()
      }
    }

    // Increment our needed messages by 1 and ask the channel to read if necessary
    def incrementAndReadIfNeeded(): Unit = {
      val value = msgsNeeded.incrementAndGet()
      if (value >= 0 && !ch.config.isAutoRead) {
        ch.read()
      }
    }

    // Called when we have received a message from the channel pipeline
    def decrement(): Unit = {
      msgsNeeded.decrementAndGet()
    }
  }

  override def channelActive(ctx: ChannelHandlerContext): Unit = {
    ReadManager.readIfNeeded()
    super.channelActive(ctx)
  }

  override def channelReadComplete(ctx: ChannelHandlerContext): Unit = {
    // Check to see if we need more data
    ReadManager.readIfNeeded()
    super.channelReadComplete(ctx)
  }

  override def channelRead(ctx: ChannelHandlerContext, msg: Any): Unit = {
    ReadManager.decrement()

    // if (!readQueue.offer(msg)) // Dropped messages are fatal
//      fail(Failure(s"offer failure on $this $readQueue"))
  }

//  override def channelInactive(ctx: ChannelHandlerContext): Unit = {
////    fail(new ChannelClosedException(context.remoteAddress))
//  }
//
//  override def exceptionCaught(ctx: ChannelHandlerContext, e: Throwable): Unit = {
//    // fail(ChannelException(e, context.remoteAddress))
//  }
}
