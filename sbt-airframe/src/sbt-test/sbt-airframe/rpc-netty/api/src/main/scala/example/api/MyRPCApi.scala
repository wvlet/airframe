package example.api

import wvlet.airframe.http.RPC
import wvlet.airframe.http.router.{RxRouter,RxRouterProvider}
import wvlet.log.LogSupport

@RPC
trait MyRPCApi extends LogSupport {
  import MyRPCApi._
  def helloRPC(request: HelloRequest): HelloResponse = {
    val message = s"Hello ${request.name}!"
    HelloResponse(message)
  }

}

object MyRPCApi extends RxRouterProvider {
  override def router = RxRouter.of[MyRPCApi]

  case class HelloRequest(name: String)
  case class HelloResponse(message: String)
}
