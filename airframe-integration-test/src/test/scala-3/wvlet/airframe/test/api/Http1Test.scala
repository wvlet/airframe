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
package wvlet.airframe.test.api

import wvlet.airframe.Design
import wvlet.airframe.http.{Http, HttpServer, RxRouter}
import wvlet.airframe.http.netty.Netty
import wvlet.airframe.test.api.ServiceRPC.RPCSyncClient
import wvlet.airspec.AirSpec

class Http1Test extends AirSpec {

  initDesign {
    _ + Netty.server
      .withName("hello-rpc-test")
      .withRouter(RxRouter.of[HelloRPCImpl])
      .design
      .bind[ServiceRPC.RPCSyncClient].toProvider { (server: HttpServer) =>
        // Use HTTP1 client
        ServiceRPC.newRPCSyncClient(Http.client.withHTTP1.newSyncClient(server.localAddress))
      }
  }

  test("rpc") { (client: RPCSyncClient) =>
    client.HelloRPC.hello("RPC") shouldBe "Hello RPC!"
  }
}
