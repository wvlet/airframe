package myapp.client

import myapp.spi._
import wvlet.airframe.http.Http

object Main {

  val rpcClient = ServiceRPC.newRPCAsyncClient(Http.client.newJSClient)

  import MyRPC._

  // RPC client that returns Rx[X] type
  rpcClient.MyRPC.hello(System.currentTimeMillis(), HelloRequest()).run {
    case x => println(x)
  }
}
