package example.api

import wvlet.airframe.http.*
import wvlet.log.LogSupport

@RPC
class MyRPCApi extends LogSupport {
  import MyRPCApi.*
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
