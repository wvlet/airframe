package example

import wvlet.airspec.AirSpec
import wvlet.airframe._
import wvlet.airframe.http.Http
import wvlet.airframe.http.netty.{Netty, NettyServer}
import example.api.MyRPCApi
import example.api.MyRPCApi.{HelloRequest, HelloResponse}
import example.api.MyRPCClient
import example.api.MyRPCClient.RPCAsyncClient

class MyRPCTest extends AirSpec {
  override protected def design: Design = {
    Netty.server
      .withRouter(MyRPCApi.router).design
      .bind[RPCAsyncClient].toProvider[NettyServer] { (server: NettyServer) =>
        MyRPCClient.newRPCAsyncClient(Http.client.newAsyncClient(server.localAddress))
      }
  }

  test("Access RPC") { (client: RPCAsyncClient) =>
    client.MyRPCApi.helloRPC(HelloRequest("Airframe")).map { ret =>
      info(ret)
      ret shouldBe HelloResponse("Hello Airframe!")
    }
  }
}
