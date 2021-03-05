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

import wvlet.airframe.http.Router
import wvlet.airframe.http.grpc.example.DemoApi
import wvlet.airframe.http.grpc.example.DemoApi.DemoApiClient
import wvlet.airframe.rx.Rx
import wvlet.airspec.AirSpec

/**
  */
object GrpcStreamingTest extends AirSpec {

  private def router = Router.add[DemoApi]
  debug(router)

  protected override def design = gRPC.server.withRouter(router).designWithChannel

  // launching a gRPC server first
  test("Launch a standalone gRPC server") { server: GrpcServer =>
    test("Test gRPC client methods") { stub: DemoApiClient =>
      val N = 10

      test("unary") {
        for (i <- 0 to N) {
          val ret = stub.hello("world")
          ret shouldBe "Hello world!"
        }
      }

      test("n-ary") {
        for (i <- 0 to N) {
          val ret2 = stub.hello2("world", i)
          ret2 shouldBe s"Hello world! (id:${i})"
        }
      }

      test("server streaming") {
        val streamingResults = stub.helloStreaming("RPC").toIndexedSeq
        streamingResults shouldBe Seq("Hello RPC!", "Bye RPC!")
      }

      test("client streaming") {
        for (i <- 0 to N) {
          val result = stub.helloClientStreaming(Rx.sequence("Apple", "Banana"))
          result shouldBe "Apple, Banana"
        }
      }

      test("bidi streaming") {
        for (i <- 0 to N) {
          val result = stub.helloBidiStreaming(Rx.sequence("Apple", "Banana")).toSeq
          result shouldBe Seq("Hello Apple!", "Hello Banana!")
        }
      }

      test("opt arg") {
        stub.helloOpt(Some("opt-arg")) shouldBe "Hello opt-arg!"
        stub.helloOpt(None) shouldBe "Hello unknown!"
      }
    }
  }
}
