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
import io.grpc.MethodDescriptor.{Marshaller, MethodType}
import io.grpc.stub.{AbstractBlockingStub, ClientCalls}
import io.grpc.{CallOptions, Channel, ManagedChannel, ManagedChannelBuilder, MethodDescriptor, Server}
import wvlet.airframe.Design
import wvlet.airframe.codec.CollectionCodec.MapCodec
import wvlet.airframe.codec.MessageCodec
import wvlet.airframe.codec.PrimitiveCodec.StringCodec
import wvlet.airframe.http.grpc.GrpcServiceBuilder.{RPCRequestMarshaller, RPCResponseMarshaller}
import wvlet.airframe.http.{RPC, Router}
import wvlet.airframe.msgpack.spi.MsgPack
import wvlet.airspec.AirSpec

/**
  */
object GrpcServiceBuilderTest extends AirSpec {

  @RPC
  trait MyApi {
    def hello(name: String): String = {
      s"Hello ${name}!"
    }
  }

  // TODO: Generate this stub using sbt-airframe
  class MyApiStub(channel: Channel, callOptions: CallOptions = CallOptions.DEFAULT)
      extends AbstractBlockingStub[MyApiStub](channel, callOptions) {
    override def build(channel: Channel, callOptions: CallOptions): MyApiStub = {
      new MyApiStub(channel, callOptions)
    }

    private val helloMethodDescriptor =
      MethodDescriptor
        .newBuilder[MsgPack, String](RPCRequestMarshaller, new RPCResponseMarshaller(StringCodec))
        .setFullMethodName("wvlet.airframe.http.grpc.GrpcServiceBuilderTest.MyApi/hello")
        .setType(MethodType.UNARY)
        .build()

    def hello(name: String): String = {
      val m     = Map("name" -> name)
      val codec = MessageCodec.of[Map[String, String]]
      ClientCalls.blockingUnaryCall(getChannel, helloMethodDescriptor, getCallOptions, codec.toMsgPack(m))
    }
  }

  private val router = Router.add[MyApi]
  info(router)

  test("generate a service definition from Router") {
    Design.newDesign.withSession { session =>
      val services = GrpcServiceBuilder.buildService(router, session)
      info(services.map(_.getServiceDescriptor).mkString("\n"))
    }
  }

  test(
    "create gRPC client",
    design = {
      Grpc.server
        .withRouter(router)
        .design
        .bind[ManagedChannel].toProvider { server: GrpcServer =>
          ManagedChannelBuilder.forTarget(server.localAddress).usePlaintext().build()
        }
        .onShutdown { channel =>
          channel.shutdownNow()
        }
    }
  ) { (server: GrpcServer, channel: ManagedChannel) =>
    val stub = new MyApiStub(channel)
    val ret  = stub.hello("world")
    info(ret)
  }
}
