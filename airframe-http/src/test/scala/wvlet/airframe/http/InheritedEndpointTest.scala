package wvlet.airframe.http

import wvlet.airspec._
import wvlet.airframe.surface.Surface

object InheritedEndpointTest extends AirSpec {

  trait Base {
    @Endpoint(path = "/hello")
    def hello: String = "hello"
  }
  trait MyApp extends Base

  def `find inherited endpoints`: Unit = {
    val router = Router.of[MyApp]
    router.routes.find(_.path == "/hello") shouldBe defined
  }
}
