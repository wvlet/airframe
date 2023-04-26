package example

import wvlet.airspec.AirSpec
import wvlet.airframe._
import wvlet.airframe.http.{Http, Router}
import wvlet.airframe.http.finagle.{Finagle, FinagleServer}
import example.api.MyRPCApi
import example.api.MyRPCApi.{HelloRequest, HelloResponse}
import example.api.MyRPCClient
import example.api.MyRPCClient.RPCSyncClient

class MyRPCTest extends AirSpec {

  private val router = Router.of[MyRPCApi]

  override protected def design: Design = {
    Finagle.server
      .withRouter(router).design
      .bind[RPCSyncClient].toProvider { (server: FinagleServer) =>
        MyRPCClient.newRPCSyncClient(Http.client.newSyncClient(server.localAddress))
      }
  }

  test("Access RPC") { (client: RPCSyncClient) =>
    client.MyRPCApi.helloRPC(HelloRequest("Airframe")) shouldBe HelloResponse("Hello Airframe!")
  }
}
