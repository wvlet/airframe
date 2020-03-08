package myapp.server

import wvlet.airframe.http.{Endpoint, HttpMethod, Router, finagle}
import wvlet.airframe.http.finagle._
import wvlet.log.LogSupport
import wvlet.airframe.control.Control
import com.twitter.util.Await
import myapp.spi.MyService

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
      val future = client.myService.hello(100).map { v => logger.info(v) }
      Await.result(future)
    }
  }
}
