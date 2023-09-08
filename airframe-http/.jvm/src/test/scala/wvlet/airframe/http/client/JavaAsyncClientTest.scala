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

import wvlet.airframe.codec.MessageCodec
import wvlet.airframe.http.*
import wvlet.airframe.json.JSON
import wvlet.airframe.{Design, newDesign}
import wvlet.airspec.AirSpec

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

object JavaAsyncClientTest extends AirSpec {

  case class Person(id: Int, name: String)
  private val p     = Person(1, "leo")
  private val pJson = """{"id":1,"name":"leo"}"""

  private implicit val ec: ExecutionContext = defaultExecutionContext

  // Use a public REST test server
  private val PUBLIC_REST_SERVICE = "https://httpbin.org/"

  override def design: Design =
    Design.newDesign
      .bind[AsyncClient].toInstance {
        Http.client
          .withBackend(JavaHttpClientBackend)
          .withJSONEncoding
          .newAsyncClient(PUBLIC_REST_SERVICE)
      }

  test("java http sync client") { (client: AsyncClient) =>
    test("GET") {
      client
        .send(Http.GET("/get?id=1&name=leo"))
        .toRx
        .map { resp =>
          resp.status shouldBe HttpStatus.Ok_200
          resp.isContentTypeJson shouldBe true
          val json = JSON.parse(resp.message.toContentString).toJSON
          val m    = MessageCodec.of[Map[String, Any]].fromJson(json)
          m("args") shouldBe Map("id" -> "1", "name" -> "leo")
        }
    }

    test("call with GET") {
      // .
      client
        .call[Person, Map[String, Any]](Http.GET("/get"), p)
        .toRx
        .map { m =>
          m("args") shouldBe Map("id" -> "1", "name" -> "leo")
        }
    }

    test("call with GET") {
      // .
      client
        .call[Person, Map[String, Any]](Http.GET("/get"), p)
        .toRx
        .map { m =>
          m("args") shouldBe Map("id" -> "1", "name" -> "leo")
        }
    }

    test("POST") {
      val data = """{"id":1,"name":"leo"}"""
      client
        .send(Http.POST("/post").withContent(data))
        .toRx
        .map { resp =>
          resp.status shouldBe HttpStatus.Ok_200
          resp.isContentTypeJson shouldBe true
          val json = JSON.parse(resp.message.toContentString).toJSON
          val m    = MessageCodec.of[Map[String, Any]].fromJson(json)
          m("data") shouldBe data
          m("json") shouldBe Map("id" -> 1, "name" -> "leo")
        }
    }

    test("call with POST") {
      client
        .call[Person, Map[String, Any]](Http.POST("/post"), p)
        .toRx
        .map { m =>
          m("data") shouldBe pJson
          m("json") shouldBe Map("id" -> 1, "name" -> "leo")
        }
    }

    test("404") {
      client
        .sendSafe(Http.GET("/status/404"))
        .toRx
        .transform {
          case Success(resp) =>
            resp.status shouldBe HttpStatus.NotFound_404
          case _ =>
            fail(s"Cannot reach here")
        }
    }

    test("404 with HttpClientException") {
      client
        .send(Http.GET("/status/404"))
        .toRx
        .transform {
          case Failure(e: HttpClientException) =>
            e.status shouldBe HttpStatus.NotFound_404
            Success(())
          case other =>
            throw new IllegalStateException(s"should not reach here: ${other}")
        }
    }
  }

  test("retry test") { (client: AsyncClient) =>
    test("handle max retry") {
      client
        .withRetryContext(_.withMaxRetry(1))
        .send(Http.GET("/status/500"))
        .toRx
        .transform {
          case Failure(e: HttpClientMaxRetryException) =>
            e.status.isServerError shouldBe true
            Success(())
          case _ =>
            throw new IllegalStateException("should not reach here")
        }
    }
  }
}
