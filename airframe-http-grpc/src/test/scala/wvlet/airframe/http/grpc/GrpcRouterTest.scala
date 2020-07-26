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
import wvlet.airframe.Design
import wvlet.airframe.http.{RPC, Router}
import wvlet.airspec.AirSpec

/**
  */
object GrpcRouterTest extends AirSpec {

  @RPC
  trait MyApi {
    def hello(name: String): String = {
      s"Hello ${name}!"
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

  test("create gRPC client") {
    val d = Grpc.server
      .withRouter(router)
      .design
      .noLifeCycleLogging

    d.build[GrpcServer] { server => }
  }
}
