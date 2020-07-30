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

import io.grpc.MethodDescriptor.MethodType
import io.grpc._
import io.grpc.stub.{AbstractBlockingStub, ClientCalls, ServerCalls, StreamObserver}
import wvlet.airframe.Design
import wvlet.airspec.AirSpec
import wvlet.log.LogSupport
import wvlet.log.io.IOUtil

object MyService extends LogSupport {
  def helloMethod: MethodDescriptor[String, String] =
    MethodDescriptor
      .newBuilder[String, String](StringMarshaller, StringMarshaller)
      .setFullMethodName(MethodDescriptor.generateFullMethodName("my-service", "hello"))
      .setType(MethodType.UNARY)
      .build()

  def helloMethodDef: ServerMethodDefinition[String, String] = {
    ServerMethodDefinition.create[String, String](
      helloMethod,
      ServerCalls.asyncUnaryCall(
        new MethodHandlers()
      )
    )
  }

  class MethodHandlers extends ServerCalls.UnaryMethod[String, String] {
    override def invoke(request: String, responseObserver: StreamObserver[String]): Unit = {
      helloImpl(request, responseObserver)
    }
  }

  def helloImpl(request: String, responseObserver: StreamObserver[String]): Unit = {
    responseObserver.onNext(s"Hello ${request}")
    responseObserver.onCompleted()
  }

  class MyServiceBlockingStub(channel: Channel, callOptions: CallOptions)
      extends AbstractBlockingStub[MyServiceBlockingStub](channel, callOptions) {
    override def build(channel: Channel, callOptions: CallOptions): MyServiceBlockingStub = {
      new MyServiceBlockingStub(channel, callOptions)
    }

    def hello(message: String): String = {
      ClientCalls.blockingUnaryCall(getChannel, MyService.helloMethod, getCallOptions, message)
    }
  }

  def newBlockingStub(channel: Channel): MyServiceBlockingStub = {
    new MyServiceBlockingStub(channel, CallOptions.DEFAULT)
  }
}

/**
  * Test for running gRPC server and client for checking the basic data flow of grpc-java
  */
object GrpcTest extends AirSpec {
  private val service: ServerServiceDefinition =
    ServerServiceDefinition
      .builder("my-service")
      .addMethod[String, String](MyService.helloMethodDef)
      .build()

  private val port = IOUtil.randomPort

  override protected def design =
    Design.newDesign
      .bind[Server].toInstance(
        ServerBuilder.forPort(port).addService(service).build()
      ).onStart { server =>
        server.start()
        info(s"Starting gRPC server localhost:${port}")
      }
      .onShutdown { server =>
        info(s"Shutting down gRPC server localhost:${port}")
        server.shutdownNow()
      }
      .bind[ManagedChannel].toProvider { server: Server =>
        ManagedChannelBuilder.forTarget(s"localhost:${server.getPort}").usePlaintext().build()
      }
      .onShutdown { channel =>
        channel.shutdownNow()
      }

  test("run server") { (server: Server, channel: ManagedChannel) =>
    val client = MyService.newBlockingStub(channel)
    for (i <- 0 to 10) {
      val ret = client.hello(s"airframe-grpc ${i}")
      debug(ret)
      ret shouldBe s"Hello airframe-grpc ${i}"
    }
  }
}
