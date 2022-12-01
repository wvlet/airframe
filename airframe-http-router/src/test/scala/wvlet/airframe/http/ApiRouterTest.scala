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
import wvlet.airframe.http.router.ControllerRoute

/**
  */
object ApiRouterTest extends AirSpec {

  @Endpoint(path = "/v1")
  trait MyApi {
    @Endpoint(path = "/hello")
    def hello: String
  }

  class MyApiImpl extends MyApi {
    override def hello: String = "hello"
  }

  test("Create a router using API implementations") {
    val r = Router.add[MyApiImpl]

    r.routes shouldNotBe empty
    val x = r.routes.head
    x.httpMethod shouldBe HttpMethod.GET
    x.path shouldBe "/v1/hello"
    x match {
      case c: ControllerRoute =>
        c.rpcMethod.rpcInterfaceName shouldBe "wvlet.airframe.http.ApiRouterTest.MyApi"
      case _ =>
        fail("cannot reach here")
    }
  }
}
