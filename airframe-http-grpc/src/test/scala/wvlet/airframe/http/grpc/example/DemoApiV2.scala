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
import wvlet.airframe.http.grpc.example.DemoApiV2.{DemoMessage, DemoResponse}
import wvlet.airframe.http.grpc.internal.GrpcServiceBuilder
import wvlet.airframe.http.grpc.{GrpcClient, GrpcClientConfig, GrpcClientInterceptor, gRPC}
import wvlet.airframe.http.router.Route
import wvlet.airframe.http.{RPC, RPCEncoding, RPCStatus, Router}
import wvlet.airframe.rx.{Rx, RxStream}
import wvlet.airframe.surface.Surface
import wvlet.log.LogSupport

/**
  * Demo gRPC API used for GrpcClientTest
  */
@RPC
trait DemoApiV2 extends LogSupport {
  def hello(name: String): String = {
    name match {
      case "XXX" =>
        throw RPCStatus.INVALID_ARGUMENT_U2.newException(s"Hello error: ${name}")
      case _ =>
        s"Hello ${name}!"
    }
  }

  def serverStreaming(name: String): Rx[String] = {
    name match {
      case "XXX" =>
        throw RPCStatus.INVALID_ARGUMENT_U2.newException(s"invalid name: ${name}")
      case _ =>
        Rx.sequence(s"${name}:0", s"${name}:1")
    }
  }

  def clientStreaming(input: RxStream[DemoMessage]): String = {
    val x = input.toSeq
    x.map(_.name).mkString(", ")
  }

  def bidiStreaming(input: RxStream[DemoMessage]): RxStream[DemoResponse] = {
    input.map(x => DemoResponse(s"Hello ${x.name}"))
  }
}

object DemoApiV2 {
  case class DemoMessage(name: String)
  case class DemoResponse(name: String)

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

    private lazy val _channel = GrpcClientInterceptor.wrap(getChannel, rpcEncoding)
    private val client =
      new GrpcClient(_channel, GrpcClientConfig(rpcEncoding = rpcEncoding, callOptions = callOptions))
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

    private val clientStreamingMethod =
      GrpcServiceBuilder.buildGrpcMethod[DemoMessage, String](
        getRoute("clientStreaming"),
        Surface.of[DemoMessage],
        codecFactory
      )

    private val bidiStreamingMethod =
      GrpcServiceBuilder.buildGrpcMethod[DemoMessage, DemoResponse](
        getRoute("bidiStreaming"),
        Surface.of[DemoMessage],
        codecFactory
      )

    def hello(name: String): String = {
      client.unaryCall(helloMethod, Map("name" -> name))
    }

    def helloAsync(name: String, observer: StreamObserver[String]): Unit = {
      client.asyncUnaryCall(helloMethod, Map("name" -> name), observer)
    }

    def serverStreaming(name: String): RxStream[String] = {
      client.serverStreamingCall(serverStreamingMethod, Map("name" -> name))
    }

    def serverStreamingAsync(name: String, observer: StreamObserver[String]): Unit = {
      client.asyncServerStreamingCall(serverStreamingMethod, Map("name" -> name), observer)
    }

    def clientStreaming(input: RxStream[DemoMessage]): String = {
      client.clientStreamingCall(clientStreamingMethod, input)
    }

    def asyncClientStreaming(observer: StreamObserver[String]): StreamObserver[DemoMessage] = {
      client.asyncClientStreamingCall(clientStreamingMethod, observer)
    }

    def bidiStreaming(input: RxStream[DemoMessage]): RxStream[DemoResponse] = {
      client.bidiStreamingCall(bidiStreamingMethod, input)
    }

    def asyncBidiStreaming(observer: StreamObserver[DemoResponse]): StreamObserver[DemoMessage] = {
      client.asyncBidiStreamingCall(bidiStreamingMethod, observer)
    }

  }

}
