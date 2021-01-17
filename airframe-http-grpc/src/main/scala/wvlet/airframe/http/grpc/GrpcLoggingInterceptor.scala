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
package wvlet.airframe.http.grpc
import io.grpc._
import wvlet.airframe.codec.PrimitiveCodec.ValueCodec
import wvlet.airframe.msgpack.spi.MsgPack
import wvlet.log.LogSupport

/**
  * TODO: An example implementation of gRPC logging.
  * We should write logs to files HttpAccessLogFilter
  */
class GrpcLoggingInterceptor extends ServerInterceptor with LogSupport {
  override def interceptCall[ReqT, RespT](
      call: ServerCall[ReqT, RespT],
      headers: Metadata,
      next: ServerCallHandler[ReqT, RespT]
  ): ServerCall.Listener[ReqT] = {
    debug(s"method: ${call.getMethodDescriptor}")
    debug(s"headers: ${headers}")
    debug(s"attributes: ${call.getAttributes}")

    val ctx                 = Context.current
    val listenerWithContext = Contexts.interceptCall(ctx, call, headers, next)
    val tracingListenerWithContext =
      new ForwardingServerCallListener.SimpleForwardingServerCallListener[ReqT](listenerWithContext) {
        override def onMessage(message: ReqT): Unit = {
          val v = ValueCodec.fromMsgPack(message.asInstanceOf[MsgPack])
          logger.debug(s"request: ${v}")
          delegate().onMessage(message)
        }
      }

    tracingListenerWithContext
  }
}
