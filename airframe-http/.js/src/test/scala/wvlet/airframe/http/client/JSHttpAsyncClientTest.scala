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
import wvlet.airframe.http._
import wvlet.airframe.json.JSON
import wvlet.airframe.rx.Rx
import wvlet.airspec.AirSpec

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

class JSHttpAsyncClientTest extends AirSpec {
  private implicit val ec: ExecutionContext = defaultExecutionContext

  // Use a public REST test server
  private val PUBLIC_REST_SERVICE = "https://jsonplaceholder.typicode.com/"

  override def design: Design =
    Design.newDesign
      .bind[AsyncClient].toInstance {
        Http.client
          .withBackend(JSHttpClientBackend)
          .newAsyncClient(PUBLIC_REST_SERVICE)
      }

  test("java http sync client") { (client: AsyncClient) =>
    test("GET") {
      flaky {
        client
          .send(Http.GET("/posts/1"))
          .map { resp =>
            resp.status shouldBe HttpStatus.Ok_200
            resp.isContentTypeJson shouldBe true
            val json = JSON.parse(resp.message.toContentString).toJSON
            val m    = MessageCodec.of[Map[String, Any]].fromJson(json)
            m("userId") shouldBe 1
            m("id") shouldBe 1
          }
      }
    }

    test("POST") {
      flaky {
        val data = """{"id":1,"name":"leo"}"""
        client
          .send(Http.POST("/posts").withContent(data))
          .map { resp =>
            resp.status shouldBe HttpStatus.Created_201
            resp.isContentTypeJson shouldBe true
            val json = JSON.parse(resp.message.toContentString).toJSON
            val m    = MessageCodec.of[Map[String, Any]].fromJson(json)
            m("id") shouldBe 101
          }
      }
    }

    test("404") {
      flaky {
        client
          .sendSafe(Http.GET("/status/404"))
          .transform {
            case Success(resp) =>
              resp.status shouldBe HttpStatus.NotFound_404
            case _ =>
              fail(s"Cannot reach here")
          }
      }
    }

    test("404 with HttpClientException") {
      flaky {
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
  }

  test("retry test") { (client: AsyncClient) =>
    test("handle max retry") {
      flaky {
        client
          .withRetryContext(_.withMaxRetry(1))
          .withClientFilter(new RxHttpFilter {
            override def apply(request: HttpMessage.Request, next: RxHttpEndpoint): Rx[HttpMessage.Response] = {
              // Return a dummy response
              Rx.single(Http.response(HttpStatus.InternalServerError_500))
            }
          })
          .send(Http.GET("/status/500"))
          .transform {
            case Failure(e: HttpClientMaxRetryException) =>
              e.status.isServerError shouldBe true
            case _ =>
              fail("should not reach here")
          }
      }
    }
  }

  test("circuit breaker test") { (client: AsyncClient) =>
    flaky {
      client
        .withCircuitBreaker(_ => CircuitBreaker.withConsecutiveFailures(1))
        .withClientFilter(new RxHttpFilter {
          override def apply(request: HttpMessage.Request, next: RxHttpEndpoint): Rx[HttpMessage.Response] = {
            // Return a dummy response
            Rx.single(Http.response(HttpStatus.InternalServerError_500))
          }
        })
        .send(Http.GET("/status/500"))
        .transform {
          case Failure(e) =>
            e.getCause match {
              case c: CircuitBreakerOpenException =>
                // ok
                Success(())
              case other =>
                fail(s"Unexpected exception: ${other}")
            }
          case other =>
            fail(s"Unexpected response: ${other}")
        }
    }
  }
}
