package myapp.server

import wvlet.airframe.http._
import wvlet.airframe.http.client.SyncClient
import wvlet.airframe.http.netty.Netty
import wvlet.log.LogSupport
import myapp.spi.MyService
import myapp.spi.MyRPC._

class MyServiceImpl extends myapp.spi.MyService {
  override def hello(id: Int): String    = s"hello ${id}"
  override def books(limit: Int): String = s"${limit} books"
}

class MyRPCImpl extends myapp.spi.MyRPC {
  override def world()                                                                 = myapp.spi.MyRPC.World("world")
  override def addEntry(id: Int, name: String)                                         = s"${id}:${name}"
  override def createPage(createPageRequest: CreatePageRequest): String                = s"${0}:${createPageRequest}"
  override def createPageWithId(id: Int, createPageRequest: CreatePageRequest): String = s"${id}:${createPageRequest}"
}

object MyServer extends LogSupport {

  def main(args: Array[String]): Unit = {
    val router = RxRouter.of(
      RxRouter.of[MyServiceImpl],
      RxRouter.of[MyRPCImpl]
    )
    info(router)

    val d =
      Netty.server
        .withRouter(router)
        .designWithSyncClient

    d.build[SyncClient] { httpClient =>
      val syncClient = myapp.spi.ServiceRPC.newRPCSyncClient(httpClient)
      val ret        = syncClient.MyService.hello(101)
      info(ret)
      assert(ret == "hello 101")

      val ret2 = syncClient.MyRPC.world()
      info(ret2)
      assert(ret2 == myapp.spi.MyRPC.World("world"))

      val ret3 = syncClient.MyRPC.addEntry(1234, "rpc")
      info(ret3)
      assert(ret3 == "1234:rpc")

      val ret4 = syncClient.MyRPC.createPage(CreatePageRequest("hello"))
      info(ret4)
      assert(ret4 == """0:CreatePageRequest(hello)""")

      val ret5 = syncClient.MyRPC.createPageWithId(10, CreatePageRequest("hello"))
      info(ret5)
      assert(ret5 == """10:CreatePageRequest(hello)""")
    }
  }
}
