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

import io.grpc.stub.{AbstractBlockingStub, AbstractStub, ClientCalls}
import io.grpc.{
  CallOptions,
  Channel,
  ManagedChannel,
  ManagedChannelBuilder,
  Metadata,
  MethodDescriptor,
  Server,
  ServerBuilder,
  ServerCall,
  ServerCallHandler,
  ServerMethodDefinition,
  ServerServiceDefinition
}
import wvlet.airframe.Design
import wvlet.airspec.AirSpec
import wvlet.log.io.IOUtil

object MyService {
  def helloMethod: MethodDescriptor[String, String] = ???

  def helloMethodDef: ServerMethodDefinition[String, String] = {
    ServerMethodDefinition.create[String, String](
      helloMethod,
      new HelloCallHandler()
    )
  }

  class HelloCallHandler extends ServerCallHandler[String, String] {
    override def startCall(call: ServerCall[String, String], headers: Metadata): ServerCall.Listener[String] = ???
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
}

/**
  */

object GrpcTest extends AirSpec {
  private val service: ServerServiceDefinition =
    ServerServiceDefinition
      .builder("my-service")
      .addMethod[String, String](MyService.helloMethodDef)
      .build()

  private val port = IOUtil.randomPort

  override def design =
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

  test("run server") { server: Server => }
}
