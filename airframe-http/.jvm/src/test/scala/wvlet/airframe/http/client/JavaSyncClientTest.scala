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
import wvlet.airframe.http.{Http, HttpClientException, HttpClientMaxRetryException, HttpStatus}
import wvlet.airframe.json.{JSON, Json}
import wvlet.airspec.AirSpec

class JavaSyncClientTest extends AirSpec {

  // Use a public REST test server
  private val PUBLIC_REST_SERVICE = "https://httpbin.org/"

  override def design: Design =
    Design.newDesign
      .bind[SyncClient].toInstance {
        Http.client
          .withBackend(JavaHttpClientBackend)
          .withJSONEncoding
          .newSyncClient(PUBLIC_REST_SERVICE)
      }

  test("java http sync client") { (client: SyncClient) =>
    test("GET") {
      val resp = client.send(Http.GET("/get?id=1&name=leo"))
      resp.status shouldBe HttpStatus.Ok_200
      resp.isContentTypeJson shouldBe true
      resp.getHeader(":status") shouldBe None // Pseudo headers should be excluded
      val json = JSON.parse(resp.message.toContentString).toJSON
      val m    = MessageCodec.of[Map[String, Any]].fromJson(json)
      m("args") shouldBe Map("id" -> "1", "name" -> "leo")
    }

    test("readAs") {
      val m = client.readAs[Map[String, Any]](Http.GET("/get?id=1&name=leo"))
      m("args") shouldBe Map("id" -> "1", "name" -> "leo")
    }

    test("readAs with HttpClientException") {
      val e = intercept[HttpClientException] {
        client.readAs[Map[String, Any]](Http.GET("/status/404"))
      }
      e.status shouldBe HttpStatus.NotFound_404
    }

    test("POST") {
      val data = """{"id":1,"name":"leo"}"""
      val resp = client.send(Http.POST("/post").withContent(data))
      resp.status shouldBe HttpStatus.Ok_200
      resp.isContentTypeJson shouldBe true
      val json = JSON.parse(resp.message.toContentString).toJSON
      val m    = MessageCodec.of[Map[String, Any]].fromJson(json)
      m("data").toString shouldBe data
      m("json") shouldBe Map("id" -> 1, "name" -> "leo")
    }

    test("call") {
      val data = """{"id":1,"name":"leo"}"""
      val m    = client.call[Json, Map[String, Any]](Http.POST("/post"), data)
      m("data") shouldBe data
      m("json") shouldBe Map("id" -> 1, "name" -> "leo")
    }

    test("call with HttpClientException") {
      val data = """{"id":1,"name":"leo"}"""
      val e = intercept[HttpClientException] {
        client.call[Json, Map[String, Any]](Http.POST("/status/404"), data)
      }
      e.status shouldBe HttpStatus.NotFound_404
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

  test("retry test") { (client: SyncClient) =>
    test("handle max retry") {
      val e = intercept[HttpClientMaxRetryException] {
        client.withRetryContext(_.withMaxRetry(1)).send(Http.GET("/status/500"))
      }
      e.status.isServerError shouldBe true
    }

    test("handle max retry safely") {
      val lastResp = client.withRetryContext(_.withMaxRetry(1)).sendSafe(Http.GET("/status/500"))
      lastResp.status.isServerError shouldBe true
    }
  }

  test("circuit breaker") { (client: SyncClient) =>
    val e = intercept[HttpClientException] {
      client
        .withCircuitBreaker(_ => CircuitBreaker.withConsecutiveFailures(1))
        .send(Http.GET("/status/500"))
    }
    e.getCause match {
      case c: CircuitBreakerOpenException =>
      // ok
      case other =>
        fail(s"Unexpected failure: ${e}")
    }
  }
}
