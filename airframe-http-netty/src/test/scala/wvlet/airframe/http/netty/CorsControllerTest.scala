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
package wvlet.airframe.http.netty

import wvlet.airframe.http.*
import wvlet.airframe.http.client.SyncClient
import wvlet.airframe.http.filter.Cors
import wvlet.airspec.AirSpec

object CorsControllerTest extends AirSpec {
  @RPC
  class MyApi {
    def hello(msg: String): String = s"Hello ${msg}!"
  }

  private val corsRouter = RxRouter
    .filter(Cors.newFilter(Cors.unsafePermissivePolicy))
    .andThen(RxRouter.of[MyApi])
}

/**
  * Test that CORS filter works with controller-based routing via RxRouter.filter(...). Previously, OPTIONS preflight
  * requests would return 404 because the route matcher rejects unmatched HTTP methods before filters run.
  */
class CorsControllerTest extends AirSpec {
  import CorsControllerTest.*

  initDesign { d =>
    Netty.server.withRouter(corsRouter).designWithSyncClient
  }

  test("handle OPTIONS preflight for controller routes") { (client: SyncClient) =>
    val resp = client.send(
      Http
        .request(HttpMethod.OPTIONS, "/wvlet.airframe.http.netty.CorsControllerTest.MyApi/hello")
        .withHeader("Origin", "https://example.com")
        .withHeader("Access-Control-Request-Method", "POST")
        .withHeader("Access-Control-Request-Headers", "Content-Type")
    )
    resp.statusCode shouldBe 200
    resp.header.get("access-control-allow-origin") shouldBe Some("https://example.com")
    resp.header.get("access-control-allow-methods") shouldBe Some("POST")
    resp.header.get("access-control-allow-headers") shouldBe Some("Content-Type")
    resp.header.get("access-control-allow-credentials") shouldBe Some("true")
  }

  test("add CORS headers to normal responses") { (client: SyncClient) =>
    val resp = client.send(
      Http
        .POST("/wvlet.airframe.http.netty.CorsControllerTest.MyApi/hello")
        .withJson("""{"msg":"World"}""")
        .withHeader("Origin", "https://example.com")
    )
    resp.statusCode shouldBe 200
    resp.contentString shouldBe "Hello World!"
    resp.header.get("access-control-allow-origin") shouldBe Some("https://example.com")
    resp.header.get("access-control-allow-credentials") shouldBe Some("true")
  }
}
