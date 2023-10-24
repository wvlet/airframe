package example

import wvlet.airspec.AirSpec
import wvlet.airframe.*
import wvlet.airframe.http.*
import wvlet.airframe.http.finagle.{Finagle, FinagleServer}
import example.api.MyRPCApi
import example.api.MyRPCApi.{HelloRequest, HelloResponse}
import example.api.MyRPCClient
import example.api.MyRPCClient.RPCAsyncClient

class MyRPCTest extends AirSpec {

  private val router = RxRouter.of[MyRPCApi]

  override protected def design: Design = {
    Finagle.server
      .withRouter(router).design
      .bind[RPCAsyncClient].toProvider { (server: FinagleServer) =>
        MyRPCClient.newRPCAsyncClient(Http.client.newAsyncClient(server.localAddress))
      }
  }

  test("Access RPC") { (client: RPCAsyncClient) =>
    client.MyRPCApi.helloRPC(HelloRequest("Airframe")).map { ret =>
      ret shouldBe HelloResponse("Hello Airframe!")
    }
  }
}
