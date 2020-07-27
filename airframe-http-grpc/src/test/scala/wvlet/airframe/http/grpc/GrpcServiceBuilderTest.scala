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
import io.grpc.stub.{AbstractBlockingStub, ClientCalls}
import wvlet.airframe.Design
import wvlet.airframe.codec.MessageCodecFactory
import wvlet.airframe.http.router.Route
import wvlet.airframe.http.{RPC, Router}
import wvlet.airspec.AirSpec

/**
  */
object GrpcServiceBuilderTest extends AirSpec {

  @RPC
  trait MyApi {
    def hello(name: String): String = {
      s"Hello ${name}!"
    }

    def hello2(name: String, id: Int): String = {
      s"Hello ${name}! (id:${id})"
    }
  }

  private val router = Router.add[MyApi]
  debug(router)

  private def getRoute(name: String): Route = {
    router.routes.find(_.methodSurface.name == name).getOrElse {
      throw new IllegalArgumentException(s"Route is not found :${name}")
    }
  }

  // TODO: Generate this stub using sbt-airframe
  class MyApiStub(
      channel: Channel,
      callOptions: CallOptions = CallOptions.DEFAULT,
      codecFactory: MessageCodecFactory = MessageCodecFactory.defaultFactoryForJSON
  ) extends AbstractBlockingStub[MyApiStub](channel, callOptions) {
    override def build(channel: Channel, callOptions: CallOptions): MyApiStub = {
      new MyApiStub(channel, callOptions)
    }
    private val codec = codecFactory.of[Map[String, Any]]
    private val helloMethodDescriptor =
      GrpcServiceBuilder.buildMethodDescriptor(getRoute("hello"), codecFactory)
    private val hello2MethodDescriptor =
      GrpcServiceBuilder.buildMethodDescriptor(getRoute("hello2"), codecFactory)

    def hello(name: String): String = {
      val m = Map("name" -> name)
      ClientCalls
        .blockingUnaryCall(getChannel, helloMethodDescriptor, getCallOptions, codec.toMsgPack(m)).asInstanceOf[String]
    }
    def hello2(name: String, id: Int): String = {
      val m = Map("name" -> name, "id" -> id)
      ClientCalls
        .blockingUnaryCall(getChannel, hello2MethodDescriptor, getCallOptions, codec.toMsgPack(m)).asInstanceOf[String]
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
    for (i <- 0 to 100) {
      val ret = stub.hello("world")
      ret shouldBe "Hello world!"

      val ret2 = stub.hello2("world", i)
      ret2 shouldBe s"Hello world! (id:${i})"
    }
  }
}
