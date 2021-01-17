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

  def current: GrpcContext = contextKey.get()

  private[grpc] object ContextTrackInterceptor extends ServerInterceptor with LogSupport {
    override def interceptCall[ReqT, RespT](call: ServerCall[ReqT, RespT], headers: Metadata, next: ServerCallHandler[ReqT, RespT]): ServerCall.Listener[ReqT] = {
      val context = Context.current().withValue(contextKey, GrpcContext(headers))
      val listenerWithContext = Contexts.interceptCall(context, call , headers, next)
      val listener = new ForwardingServerCallListener.SimpleForwardingServerCallListener[ReqT](listenerWithContext){}
      listener
    }
  }
}

case class GrpcContext(metadata:Metadata)

