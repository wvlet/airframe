package myapp.spi

import wvlet.airframe.http._

trait MyService {
  @Endpoint(method = HttpMethod.GET, path = "/v1/hello/:id")
  def hello(id: Int): String

  @Endpoint(method = HttpMethod.GET, path = "/v1/books")
  def books(limit: Int = 100): String
}
