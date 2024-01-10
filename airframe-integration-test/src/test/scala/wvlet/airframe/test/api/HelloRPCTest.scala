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
import wvlet.airframe.http.netty.Netty
import wvlet.airframe.http.{Http, HttpServer, RPCEncoding, RxRouter}
import wvlet.airframe.test.api.HelloRPC.VariousParams
import wvlet.airspec.AirSpec

class HelloRPCTest extends AirSpec {

  override protected def design: Design = {
    Netty.server
      .withName("hello-rpc-test")
      .withRouter(RxRouter.of[HelloRPCImpl])
      .design
      .bind[ServiceRPC.RPCSyncClient].toProvider { (server: HttpServer) =>
        ServiceRPC.newRPCSyncClient(Http.client.newSyncClient(server.localAddress))
      }
      .bind[ServiceRPC.RPCAsyncClient].toProvider { (server: HttpServer) =>
        ServiceRPC.newRPCAsyncClient(Http.client.newAsyncClient(server.localAddress))
      }
  }

  test("rpc") { (server: HttpServer) =>
    test("sync client") { (client: ServiceRPC.RPCSyncClient) =>
      test("String response") {
        client.HelloRPC.hello("RPC") shouldBe "Hello RPC!"
      }

      test("case object response") {
        client.HelloRPC.serverStatus() shouldBe Status.OK
      }

      test("case object input") {
        client.HelloRPC.ackStatus(Status.OK) shouldBe Status.OK
      }

      test("case class with various data types") {
        val resp = client.HelloRPC.variousParams(VariousParams(p1 = 1L, p2 = true, p3 = 1.0))
        resp shouldBe VariousParams(p1 = 1L, p2 = true, p3 = 1.0)
      }

      test("json encoding") {
        client
          .withRPCEncoding(RPCEncoding.JSON).withResponseFilter { resp =>
            resp.isContentTypeJson shouldBe true
            resp
          }.HelloRPC.serverStatus() shouldBe Status.OK
      }

      test("msgpack encoding") {
        client
          .withRPCEncoding(RPCEncoding.MsgPack).withResponseFilter { resp =>
            resp.isContentTypeMsgPack shouldBe true
            resp
          }.HelloRPC.serverStatus() shouldBe Status.OK
      }
    }

    test("async client") { (client: ServiceRPC.RPCAsyncClient) =>
      client.HelloRPC.hello("RPC").map { ret =>
        ret shouldBe "Hello RPC!"
      }
    }

  }

}
