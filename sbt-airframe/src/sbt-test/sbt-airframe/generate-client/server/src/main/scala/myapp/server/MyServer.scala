package myapp.server

import wvlet.airframe.http.Router
import wvlet.airframe.http.finagle._
import wvlet.log.LogSupport
import myapp.spi.MyService
import myapp.spi.MyRPC._
import com.twitter.util.Await

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
    val router = Router.add[MyServiceImpl].add[MyRPCImpl]
    info(router)

    val d =
      newFinagleServerDesign(router = router)
        .add(finagleClientDesign)

    d.build[FinagleClient] { finagleClient =>
      val client = new myapp.spi.ServiceClient(finagleClient)
      val future = client.myService.hello(100).map { v =>
        logger.info(v)
        assert(v == "hello 100")
      }
      Await.result(future)

      val syncClient = new myapp.spi.ServiceSyncClient(finagleClient.syncClient)
      val ret        = syncClient.myService.hello(101)
      info(ret)
      assert(ret == "hello 101")

      val future2 = client.myService.books(10).map { v =>
        logger.info(v)
        assert(v == s"10 books")
      }
      Await.result(future2)

      val ret2 = syncClient.myRPC.world()
      info(ret2)
      assert(ret2 == myapp.spi.MyRPC.World("world"))

      val ret3 = syncClient.myRPC.addEntry(1234, "rpc")
      info(ret3)
      assert(ret3 == "1234:rpc")

      val ret4 = syncClient.myRPC.createPage(CreatePageRequest("hello"))
      info(ret4)
      assert(ret4 == """0:CreatePageRequest(hello)""")

      val ret5 = syncClient.myRPC.createPageWithId(10, CreatePageRequest("hello"))
      info(ret5)
      assert(ret5 == """10:CreatePageRequest(hello)""")
    }
  }
}
