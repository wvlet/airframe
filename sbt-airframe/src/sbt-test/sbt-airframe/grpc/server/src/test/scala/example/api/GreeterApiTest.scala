package example.api

import wvlet.airspec._
import wvlet.airframe.http._
import wvlet.airframe.http.grpc.gRPC
import io.grpc.ManagedChannelBuilder

object GreeterApiTest extends AirSpec {
  class GreeterApiImpl extends GreeterApi {
    def sayHello(message: String): String = s"Hello ${message}!"
  }

  private val router = Router.of[GreeterApiImpl]

  test("test grpc server") {
    gRPC.server.withRouter(router).start { server =>
      val channel = ManagedChannelBuilder.forTarget(server.localAddress).usePlaintext().build()
      val client  = new ServiceGrpcSyncClient(channel)

      val ret = client.GreeterApi.sayHello("Airframe gRPC")
      info(ret)
      ret shouldBe "Hello Airframe gRPC!"

      client.close()
    }
  }
}
