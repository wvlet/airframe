package example.api

import wvlet.airframe.http.RPC

@RPC
trait GreeterApi {
  def sayHello(message: String): String
}
