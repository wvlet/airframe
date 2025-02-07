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
package wvlet.airframe.test
import wvlet.airframe.Design
import wvlet.airframe.codec.MessageCodec
import wvlet.airframe.http.{Http, HttpClientException, HttpMessage, HttpStatus, ServerSentEvent, ServerSentEventHandler}
import wvlet.airframe.http.client.{AsyncClient, JSHttpClientBackend}
import wvlet.airframe.json.JSON
import wvlet.airframe.rx.RxQueue
import wvlet.airspec.AirSpec

import scala.util.{Failure, Success}

class FetchTest extends AirSpec:
  // Use a public REST test server
  private val PUBLIC_REST_SERVICE = "https://jsonplaceholder.typicode.com/"

  override def design: Design =
    Design.newDesign
      .bind[AsyncClient].toInstance {
        Http.client
          .withBackend(JSHttpClientBackend)
          .withFetchAPI
          .newAsyncClient(PUBLIC_REST_SERVICE)
      }

  test("js http async client") { (client: AsyncClient) =>
    test("GET") {
      client
        .send(Http.GET("/posts/1"))
        .map { resp =>
          resp.status shouldBe HttpStatus.Ok_200
          resp.isContentTypeJson shouldBe true
          val json = JSON.parse(resp.message.toContentString).toJSON
          debug(json)
          val m = MessageCodec.of[Map[String, Any]].fromJson(json)
          m("userId") shouldBe 1d
          m("id") shouldBe 1
        }
    }

    test("POST") {
      val data = """{"id":1,"name":"leo"}"""
      client
        .send(Http.POST("/posts").withContent(data))
        .map { resp =>
          resp.status shouldBe HttpStatus.Created_201
          resp.isContentTypeJson shouldBe true
          val json = JSON.parse(resp.message.toContentString).toJSON
          debug(json)
          val m = MessageCodec.of[Map[String, Any]].fromJson(json)
          m("id") shouldBe 101
        }
    }

    test("404") {
      client
        .sendSafe(Http.GET("/status/404"))
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
        .transform {
          case Failure(e: HttpClientException) =>
            e.status shouldBe HttpStatus.NotFound_404
          case _ =>
            fail(s"should not reach here")
        }
    }
  }
