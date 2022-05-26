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
import wvlet.airframe.http._
import wvlet.airframe.json.JSON
import wvlet.airspec.AirSpec

import scala.util.{Failure, Success}

class JavaHttpAsyncClientTest extends AirSpec {

  private implicit val ec = defaultExecutionContext

  // Use a public REST test server
  private val PUBLIC_REST_SERVICE = "https://httpbin.org/"

  override def design: Design =
    Design.newDesign
      .bind[HttpAsyncClient].toInstance {
        new JavaHttpSyncClient(
          ServerAddress(PUBLIC_REST_SERVICE),
          Http.client.withRetryContext(_.withMaxRetry(1))
        ).toAsyncClient
      }

  test("java http sync client") { (client: HttpAsyncClient) =>
    test("GET") {
      client
        .send(Http.GET("/get?id=1&name=leo"))
        .map { resp =>
          resp.status shouldBe HttpStatus.Ok_200
          resp.isContentTypeJson shouldBe true
          val json = JSON.parse(resp.message.toContentString)
          (json / "args" / "id").toStringValue shouldBe "1"
          (json / "args" / "name").toStringValue shouldBe "leo"
        }
    }

    test("POST") {
      val data = """{"id":1,"name":"leo"}"""
      client.send(Http.POST("/post").withContent(data)).map { resp =>
        resp.status shouldBe HttpStatus.Ok_200
        resp.isContentTypeJson shouldBe true
        val json = JSON.parse(resp.message.toContentString)
        json("data").toString shouldBe data
        json("json").toString shouldBe data
      }
    }

    test("404 with HttpClientException") {
      client.send(Http.GET("/status/404")).transform { ret =>
        ret match {
          case Success(_) =>
            Failure(new IllegalStateException("should not reach here"))
          case Failure(e: HttpClientException) =>
            e.status shouldBe HttpStatus.NotFound_404
            Success(())
          case Failure(e: Throwable) =>
            ret
        }
      }
    }

    test("handle max retry") {
      client.send(Http.GET("/status/500")).transform { ret =>
        ret match {
          case Success(_) =>
            Failure(new IllegalStateException("should not reach here"))
          case Failure(e: HttpClientMaxRetryException) =>
            e.status.isServerError shouldBe true
            Success(())
          case _ =>
            ret
        }
      }
    }
  }
}
