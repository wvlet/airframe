package myapp.client

import myapp.spi._

object Main {

  val jsClient = new ServiceJSClient()
  val rpcClient = new ServiceJSClientRx()

  import MyRPC._

  // JS client
  jsClient.MyRPC.hello(System.currentTimeMillis(), HelloRequest())

  // RPC client that returns Rx[X] type
  rpcClient.MyRPC.hello(System.currentTimeMillis(), HelloRequest())
}
