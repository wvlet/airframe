package myapp.spi

import wvlet.airframe.http.*

@RPC
trait MyRPC {
  import MyRPC.*

  def world(): World
  def addEntry(id: Int, name: String): String

  def createPage(createPageRequest: CreatePageRequest): String
  def createPageWithId(id: Int, createPageRequest: CreatePageRequest): String
}

object MyRPC extends RxRouterProvider {
  override def router: RxRouter = RxRouter.of[MyRPC]

  case class World(name: String)
  case class CreatePageRequest(name: String)
}
