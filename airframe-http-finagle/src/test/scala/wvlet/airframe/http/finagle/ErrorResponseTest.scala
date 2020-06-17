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
package wvlet.airframe.http.finagle
import com.twitter.finagle.http.Request
import wvlet.airframe.Design
import wvlet.airframe.codec.MessageCodec
import wvlet.airframe.http.{Endpoint, Http, HttpHeader, HttpMessage, HttpStatus, RPC, Router}
import wvlet.airspec.AirSpec
import wvlet.log.LogSupport

/**
  */
object ErrorResponseTest extends AirSpec {

  case class ErrorResponse(code: Int, message: String)

  trait MyApp extends LogSupport {
    @Endpoint(path = "/v1/test")
    def errorResponse(req: HttpMessage.Request): String = {
      throw Http.serverException(req, HttpStatus.BadRequest_400, ErrorResponse(10, "error test"))
    }
  }

  test(
    "Return error response using JSON/MsgPack",
    design = Finagle.server.withRouter(Router.of[MyApp]).design + Finagle.client.syncClientDesign
  ) { client: FinagleSyncClient =>
    warn(s"Running an error response test")

    test("json error response") {
      val resp = client.sendSafe(Request("/v1/test"))
      resp.statusCode shouldBe 400
      resp.contentString shouldBe """{"code":10,"message":"error test"}"""
    }

    test("msgpack error response") {
      val req = Request("/v1/test")
      req.accept = HttpHeader.MediaType.ApplicationMsgPack
      val resp = client.sendSafe(req)
      resp.statusCode shouldBe 400
      val msgpack = resp.contentBytes
      val e       = MessageCodec.of[ErrorResponse].fromMsgPack(msgpack)
      e shouldBe ErrorResponse(10, "error test")
    }

  }
}
