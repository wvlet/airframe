package example.api

import wvlet.airspec._
import wvlet.airframe._
import wvlet.airframe.http._
import wvlet.airframe.http.grpc.gRPC
import wvlet.airframe.http.grpc.GrpcServer
import io.grpc.ManagedChannelBuilder
import io.grpc.ManagedChannel
import io.grpc.stub.StreamObserver

object GreeterApiTest extends AirSpec {
  class GreeterApiImpl extends GreeterApi {
    def sayHello(message: String): String = s"Hello ${message}!"
  }
  private val router = Router.of[GreeterApiImpl]

  protected override def design = {
    gRPC.server
      .withRouter(router).design
      .bind[ManagedChannel].toProvider { server: GrpcServer =>
        ManagedChannelBuilder.forTarget(server.localAddress).usePlaintext().build()
      }
      .onShutdown(_.shutdownNow)
  }

  test("test grpc server") { channel: ManagedChannel =>
    val syncClient = ServiceGrpcClient.newSyncClient(channel)
    val ret        = syncClient.GreeterApi.sayHello("Airframe gRPC")
    info(s"sync response: ${ret}")
    ret shouldBe "Hello Airframe gRPC!"

    val asyncClient = ServiceGrpcClient.newAsyncClient(channel)
    asyncClient.GreeterApi.sayHello(
      "Airframe gRPC",
      new StreamObserver[String] {
        def onNext(v: String): Unit = {
          logger.info(s"async response: ${v}")
        }
        def onError(t: Throwable): Unit = {
          logger.error(t)
        }
        def onCompleted(): Unit = {
          logger.info("completed")
        }
      }
    )
  }
}
