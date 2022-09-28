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
package wvlet.airframe.http
import wvlet.airspec.AirSpec

/**
  */
object RPCTest extends AirSpec {

  case class BookRequest(id: String)
  case class Book(id: String, name: String)

  @RPC(path = "/v1", description = "My RPC service interface")
  trait MyRPCService {
    def hello: String
    def book(request: BookRequest): Seq[Book]
  }

  test("Create a router from RPC annotation") {
    val r = Router.add[MyRPCService]
    debug(r)

    r.routes.forall(_.isRPC) shouldBe true

    val m1 = r.routes.find(_.path == "/v1/MyRPCService/hello")
    m1 shouldBe defined
    m1.get.method shouldBe HttpMethod.POST
    m1.get.methodSurface.name shouldBe "hello"

    val m2 = r.routes.find(_.path == "/v1/MyRPCService/book")
    m2 shouldBe defined
    m2.get.method shouldBe HttpMethod.POST
    m2.get.methodSurface.name shouldBe "book"
  }

  @RPC(description = "My RPC service interface")
  trait MyRPCService2 {
    def hello: String
  }

  test("Create RPC interface without any path") {
    val r = Router.add[MyRPCService2]
    debug(r)
    val m = r.routes.find(_.path == "/wvlet.airframe.http.RPCTest.MyRPCService2/hello")
    m shouldBe defined

    m.get.method shouldBe HttpMethod.POST
    m.get.methodSurface.name shouldBe "hello"
  }

  @RPC(path = "/v1", description = "My RPC service interface")
  trait MyRPCService3 {
    @RPC(path = "/hello_world")
    def hello: String
  }

  test("Create RPC interface with full paths") {
    val r = Router.add[MyRPCService3]
    debug(r)
    val m = r.routes.find(_.path == "/v1/MyRPCService3/hello_world")
    m shouldBe defined
    m.get.method shouldBe HttpMethod.POST
    m.get.methodSurface.name shouldBe "hello"
  }

  @RPC
  trait RPCOverload {
    def hello: String
    def hello(s: String): String
  }

  test("Should detect RPC method overload") {
    val r = Router.add[RPCOverload]
    val e = intercept[IllegalArgumentException] {
      r.verifyRoutes
    }
    e.getMessage.contains("RPCOverload/hello")
  }
}
