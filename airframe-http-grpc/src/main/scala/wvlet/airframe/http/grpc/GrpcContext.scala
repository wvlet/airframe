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
import wvlet.log.LogSupport

object GrpcContext {
  private val contextKey = Context.key[GrpcContext]("grpc_context")

  /**
    * Get the current GrpcContext. If it returns None, it means this method is called outside gRPC's local thread for processing the request
    * @return
    */
  def current: Option[GrpcContext] = Option(contextKey.get())

  private[grpc] object ContextTrackInterceptor extends ServerInterceptor with LogSupport {
    override def interceptCall[ReqT, RespT](
        call: ServerCall[ReqT, RespT],
        headers: Metadata,
        next: ServerCallHandler[ReqT, RespT]
    ): ServerCall.Listener[ReqT] = {
      // Create a new context that conveys GrpcContext object.
      val newContext = Context
        .current().withValue(
          contextKey,
          GrpcContext(Option(call.getAuthority), call.getAttributes, headers, call.getMethodDescriptor)
        )
      Contexts.interceptCall(newContext, call, headers, next)
    }
  }

}

case class GrpcContext(
    authority: Option[String],
    attributes: Attributes,
    metadata: Metadata,
    descriptor: MethodDescriptor[_, _]
)
