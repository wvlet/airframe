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
package wvlet.airframe.http.client

import wvlet.airframe.Design
import wvlet.airframe.codec.MessageCodec
import wvlet.airframe.http.HttpMessage.Response
import wvlet.airframe.http.{Http, HttpClientException, HttpStatus}
import wvlet.airspec.AirSpec
import wvlet.log.Logger
import scala.concurrent.duration.Duration
import java.util.concurrent.TimeUnit

/**
  */
object URLConnectionClientTest extends AirSpec {

  // Use a public REST test server - skip tests if unavailable
  private val PUBLIC_REST_SERVICE = "https://httpbin.org/"

  private def isServiceAvailable: Boolean = {
    try {
      val client = Http.client.withJSONEncoding
        .withConnectTimeout(Duration(5, TimeUnit.SECONDS))
        .withReadTimeout(Duration(5, TimeUnit.SECONDS))
        .newSyncClient(PUBLIC_REST_SERVICE)
      val resp = client.sendSafe(Http.GET("/get"))
      resp.status.isSuccessful
    } catch {
      case _: Exception => false
    }
  }

  override protected def design: Design = {
    Design.newDesign
      .bind[SyncClient]
      .toInstance(
        Http.client.withJSONEncoding
          .newSyncClient(PUBLIC_REST_SERVICE)
      )
  }

  case class Person(id: Int, name: String)

  val p     = Person(1, "leo")
  val pJson = MessageCodec.of[Person].toJson(p)

  private def check(r: Response): Unit = {
    r.status shouldBe HttpStatus.Ok_200
    val m = MessageCodec.of[Map[String, Any]].fromJson(r.contentString)
    check(m)
  }

  private def check(m: Map[String, Any]): Unit = {
    m("json") shouldBe Map("id" -> 1, "name" -> "leo")
  }

  test("sync client") { (client: SyncClient) =>
    if (!isServiceAvailable) {
      pending(
        s"External service ${PUBLIC_REST_SERVICE} is not available. Use integration tests with local Netty server instead."
      )
    }

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
    if (!isServiceAvailable) {
      pending(
        s"External service ${PUBLIC_REST_SERVICE} is not available. Use integration tests with local Netty server instead."
      )
    }

    test("Handle 5xx retry") {
      flaky {
        Logger("wvlet.airframe.http.HttpClient").suppressWarnings {
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
}
