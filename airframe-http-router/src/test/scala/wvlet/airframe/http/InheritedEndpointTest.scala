package wvlet.airframe.http

import wvlet.airspec._

object InheritedEndpointTest extends AirSpec {

  trait Base {
    @Endpoint(path = "/hello")
    def hello: String = "hello"
  }
  trait MyApp extends Base

  test("find inherited endpoints") {
    val router = Router.of[MyApp]
    router.routes.find(_.path == "/hello") shouldBe defined
  }
}
