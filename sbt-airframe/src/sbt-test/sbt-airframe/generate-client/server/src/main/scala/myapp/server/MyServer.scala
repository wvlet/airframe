package myapp.server

import wvlet.airframe.http.Router
import wvlet.airframe.http.finagle._
import wvlet.log.LogSupport
import myapp.spi.MyService
import com.twitter.util.Await

class MyServiceImpl extends myapp.spi.MyService {
  override def hello(id: Int): String    = s"hello ${id}"
  override def books(limit: Int): String = s"${limit} books"
}

object MyServer extends LogSupport {

  def main(args: Array[String]): Unit = {
    val router = Router.of[MyServiceImpl]

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
    }
  }
}
