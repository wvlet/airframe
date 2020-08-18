package example.api

import wvlet.airspec._
import wvlet.airframe._
import wvlet.airframe.rx.Rx
import wvlet.airframe.http._
import wvlet.airframe.http.grpc.gRPC
import wvlet.airframe.http.grpc.GrpcServer
import io.grpc.ManagedChannelBuilder
import io.grpc.ManagedChannel
import io.grpc.stub.StreamObserver

object GreeterApiTest extends AirSpec {
  class GreeterApiImpl extends GreeterApi {
    def sayHello(message: String): String = s"Hello ${message}!"
    def serverStreaming(message: String): Rx[String] = {
      Rx.sequence("Hello", "See you").map { x => s"${x} ${message}!" }
    }
    def clientStreaming(message: Rx[String]): String = {
      message.map { x => s"Hello ${x}!" }.toSeq.mkString(", ")
    }
    def bidiStreaming(messaage: Rx[String]): Rx[String] = {
      message.map { x => s"Hello ${x}!" }
    }
  }
  private val router = Router.of[GreeterApiImpl]

  protected override def design = {
    gRPC.server
      .withRouter(router).design
      .bind[ManagedChannel].toProvider { server: GrpcServer =>
        ManagedChannelBuilder.forTarget(server.localAddress).usePlaintext().build()
      }
      .onShutdown(_.shutdownNow)
      .bind[ServiceGrpcClient].toProvider { channel: ManagedChannel => ServiceGrpcClient.newSyncClient(channel) }
  }

  test("test unary RPC") { syncClient: ServiceGrpcClient =>
    val ret = syncClient.GreeterApi.sayHello("Airframe gRPC")
    info(s"sync response: ${ret}")
    ret shouldBe "Hello Airframe gRPC!"
  }

//  test("test async RPC") { channel: ManagedChannel =>
//    val asyncClient = ServiceGrpcClient.newAsyncClient(channel)
//    asyncClient.GreeterApi.sayHello(
//      "Airframe gRPC",
//      new StreamObserver[String] {
//        def onNext(v: String): Unit = {
//          logger.info(s"async response: ${v}")
//        }
//        def onError(t: Throwable): Unit = {
//          logger.error(t)
//        }
//        def onCompleted(): Unit = {
//          logger.info("completed")
//        }
//      }
//    )
// }

  test("test streaming") { syncClient: ServiceGrpcClient =>
    val r1 = syncClient.GreeterApi.serverStreaming("gRPC")
    r1.toSeq shouldBe Seq("Hello gRPC!", "See you gRPC!")

    val r2 = syncClient.GreeterApi.clientStreaming(Rx.sequence("airframe", "rpc"))
    r2 shouldBe "Hello airframe!, Hello rpc!"

    val r3 = syncClient.GreeterApi.bidiStreaming(Rx.sequence("airframe", "rpc"))
    r3.toSeq shouldBe Seq("Hello airframe!", "Hello rpc!")
  }

}
