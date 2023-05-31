package example

import wvlet.airspec.AirSpec
import wvlet.airframe._
import wvlet.airframe.http.Http
import wvlet.airframe.http.netty.{Netty, NettyServer}
import example.api.MyRPCApi
import example.api.MyRPCApi.{HelloRequest, HelloResponse}
import example.api.MyRPCClient
import example.api.MyRPCClient.RPCSyncClient

class MyRPCTest extends AirSpec {
  override protected def design: Design = {
    Netty.server
      .withRouter(MyRPCApi.router).design
      .bind[RPCSyncClient].toProvider[NettyServer] { (server: NettyServer) =>
        MyRPCClient.newRPCSyncClient(Http.client.newSyncClient(server.localAddress))
      }
  }

  test("Access RPC") { (client: RPCSyncClient) =>
    val ret = client.MyRPCApi.helloRPC(HelloRequest("Airframe"))
    info(ret)
    ret shouldBe HelloResponse("Hello Airframe!")
  }
}
