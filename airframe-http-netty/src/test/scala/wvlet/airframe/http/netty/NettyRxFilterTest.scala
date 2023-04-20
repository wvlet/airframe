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

import wvlet.airframe.Design
import wvlet.airframe.http.client.SyncClient
import wvlet.airframe.http.{Http, HttpMessage, RPCException, RPCStatus, RxEndpoint, RxFilter}
import wvlet.airframe.http.router.RxRouter
import wvlet.airframe.rx.Rx
import wvlet.airspec.AirSpec

object NettyRxFilterTest extends AirSpec {
  class MyRPC {
    def hello(msg: String): String = s"Hello ${msg}!"
  }

  class AuthFilter extends RxFilter {
    override def apply(request: HttpMessage.Request, endpoint: RxEndpoint): Rx[HttpMessage.Response] = {
      request.authorization match {
        case Some(auth) if auth == "Bearer xxxx" =>
          endpoint(request)
        case _ =>
          Rx.exception(RPCStatus.UNAUTHENTICATED_U13.newException("authentication failed"))
      }
    }
  }

  private def router = RxRouter.filter[AuthFilter].andThen(RxRouter.of[MyRPC])

  override protected def design: Design = {
    Netty.server
      .withRouter(router)
      .designWithSyncClient
  }

  test("Run server with filter") { (client: SyncClient) =>
    test("when no auth header") {
      val ex = intercept[RPCException] {
        client.send(Http.POST("/hello").withJson("""{"msg":"Netty"}"""))
      }
      ex.status shouldBe RPCStatus.UNAUTHENTICATED_U13
      ex.message shouldBe "authentication failed"
    }

    test("with auth header") {
      val resp = client.send(Http.POST("/hello").withJson("""{"msg":"Netty"}""").withAuthorization("Bearer xxxx"))
      resp.contentString shouldBe "Hello Netty!"
    }
  }
}
