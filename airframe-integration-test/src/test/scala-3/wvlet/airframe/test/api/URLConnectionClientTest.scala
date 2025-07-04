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
package wvlet.airframe.test.api

import wvlet.airframe.Design
import wvlet.airframe.codec.MessageCodec
import wvlet.airframe.http.HttpMessage.Response
import wvlet.airframe.http.{Http, HttpClientException, HttpStatus, RxRouter}
import wvlet.airframe.http.client.{SyncClient, URLConnectionClientBackend}
import wvlet.airframe.http.netty.{Netty, NettyServer}
import wvlet.airspec.AirSpec
import wvlet.log.Logger

/**
  * URLConnectionClient test using local Netty server instead of external httpbin.org
  */
class URLConnectionClientTest extends AirSpec:

  override protected def design: Design =
    Design.newDesign
      .add(
        Netty.server
          .withRouter(RxRouter.of[MockServer])
          .design
      )
      .bind[SyncClient].toProvider { (server: NettyServer) =>
        Http.client
          .withBackend(URLConnectionClientBackend)
          .withJSONEncoding
          .newSyncClient(server.localAddress)
      }

  case class Person(id: Int, name: String)

  val p     = Person(1, "leo")
  val pJson = MessageCodec.of[Person].toJson(p)

  private def check(r: Response): Unit =
    r.status shouldBe HttpStatus.Ok_200
    val m = MessageCodec.of[Map[String, Any]].fromJson(r.contentString)
    check(m)

  private def check(m: Map[String, Any]): Unit =
    m("json") shouldBe Map("id" -> 1, "name" -> "leo")

  test("sync client") { (client: SyncClient) =>
    test("complement missing slash") {
      val resp = client.sendSafe(Http.GET("get"))
      resp.status shouldBe HttpStatus.Ok_200
    }

    test("Read content with 200") {
      val resp = client.sendSafe(Http.GET("/get"))
      debug(resp)
      resp.status shouldBe HttpStatus.Ok_200
      debug(resp.contentString)
    }

    test("user-agent") {
      val resp =
        client
          .withRequestFilter(_.withUserAgent("airframe-http"))
          .sendSafe(Http.GET("/user-agent"))
      MessageCodec.of[Map[String, Any]].fromJson(resp.contentString).get("user-agent") shouldBe Some("airframe-http")
    }

    test("delete") {
      val resp = client.sendSafe(Http.DELETE("/delete"))
      resp.status shouldBe HttpStatus.Ok_200
    }

    test("patch") {
      // URLConnection doesn't support PATCH method properly, skip this test
      pending("URLConnection client doesn't support PATCH method")
    }

    test("call with Response return value") {
      check(client.call[Person, Response](Http.POST("/post"), p))
      check(client.call[Person, Response](Http.PUT("/put"), p))
    }

    test("call with GET") {
      val m = client.call[Person, Map[String, Any]](Http.GET(s"/get"), p)
      m("args") shouldBe Map("id" -> "1", "name" -> "leo")
    }

    test("call with POST") {
      check(client.call[Person, Map[String, Any]](Http.POST("/post"), p))
    }

    test("call with PUT") {
      check(client.call[Person, Map[String, Any]](Http.PUT("/put"), p))
    }

    test("Handle 404 (Not Found)") {
      val errorResp = client.sendSafe(Http.GET("/status/404"))
      debug(errorResp)
      errorResp.status shouldBe HttpStatus.NotFound_404
      debug(errorResp.contentString)
    }
  }

  test("retry test") { (client: SyncClient) =>
    test("Handle 5xx retry") {
      Logger("wvlet.airframe.http.HttpClient").suppressWarnings {
        flaky {
          val e = intercept[HttpClientException] {
            client
              .withRetryContext(_.withMaxRetry(1))
              .send(Http.GET("/status/500"))
          }
          e.status shouldBe HttpStatus.InternalServerError_500
        }
      }
    }
  }
