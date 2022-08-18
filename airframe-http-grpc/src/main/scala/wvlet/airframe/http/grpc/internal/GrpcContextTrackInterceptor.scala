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
import wvlet.airframe.http.{Compat, RPCContext}
import wvlet.airframe.http.grpc.GrpcContext
import wvlet.log.LogSupport

/**
  * A server request interceptor to set GrpcContext to the thread-local storage
  */
private[grpc] object GrpcContextTrackInterceptor extends ServerInterceptor with LogSupport {
  override def interceptCall[ReqT, RespT](
      call: ServerCall[ReqT, RespT],
      headers: Metadata,
      next: ServerCallHandler[ReqT, RespT]
  ): ServerCall.Listener[ReqT] = {

    // Wrap the current context
    val rpcContext = GrpcContext(Option(call.getAuthority), call.getAttributes, headers, call.getMethodDescriptor)

    // Create a new context that conveys GrpcContext object.
    val newContext = Context
      .current().withValue(
        GrpcContext.contextKey,
        rpcContext
      )

    val previous    = newContext.attach()
    val prevContext = Compat.attachRPCContext(rpcContext)
    try {
      new WrappedServerCallListener[(RPCContext, Context), ReqT](
        onInit = {
          (Compat.attachRPCContext(rpcContext), newContext.attach())
        },
        onDetach = { case (previousRpcContext: RPCContext, ctx: Context) =>
          Compat.detachRPCContext(previousRpcContext)
          newContext.detach(ctx)
        },
        next.startCall(call, headers)
      )
    } finally {
      Compat.detachRPCContext(prevContext)
      newContext.detach(previous)
    }
  }
}

/**
  */
private[grpc] class WrappedServerCallListener[A, ReqT](
    onInit: => A,
    onDetach: A => Unit,
    delegate: ServerCall.Listener[ReqT]
) extends ForwardingServerCallListener.SimpleForwardingServerCallListener[ReqT](delegate) {
  private def wrap(body: => Unit): Unit = {
    val previous = onInit
    try {
      body
    } finally {
      onDetach(previous)
    }
  }

  override def onMessage(message: ReqT): Unit = {
    wrap {
      delegate.onMessage(message)
    }
  }

  override def onHalfClose(): Unit = {
    wrap {
      delegate.onHalfClose()
    }
  }

  override def onComplete(): Unit = {
    wrap {
      delegate.onComplete()
    }
  }

  override def onReady(): Unit = {
    wrap {
      delegate.onReady()
    }
  }
}
