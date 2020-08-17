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
import io.grpc.{CallOptions, Channel, ManagedChannel, ManagedChannelBuilder}
import io.grpc.stub.{AbstractBlockingStub, ClientCallStreamObserver, ClientCalls, StreamObserver}
import wvlet.airframe.Design
import wvlet.airframe.codec.MessageCodecFactory
import wvlet.airframe.http.router.Route
import wvlet.airframe.http.{RPC, Router}
import wvlet.airframe.msgpack.spi.MsgPack
import wvlet.airframe.rx.{OnCompletion, OnError, OnNext, Rx, RxRunner}
import wvlet.airspec.AirSpec

import scala.concurrent.Await
import scala.concurrent.duration.Duration

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

    def helloStreaming(name: String): Rx[String] = {
      Rx.sequence("Hello", "Bye").map(x => s"${x} ${name}!")
    }

    def helloClientStreaming(input: Rx[String]): String = {
      input.toSeq.mkString(", ")
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
    private val helloStreamingMethodDescriptor =
      GrpcServiceBuilder.buildMethodDescriptor(getRoute("helloStreaming"), codecFactory)
    private val helloClientStreamingMethodDescriptor =
      GrpcServiceBuilder.buildMethodDescriptor(getRoute("helloClientStreaming"), codecFactory)

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
    def helloStreaming(name: String): Seq[String] = {
      val m = Map("name" -> name)
      import scala.jdk.CollectionConverters._
      val it = ClientCalls
        .blockingServerStreamingCall(
          getChannel,
          helloStreamingMethodDescriptor,
          getCallOptions,
          codec.toMsgPack(m)
        ).asScala
      it.map(_.asInstanceOf[String]).toSeq
    }
    def helloClientStreaming(input: Rx[String]): String = {
      val responseObserver = GrpcClient.newSingleResponseObserver
      val requestObserver: ClientCallStreamObserver[MsgPack] = ClientCalls
        .asyncClientStreamingCall[MsgPack, Any](
          getChannel.newCall(
            helloClientStreamingMethodDescriptor,
            getCallOptions
          ),
          responseObserver
        ).asInstanceOf[ClientCallStreamObserver[MsgPack]]

      val argCodec = codecFactory.of[String]

      // TODO: Create a helper method
      val c = RxRunner.run(input) {
        case OnNext(x) => {
          val msgPack = argCodec.toMsgPack(x.asInstanceOf[String])
          requestObserver.onNext(msgPack)
        }
        case OnError(e) => requestObserver.onError(e)
        case OnCompletion => {
          requestObserver.onCompleted()
        }
      }

      val f = responseObserver.promise.future
      Await.result(f, Duration.Inf).asInstanceOf[String]
    }
  }

  test("create a standalone gRPC server") {
    gRPC.server.withRouter(router).start { server =>
      // sanity test for launching gRPC server
    }
  }

  test(
    "create gRPC client",
    design = {
      wvlet.airframe.http.grpc.gRPC.server
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

    test("unary") {
      for (i <- 0 to 100) {
        val ret = stub.hello("world")
        ret shouldBe "Hello world!"
      }
    }

    test("n-ary") {
      for (i <- 0 to 100) {
        val ret2 = stub.hello2("world", i)
        ret2 shouldBe s"Hello world! (id:${i})"
      }
    }

    test("server streaming") {
      val streamingResults = stub.helloStreaming("RPC").toIndexedSeq
      streamingResults shouldBe Seq("Hello RPC!", "Bye RPC!")
    }

    test("client streaming") {
      val result = stub.helloClientStreaming(Rx.sequence("Apple", "Banana"))
      result shouldBe "Apple, Banana"
    }
  }
}
