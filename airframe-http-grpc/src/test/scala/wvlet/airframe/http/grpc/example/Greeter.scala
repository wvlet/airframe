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
import io.grpc.stub.{AbstractBlockingStub, ClientCalls}
import io.grpc.{CallOptions, Channel}
import wvlet.airframe.codec.MessageCodecFactory
import wvlet.airframe.http.grpc.GrpcServiceBuilder
import wvlet.airframe.http.router.Route
import wvlet.airframe.http.{RPC, Router}

/**
  */
@RPC
trait Greeter {
  def hello(name: String): String = s"Hello ${name}!"
}

object Greeter {

  def router = Router.add[Greeter]

  // TODO: Generate this stub using sbt-airframe
  class GreeterStub(
      channel: Channel,
      callOptions: CallOptions = CallOptions.DEFAULT,
      codecFactory: MessageCodecFactory = MessageCodecFactory.defaultFactoryForJSON
  ) extends AbstractBlockingStub[GreeterStub](channel, callOptions) {
    override def build(channel: Channel, callOptions: CallOptions): GreeterStub = {
      new GreeterStub(channel, callOptions)
    }
    private val codec = codecFactory.of[Map[String, Any]]
    private val helloMethodDescriptor =
      GrpcServiceBuilder.buildMethodDescriptor(getRoute("hello"), codecFactory)

    def hello(name: String): String = {
      val m = Map("name" -> name)
      ClientCalls
        .blockingUnaryCall(getChannel, helloMethodDescriptor, getCallOptions, codec.toMsgPack(m)).asInstanceOf[String]
    }
  }

  private def getRoute(name: String): Route = {
    router.routes.find(_.methodSurface.name == name).getOrElse {
      throw new IllegalArgumentException(s"Route is not found :${name}")
    }
  }
}
