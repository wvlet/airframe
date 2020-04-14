package myapp.spi

import wvlet.airframe.http._

@RPC
trait MyRPC {
  import MyRPC._

  def world(): World
}

object MyRPC {
  case class World(name: String)
}
