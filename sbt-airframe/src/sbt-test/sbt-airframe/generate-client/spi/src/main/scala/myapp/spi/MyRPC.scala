package myapp.spi

import wvlet.airframe.http._

@RPC
trait MyRPC {
  def world: String
}
