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
import io.grpc.stub.{AbstractBlockingStub, ClientCalls}
import wvlet.airframe.Design
import wvlet.airframe.codec.MessageCodecFactory
import wvlet.airframe.http.grpc.{GrpcContext, GrpcServiceBuilder, gRPC}
import wvlet.airframe.http.router.Route
import wvlet.airframe.http.{RPC, Router}
import wvlet.log.LogSupport

@RPC
trait DemoApi extends LogSupport {
  def getContext: String = {
    val ctx = GrpcContext.current
    info(ctx)
    "Ok"
  }
}

object DemoApi {

  def design: Design = gRPC
          .server
          .withRouter(router)
          .withName("DemoApi")
          .designWithChannel
          .bind[DemoApiClient].toProvider{ channel: Channel => new DemoApiClient(channel) }

  def router = Router.add[DemoApi]

  private def getRoute(name: String): Route = {
    router.routes.find(_.methodSurface.name == name).getOrElse {
      throw new IllegalArgumentException(s"Route is not found :${name}")
    }
  }

  class DemoApiClient(
          channel: Channel,
          callOptions: CallOptions = CallOptions.DEFAULT,
          codecFactory: MessageCodecFactory = MessageCodecFactory.defaultFactoryForJSON
  ) extends AbstractBlockingStub[DemoApiClient](channel, callOptions) {
    override def build(channel: Channel, callOptions: CallOptions): DemoApiClient = {
      new DemoApiClient(channel, callOptions)
    }
    private val codec = codecFactory.of[Map[String, Any]]
    private val getContextMethodDescriptor =
      GrpcServiceBuilder.buildMethodDescriptor(getRoute("getContext"), codecFactory)

    def getContext: String = {
      val m = Map.empty[String, Any]
      ClientCalls
              .blockingUnaryCall(getChannel, getContextMethodDescriptor, getCallOptions, codec.toMsgPack(m)).asInstanceOf[String]
    }
  }

}
