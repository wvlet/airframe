package myapp.client

import myapp.spi.*
import wvlet.airframe.http.Http

object Main {

  val rpcClient = ServiceRPC.newRPCAsyncClient(Http.client.newJSClient)

  import MyRPC.*

  // RPC client that returns Rx[X] type
  rpcClient.MyRPC.hello(System.currentTimeMillis(), HelloRequest()).run { case x =>
    println(x)
  }
}
