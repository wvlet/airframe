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
package wvlet.airframe.http.grpc.example

import io.grpc.stub.{AbstractBlockingStub, StreamObserver}
import io.grpc.{CallOptions, Channel}
import wvlet.airframe.Design
import wvlet.airframe.codec.MessageCodecFactory
import wvlet.airframe.http.grpc.internal.GrpcServiceBuilder
import wvlet.airframe.http.grpc.{GrpcClient, GrpcClientConfig, GrpcClientInterceptor, gRPC}
import wvlet.airframe.http.router.Route
import wvlet.airframe.http.{RPC, RPCEncoding, RPCStatus, Router}
import wvlet.airframe.rx.{Rx, RxStream}
import wvlet.airframe.surface.Surface

/**
  * Demo gRPC API used for GrpcClientTest
  */
@RPC
trait DemoApiV2 {
  def hello(name: String): String = {
    s"Hello ${name}!"
  }

  def serverStreaming(name: String): Rx[String] = {
    Rx.sequence(s"${name}:0", s"${name}:1")
  }

  def errorTest(name: String): String = {
    throw RPCStatus.INVALID_ARGUMENT_U2.newException(s"Hello error: ${name}")
  }
}

object DemoApiV2 {
  private val router = Router.add[DemoApiV2]

  def design: Design = gRPC.server
    .withRouter(router)
    .withName("DemoApi2")
    .designWithChannel
    .bind[SyncClient].toProvider { (channel: Channel) => new SyncClient(channel) }

  private def getRoute(name: String): Route = {
    router.routes.find(_.methodSurface.name == name).getOrElse {
      throw new IllegalArgumentException(s"Route is not found :${name}")
    }
  }

  class SyncClient(
      channel: Channel,
      callOptions: CallOptions = CallOptions.DEFAULT,
      codecFactory: MessageCodecFactory = MessageCodecFactory.defaultFactoryForJSON,
      rpcEncoding: RPCEncoding = RPCEncoding.MsgPack
  ) extends AbstractBlockingStub[SyncClient](channel, callOptions) {
    override def build(channel: Channel, callOptions: CallOptions): SyncClient = {
      new SyncClient(channel, callOptions, codecFactory, rpcEncoding)
    }

    private val client        = new GrpcClient(GrpcClientConfig(rpcEncoding = rpcEncoding, callOptions = callOptions))
    private lazy val _channel = GrpcClientInterceptor.wrap(getChannel, rpcEncoding)
    private val helloMethod =
      GrpcServiceBuilder.buildGrpcMethod[Map[String, Any], String](
        getRoute("hello"),
        Surface.of[Map[String, Any]],
        codecFactory
      )
    private val serverStreamingMethod =
      GrpcServiceBuilder.buildGrpcMethod[Map[String, Any], String](
        getRoute("serverStreaming"),
        Surface.of[Map[String, Any]],
        codecFactory
      )

    private val errorTestMethod =
      GrpcServiceBuilder.buildGrpcMethod[Map[String, Any], String](
        getRoute("errorTest"),
        Surface.of[Map[String, Any]],
        codecFactory
      )

    def hello(name: String): String = {
      client.unaryCall(_channel, helloMethod, Map("name" -> name))
    }

    def helloAsync(name: String, observer: StreamObserver[String]): Unit = {
      client.asyncUnaryCall(_channel, helloMethod, Map("name" -> name), observer)
    }

    def serverStreaming(name: String): RxStream[String] = {
      client.serverStreamingCall(_channel, serverStreamingMethod, Map("name" -> name))
    }
    def errorTest(name: String): String = {
      client.unaryCall(_channel, errorTestMethod, Map("name" -> name))
    }

    def errorTestAsync(name: String, observer: StreamObserver[String]): Unit = {
      client.asyncUnaryCall(_channel, errorTestMethod, Map("name" -> name), observer)
    }
  }

}
