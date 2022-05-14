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

import io.grpc.{CallOptions, Status, StatusException, StatusRuntimeException}
import io.grpc.stub.ClientCalls
import wvlet.airframe.codec.MessageCodec
import wvlet.airframe.http.grpc.internal.GrpcException
import wvlet.airframe.http.{RPCEncoding, RPCException, RPCStatus}

case class GrpcClientConfig(
    rpcEncoding: RPCEncoding = RPCEncoding.MsgPack,
    callOptions: CallOptions
)

case class GrpcMethod[Req, Resp](
    /**
      * Grpc method descriptor, which contains a response encoder of MessageCodec[Resp]
      */
    descriptor: io.grpc.MethodDescriptor[Array[Byte], Resp],
    requestCodec: MessageCodec[Req]
)

class GrpcClient(config: GrpcClientConfig) {

  private def prepareRPCRequestBody[Req, Resp](method: GrpcMethod[Req, Resp], request: Req): Array[Byte] = {
    try {
      config.rpcEncoding.encodeWithCodec(request, method.requestCodec)
    } catch {
      case e: Throwable =>
        throw RPCStatus.INVALID_ARGUMENT_U2.newException(
          message = s"Failed to encode the RPC request argument: ${request}: ${e.getMessage}",
          cause = e
        )
    }
  }

  private def translateToRPCException(ex: StatusRuntimeException): RPCException = {
    try {
      val trailers     = Status.trailersFromThrowable(ex)
      val rpcErrorJson = trailers.get[String](GrpcException.rpcErrorBodyKey)
      RPCException.fromJson(rpcErrorJson)
    } catch {
      case e: Throwable =>
        RPCStatus.DATA_LOSS_I8.newException(s"Failed to parse the RPC error details: ${ex.getMessage}")
    }
  }

  def unaryCall[Req, Resp](
      channel: io.grpc.Channel,
      method: GrpcMethod[Req, Resp],
      request: Req
  ): Resp = {
    val requestBody: Array[Byte] = prepareRPCRequestBody(method, request)
    try {
      ClientCalls
        .blockingUnaryCall[Array[Byte], Resp](channel, method.descriptor, config.callOptions, requestBody)
        .asInstanceOf[Resp]
    } catch {
      case e: StatusRuntimeException =>
        throw translateToRPCException(e)
    }
  }

}
