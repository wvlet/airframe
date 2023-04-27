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
import wvlet.airframe.http.{Http, RPC, RxRouter}
import wvlet.airframe.http.client.SyncClient
import wvlet.airspec.AirSpec

class NettyRxRPCServerTest extends AirSpec {

  @RPC
  class MyRPC {
    def helloNetty(msg: String): String  = s"Hello ${msg}!"
    def helloNetty2(msg: String): String = s"Hello ${msg}2!"
  }

  private def router = RxRouter.of[MyRPC]

  override protected def design: Design = {
    Netty.server
      .withRouter(router)
      .designWithSyncClient
  }

  test("Start an RPC server using RxRouter") { (client: SyncClient) =>
    val resp = client.send(
      Http.POST("/wvlet.airframe.http.netty.NettyRxRPCServerTest.MyRPC/helloNetty").withJson("""{"msg":"Netty"}""")
    )
    resp.message.toContentString shouldBe "Hello Netty!"

    val resp2 = client.send(
      Http.POST("/wvlet.airframe.http.netty.NettyRxRPCServerTest.MyRPC/helloNetty2").withJson("""{"msg":"Netty"}""")
    )
    resp2.message.toContentString shouldBe "Hello Netty2!"
  }
}
