package myapp.spi

import wvlet.airframe.http._

trait MyService {
  @Endpoint(method = HttpMethod.GET, path = "/v1/hello/:id")
  def hello(id: Int): String
}
