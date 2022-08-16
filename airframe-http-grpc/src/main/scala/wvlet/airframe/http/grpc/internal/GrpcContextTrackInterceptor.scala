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

import io.grpc._
import wvlet.airframe.http.grpc.GrpcContext
import wvlet.log.LogSupport

/**
  * A server request interceptor to set GrpcContext to the thread-local storage
  */
private[grpc] object ContextTrackInterceptor extends ServerInterceptor with LogSupport {
  override def interceptCall[ReqT, RespT](
      call: ServerCall[ReqT, RespT],
      headers: Metadata,
      next: ServerCallHandler[ReqT, RespT]
  ): ServerCall.Listener[ReqT] = {
    // Create a new context that conveys GrpcContext object.
    val rpcContext = GrpcContext(Option(call.getAuthority), call.getAttributes, headers, call.getMethodDescriptor)
    // Tell airframe-http about the thread-local RPC context
    wvlet.airframe.http.Compat.attachRPCContext(rpcContext)
    val newContext = Context
      .current().withValue(
        GrpcContext.contextKey,
        rpcContext
      )
    Contexts.interceptCall(newContext, call, headers, next)
  }
}
