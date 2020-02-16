package example

import wvlet.airframe.http._

trait MyApp {
  @Endpoint(method = HttpMethod.GET, path = "/v1/hello/:id")
  def hello(id: Int): String = {
    s"hello: ${id}"
  }
}
