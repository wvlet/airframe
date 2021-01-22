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
package wvlet.airframe.http.grpc.internal

import io.grpc.ForwardingServerCall.SimpleForwardingServerCall
import io.grpc.{Contexts, Metadata, ServerCall, ServerCallHandler, ServerInterceptor}
import wvlet.airframe.http.grpc.{GrpcContext, GrpcEncoding}

/**
  * An interceptor for setting the response headers based on the encoding used for RPC
  */
private[grpc] object GrpcResponseHeaderInterceptor extends ServerInterceptor {
  override def interceptCall[ReqT, RespT](
      call: ServerCall[ReqT, RespT],
      requestHeaders: Metadata,
      next: ServerCallHandler[ReqT, RespT]
  ): ServerCall.Listener[ReqT] = {
    next.startCall(
      new SimpleForwardingServerCall[ReqT, RespT](call) {
        override def sendHeaders(responseHeaders: Metadata): Unit = {
          import GrpcContext._
          requestHeaders.accept match {
            case GrpcEncoding.ContentTypeJson =>
              responseHeaders.setAccept(GrpcEncoding.ContentTypeJson)
            case _ =>
              responseHeaders.setAccept(GrpcEncoding.ContentTypeMsgPack)
          }
          super.sendHeaders(responseHeaders);
        }
      },
      requestHeaders
    );
  }
}
