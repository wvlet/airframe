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

import io.grpc.{CallOptions, Channel}
import io.grpc.stub.{AbstractBlockingStub, ClientCallStreamObserver, ClientCalls}
import wvlet.airframe.Design
import wvlet.airframe.codec.MessageCodecFactory
import wvlet.airframe.http.grpc.internal.GrpcServiceBuilder
import wvlet.airframe.http.grpc.{GrpcClientCalls, GrpcContext, GrpcEncoding, gRPC}
import wvlet.airframe.http.router.Route
import wvlet.airframe.http.{HttpHeader, RPC, Router}
import wvlet.airframe.msgpack.spi.MsgPack
import wvlet.log.LogSupport
import wvlet.airframe.rx.{Rx, RxStream}

import java.nio.charset.StandardCharsets

@RPC
trait DemoApi extends LogSupport {
  def getContext: String = {
    val ctx = GrpcContext.current
    debug(ctx)
    "Ok"
  }

  def hello(name: String): String = {
    s"Hello ${name}!"
  }

  def hello2(name: String, id: Int): String = {
    s"Hello ${name}! (id:${id})"
  }

  def helloStreaming(name: String): RxStream[String] = {
    Rx.sequence("Hello", "Bye").map(x => s"${x} ${name}!")
  }

  def helloClientStreaming(input: RxStream[String]): String = {
    input.toSeq.mkString(", ")
  }

  def helloBidiStreaming(input: RxStream[String]): RxStream[String] = {
    input.map(x => s"Hello ${x}!")
  }
}

object DemoApi {

  def design: Design = gRPC.server
    .withRouter(router)
    .withName("DemoApi")
    .designWithChannel
    .bind[DemoApiClient].toProvider { channel: Channel => new DemoApiClient(channel) }

  def router = Router.add[DemoApi]

  private def getRoute(name: String): Route = {
    router.routes.find(_.methodSurface.name == name).getOrElse {
      throw new IllegalArgumentException(s"Route is not found :${name}")
    }
  }

  case class DemoApiClient(
      channel: Channel,
      callOptions: CallOptions = CallOptions.DEFAULT,
      codecFactory: MessageCodecFactory = MessageCodecFactory.defaultFactoryForJSON,
      encoding: GrpcEncoding = GrpcEncoding.MsgPack
  ) extends AbstractBlockingStub[DemoApiClient](channel, callOptions) {
    override def build(channel: Channel, callOptions: CallOptions): DemoApiClient = {
      new DemoApiClient(channel, callOptions)
    }
    private val codec = codecFactory.of[Map[String, Any]]
    private val getContextMethodDescriptor =
      GrpcServiceBuilder.buildMethodDescriptor(getRoute("getContext"), codecFactory)
    private val helloMethodDescriptor =
      GrpcServiceBuilder.buildMethodDescriptor(getRoute("hello"), codecFactory)
    private val hello2MethodDescriptor =
      GrpcServiceBuilder.buildMethodDescriptor(getRoute("hello2"), codecFactory)
    private val helloStreamingMethodDescriptor =
      GrpcServiceBuilder.buildMethodDescriptor(getRoute("helloStreaming"), codecFactory)
    private val helloClientStreamingMethodDescriptor =
      GrpcServiceBuilder.buildMethodDescriptor(getRoute("helloClientStreaming"), codecFactory)
    private val helloBidiStreamingMethodDescriptor =
      GrpcServiceBuilder.buildMethodDescriptor(getRoute("helloBidiStreaming"), codecFactory)

    def withEncoding(encoding: GrpcEncoding): DemoApiClient = {
      this.copy(encoding = encoding)
    }

    private def encode(map: Map[String, Any]): Array[Byte] = {
      encoding.encodeWithCodec(map, codec)
    }

    def getContext: String = {
      val m = Map.empty[String, Any]
      ClientCalls
        .blockingUnaryCall(getChannel, getContextMethodDescriptor, getCallOptions, encode(m)).asInstanceOf[
          String
        ]
    }
    def hello(name: String): String = {
      val m = Map("name" -> name)
      ClientCalls
        .blockingUnaryCall(getChannel, helloMethodDescriptor, getCallOptions, encode(m)).asInstanceOf[String]
    }
    def hello2(name: String, id: Int): String = {
      val m = Map("name" -> name, "id" -> id)
      ClientCalls
        .blockingUnaryCall(getChannel, hello2MethodDescriptor, getCallOptions, encode(m)).asInstanceOf[String]
    }
    def helloStreaming(name: String): Seq[String] = {
      val m                = Map("name" -> name)
      val responseObserver = GrpcClientCalls.blockingResponseObserver[String]
      ClientCalls.asyncServerStreamingCall(
        getChannel.newCall(
          helloStreamingMethodDescriptor,
          getCallOptions
        ),
        encode(m),
        responseObserver
      )
      responseObserver.toRx.toSeq
    }
    def helloClientStreaming(input: Rx[String]): String = {
      val responseObserver = GrpcClientCalls.blockingResponseObserver[String]
      val requestObserver: ClientCallStreamObserver[MsgPack] = ClientCalls
        .asyncClientStreamingCall[MsgPack, Any](
          getChannel.newCall(
            helloClientStreamingMethodDescriptor,
            getCallOptions
          ),
          responseObserver
        ).asInstanceOf[ClientCallStreamObserver[MsgPack]]

      val c = GrpcClientCalls.readClientRequestStream(input, codecFactory.of[String], requestObserver, encoding)
      responseObserver.toRx.toSeq.head
    }

    def helloBidiStreaming(input: Rx[String]): Rx[String] = {
      val responseObserver = GrpcClientCalls.blockingResponseObserver[String]
      val requestObserver: ClientCallStreamObserver[MsgPack] = ClientCalls
        .asyncBidiStreamingCall[MsgPack, Any](
          getChannel.newCall(
            helloBidiStreamingMethodDescriptor,
            getCallOptions
          ),
          responseObserver
        ).asInstanceOf[ClientCallStreamObserver[MsgPack]]

      val c = GrpcClientCalls.readClientRequestStream(input, codecFactory.of[String], requestObserver, encoding)
      responseObserver.toRx
    }
  }

}
