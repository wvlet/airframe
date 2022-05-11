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

import io.grpc.CallOptions
import io.grpc.stub.ClientCalls
import wvlet.airframe.codec.MessageCodec
import wvlet.airframe.http.RPCEncoding

case class GrpcClientConfig(
    rpcEncoding: RPCEncoding = RPCEncoding.MsgPack,
    callOptions: CallOptions
)

case class GrpcMethod[Req, Resp](
    descriptor: io.grpc.MethodDescriptor[Array[Byte], Resp],
    requestCodec: MessageCodec[Req]
)

class GrpcClient(config: GrpcClientConfig) {

  def unaryCall[Req, Resp](
      channel: io.grpc.Channel,
      method: GrpcMethod[Req, Resp],
      request: Req
  ): Resp = {

    val requestBody: Array[Byte] = config.rpcEncoding.encodeWithCodec(request, method.requestCodec)
    ClientCalls
      .blockingUnaryCall[Array[Byte], Resp](channel, method.descriptor, config.callOptions, requestBody)
      .asInstanceOf[Resp]
  }
}
