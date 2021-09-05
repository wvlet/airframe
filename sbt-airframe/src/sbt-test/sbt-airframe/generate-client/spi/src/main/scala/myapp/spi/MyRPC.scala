package myapp.spi

import wvlet.airframe.http._

@RPC
trait MyRPC {
  import MyRPC._

  def world(): World
  def addEntry(id: Int, name: String): String

  def createPage(createPageRequest: CreatePageRequest): String
  def createPageWithId(id: Int, createPageRequest: CreatePageRequest): String
}

object MyRPC {
  case class World(name: String)
  case class CreatePageRequest(name: String)
}
