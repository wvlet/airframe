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
import wvlet.airframe.control.{CircuitBreaker, CircuitBreakerOpenException}
import wvlet.airframe.http.{Http, HttpClientException, HttpClientMaxRetryException, HttpStatus, ServerAddress}
import wvlet.airframe.json.JSON
import wvlet.airspec.AirSpec

class JavaHttpSyncClientTest extends AirSpec {

  // Use a public REST test server
  private val PUBLIC_REST_SERVICE = "https://httpbin.org/"

  override def design: Design =
    Design.newDesign
      .bind[HttpSyncClient].toInstance {
        new JavaHttpSyncClient(ServerAddress(PUBLIC_REST_SERVICE), Http.client.withRetryContext(_.withMaxRetry(1)))
      }

  test("java http sync client") { (client: HttpSyncClient) =>
    test("GET") {
      val resp = client.send(Http.GET("/get?id=1&name=leo"))
      resp.status shouldBe HttpStatus.Ok_200
      resp.isContentTypeJson shouldBe true
      val json = JSON.parse(resp.message.toContentString)
      (json / "args" / "id").toStringValue shouldBe "1"
      (json / "args" / "name").toStringValue shouldBe "leo"
    }

    test("POST") {
      val data = """{"id":1,"name":"leo"}"""
      val resp = client.send(Http.POST("/post").withContent(data))
      resp.status shouldBe HttpStatus.Ok_200
      resp.isContentTypeJson shouldBe true
      val json = JSON.parse(resp.message.toContentString)
      json("data").toString shouldBe data
      json("json").toString shouldBe data
    }

    test("404 with HttpClientException") {
      val e = intercept[HttpClientException] {
        client.send(Http.GET("/status/404"))
      }
      e.status shouldBe HttpStatus.NotFound_404
    }

    test("404") {
      val resp = client.sendSafe(Http.GET("/status/404"))
      resp.status shouldBe HttpStatus.NotFound_404
    }

    test("handle max retry") {
      val e = intercept[HttpClientMaxRetryException] {
        client.send(Http.GET("/status/500"))
      }
      e.status.isServerError shouldBe true
    }

    test("handle max retry safely") {
      val lastResp = client.sendSafe(Http.GET("/status/500"))
      lastResp.status.isServerError shouldBe true
    }

    test("gzip encoding") {
      val resp = client.send(Http.GET("/gzip"))
      val m    = MessageCodec.of[Map[String, Any]].fromJson(resp.contentString)
      m("gzipped") shouldBe true
      resp.contentEncoding shouldBe Some("gzip")
    }

    test("deflate encoding") {
      val resp = client.send(Http.GET("/deflate"))
      val m    = MessageCodec.of[Map[String, Any]].fromJson(resp.contentString)
      m("deflated") shouldBe true
      resp.contentEncoding shouldBe Some("deflate")
    }

  }

  test("circuit breaker") {
    val client = new JavaHttpSyncClient(
      ServerAddress(PUBLIC_REST_SERVICE),
      Http.client.withCircuitBreaker(_ => CircuitBreaker.withConsecutiveFailures(1))
    )
    val e = intercept[HttpClientException] {
      client.send(Http.GET("/status/500"))
    }
    e.getCause match {
      case c: CircuitBreakerOpenException =>
      // ok
      case other =>
        fail(s"Unexpected failure: ${e}")
    }
  }

}
