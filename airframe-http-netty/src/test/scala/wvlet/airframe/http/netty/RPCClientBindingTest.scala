/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package wvlet.airframe.http.netty

import wvlet.airframe.http._
import wvlet.airframe.http.client.SyncClient
import wvlet.airframe.http.netty.MyRPCClient.RPCSyncClient
import wvlet.airframe.http.netty.RPCClientBindingTest.MyApi.{HelloRequest, HelloResponse}
import wvlet.airframe.surface.Surface
import wvlet.airspec.AirSpec

object MyRPCClient {
  def newRPCSyncClient(client: SyncClient): RPCSyncClient = new RPCSyncClient(client)

  object internal {
    object MyRPCApiInternals {
      case class __helloRPC_request(request: HelloRequest)
      val __m_helloRPC = RPCMethod(
        "/wvlet.airframe.http.netty.RPCClientBindingTest.MyApi/hello",
        "wvlet.airframe.http.netty.RPCClientBindignTest.MyApi",
        "hello",
        Surface.of[__helloRPC_request],
        Surface.of[HelloResponse]
      )
    }
  }

  class RPCSyncClient(client: SyncClient)
      extends wvlet.airframe.http.client.ClientFactory[RPCSyncClient]
      with AutoCloseable {
    override protected def build(newConfig: HttpClientConfig): RPCSyncClient = {
      new RPCSyncClient(client.withConfig(_ => newConfig))
    }
    override protected def config: HttpClientConfig = client.config

    override def close(): Unit = {
      client.close()
    }

    def getClient: SyncClient = client

    object MyRPCApi {

      import internal.MyRPCApiInternals._

      def helloRPC(request: HelloRequest): HelloResponse = {
        client.rpc[__helloRPC_request, HelloResponse](
          __m_helloRPC,
          __helloRPC_request(request = request)
        )
      }
    }
  }
}

object RPCClientBindingTest extends AirSpec {
  import MyApi._
  @RPC
  class MyApi {
    def hello(request: HelloRequest): HelloResponse = HelloResponse(s"Hello ${request.msg}!")
  }

  object MyApi {
    case class HelloRequest(msg: String)
    case class HelloResponse(msg: String)
  }

  protected override def design =
    Netty.server
      .withRouter(RxRouter.of[MyApi]).design
      .bind[MyRPCClient.RPCSyncClient].toProvider { (server: NettyServer) =>
        MyRPCClient.newRPCSyncClient(Http.client.newSyncClient(server.localAddress))
      }

  test("Create a surface of an RPC client") {
    Surface.of[RPCSyncClient]
  }

  test("Start an RPC server with a client") { (client: RPCSyncClient) =>
    client.MyRPCApi.helloRPC(HelloRequest("Netty")).msg shouldBe "Hello Netty!"
  }
}
