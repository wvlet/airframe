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

import wvlet.airframe.http.{RPCEncoding, Router}
import wvlet.airframe.http.grpc.example.DemoApi
import wvlet.airframe.http.grpc.example.DemoApi.DemoApiClient
import wvlet.airspec.AirSpec
import wvlet.airframe.rx._

/**
  */
class GrpcJsonTest extends AirSpec {

  private def router = Router.add[DemoApi]

  protected override def design = gRPC.server.withRouter(router).designWithChannel

  test("json encoding") { c: DemoApiClient =>
    val client = c.withEncoding(RPCEncoding.JSON)

    test("unary") {
      val ret = client.hello("gRPC with JSON")
      ret shouldBe "Hello gRPC with JSON!"
    }

    test("client-streaming") {
      val ret = client.helloClientStreaming(Rx.sequence("A", "B"))
      ret shouldBe "A, B"
    }
  }
}
