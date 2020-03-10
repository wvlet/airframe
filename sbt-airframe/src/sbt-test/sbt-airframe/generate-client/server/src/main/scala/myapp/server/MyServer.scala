package myapp.server

import wvlet.airframe.http.Router
import wvlet.log.LogSupport

class MyServer extends myapp.spi.MyService {
  override def hello(id: Int): String = s"hello ${id}"
}

object MyServer extends LogSupport {

  def main(args: Array[String]): Unit = {
    val router = Router.of[MyService]

    val d =
      newFinagleServerDesign(router = router)
        .add(finagleClientDesign)
        .bind[MyService].to[MyServer]

    d.build[FinagleClient] { finagleClient =>
      val client = new generated.ServiceClient(finagleClient)
      val future = client.myService.hello(100).map { v =>
        logger.info(v)
        assert(v == "hello 100")
      }
      Await.result(future)
    }
  }
}
