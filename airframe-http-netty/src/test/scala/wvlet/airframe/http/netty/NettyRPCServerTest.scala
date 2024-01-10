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

import wvlet.airframe.Design
import wvlet.airframe.http.*
import wvlet.airframe.http.client.SyncClient
import wvlet.airframe.rx.Rx
import wvlet.airspec.AirSpec

import java.util.concurrent.TimeUnit

object NettyRPCServerTest extends AirSpec {

  @RPC(path = "/v1")
  class MyRPC {
    def helloNetty(msg: String): String = s"Hello ${msg}!"
    def unitResponse(): Unit            = {}
    def intResponse(): Int              = 1
  }

  private def router = RxRouter.of[MyRPC]

  override protected def design: Design = {
    Netty.server
      .withRouter(router)
      .designWithSyncClient
  }

  test("Start an RPC server") { (client: SyncClient) =>
    test("hello RPC") {
      val resp = client.send(Http.POST("/v1/MyRPC/helloNetty").withJson("""{"msg":"Netty"}}"""))
      resp.message.toContentString shouldBe "Hello Netty!"
    }

    test("Unit response") {
      val resp = client.send(Http.POST("/v1/MyRPC/unitResponse"))
      resp.message.toContentString shouldBe empty
    }

    test("Int response") {
      val resp = client.send(Http.POST("/v1/MyRPC/intResponse"))
      resp.message.toContentString shouldBe "1"
    }
  }

  test("await server test") { (server: HttpServer) =>
    Rx.delay(100, TimeUnit.MILLISECONDS).map(_ => server.close())
      .join(Rx.single(() => server.awaitTermination()))
  }

  test("close and await") { (server: HttpServer) =>
    server.close()
    server.awaitTermination()
  }
}
