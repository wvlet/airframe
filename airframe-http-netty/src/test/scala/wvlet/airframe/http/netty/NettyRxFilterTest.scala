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

import wvlet.airframe.http.client.SyncClient
import wvlet.airframe.http.{
  Http,
  HttpClientException,
  HttpMessage,
  RPC,
  RPCException,
  RPCStatus,
  RxHttpEndpoint,
  RxHttpFilter,
  RxRouter
}
import wvlet.airframe.rx.Rx
import wvlet.airspec.AirSpec

object NettyRxFilterTest extends AirSpec {
  @RPC
  class MyRPC {
    def hello(msg: String): String = s"Hello ${msg}!"
  }

  class AuthFilter extends RxHttpFilter {
    override def apply(request: HttpMessage.Request, next: RxHttpEndpoint): Rx[HttpMessage.Response] = {
      request.authorization match {
        case Some(auth) if auth == "Bearer xxxx" =>
          next(request)
        case _ =>
          Rx.exception(RPCStatus.UNAUTHENTICATED_U13.newException("authentication failed"))
      }
    }
  }

  class ExFilter extends RxHttpFilter {
    override def apply(request: HttpMessage.Request, next: RxHttpEndpoint): Rx[HttpMessage.Response] = {
      throw RPCStatus.UNAUTHENTICATED_U13.newException("authentication failed")
    }
  }

  private def router1 = RxRouter
    .filter[AuthFilter].andThen(RxRouter.of[MyRPC])

  private def router2 = RxRouter
    .filter[ExFilter].andThen(RxRouter.of[MyRPC])

  test("Run server with auth filter", design = _.add(Netty.server.withRouter(router1).designWithSyncClient)) {
    (client: SyncClient) =>
      test("when no auth header") {
        val e = intercept[HttpClientException] {
          client.send(
            Http.POST("/wvlet.airframe.http.netty.NettyRxFilterTest.MyRPC/hello").withJson("""{"msg":"Netty"}""")
          )
        }
        e.getCause shouldMatch { case ex: RPCException =>
          ex.status shouldBe RPCStatus.UNAUTHENTICATED_U13
          ex.message shouldBe "authentication failed"
        }
      }

      test("with auth header") {
        val resp = client.send(
          Http
            .POST("/wvlet.airframe.http.netty.NettyRxFilterTest.MyRPC/hello").withJson(
              """{"msg":"Netty"}"""
            ).withAuthorization("Bearer xxxx")
        )
        resp.contentString shouldBe "Hello Netty!"
      }
  }

  test("throw RPCException in a filter", design = _.add(Netty.server.withRouter(router2).designWithSyncClient)) {
    (client: SyncClient) =>
      val e = intercept[HttpClientException] {
        client.send(
          Http.POST("/wvlet.airframe.http.netty.NettyRxFilterTest.MyRPC/hello").withJson("""{"msg":"Netty"}""")
        )
      }
      e.getCause shouldMatch { case ex: RPCException =>
        ex.status shouldBe RPCStatus.UNAUTHENTICATED_U13
        ex.message shouldBe "authentication failed"
      }
  }
}
