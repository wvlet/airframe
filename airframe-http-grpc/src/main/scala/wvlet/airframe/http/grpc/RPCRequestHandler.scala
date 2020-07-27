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
import io.grpc.stub.ServerCalls.UnaryMethod
import io.grpc.stub.StreamObserver
import wvlet.airframe.codec.PrimitiveCodec.ValueCodec
import wvlet.airframe.msgpack.spi.MsgPack
import wvlet.airframe.surface.MethodSurface
import wvlet.log.LogSupport

/**
  * Receives MessagePack Map value for the RPC request, and call the controller method
  */
class RPCRequestHandler[A](controller: Any, methodSurface: MethodSurface)
    extends UnaryMethod[MsgPack, A]
    with LogSupport {
  override def invoke(request: MsgPack, responseObserver: StreamObserver[A]): Unit = {
    // Build method arguments from MsgPack
    val requestValue = ValueCodec.unpack(request)
    info(requestValue)

    //methodSurface.call(controller, )
    responseObserver.onNext("hello world".asInstanceOf[A])
    responseObserver.onCompleted()
  }
}
