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

import wvlet.airframe.http.client.{AsyncClient, SyncClient}
import wvlet.airframe.http.{Http, RPC, RxRouter}
import wvlet.airframe.rx.Rx
import wvlet.airspec.AirSpec

import java.util.concurrent.TimeUnit

object NettyRxResponseTest extends AirSpec {

  @RPC
  class RxApi {
    def helloRx(message: String): Rx[String] = {
      Rx.single(s"Hello ${message}!")
    }

    def helloAsyncRx(message: String): Rx[String] = {
      Rx.delay(100, TimeUnit.MILLISECONDS).map(_ => s"Hello ${message}!")
    }
  }

  initDesign {
    _.add(
      Netty.server
        .withRouter(RxRouter.of[RxApi])
        .designWithSyncClient
    )
      .bind[AsyncClient].toProvider { (server: NettyServer) =>
        Http.client.newAsyncClient(server.localAddress)
      }
  }

  test("hello rx") { (client: SyncClient) =>
    val resp = client.send(
      Http
        .POST("/wvlet.airframe.http.netty.NettyRxResponseTest.RxApi/helloRx")
        .withJson("""{"message":"Rx"}""")
    )

    resp.statusCode shouldBe 200
    resp.contentString shouldBe "Hello Rx!"
  }

  test("hello rx async") { (client: AsyncClient) =>
    client
      .withRetryContext(_.noRetry)
      .send(
        Http
          .POST("/wvlet.airframe.http.netty.NettyRxResponseTest.RxApi/helloAsyncRx")
          .withJson("""{"message":"Rx"}""")
      ).map { resp =>
        debug(resp)
        resp.statusCode shouldBe 200
        resp.contentString shouldBe "Hello Rx!"
      }
  }

}
