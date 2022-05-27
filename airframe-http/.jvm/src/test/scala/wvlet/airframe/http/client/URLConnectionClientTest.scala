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
import wvlet.airframe.control.Retry.MaxRetryException
import wvlet.airframe.http.HttpMessage.Response
import wvlet.airframe.http.{Http, HttpStatus}
import wvlet.airspec.AirSpec
import wvlet.log.Logger

/**
  */
object URLConnectionClientTest extends AirSpec {

  // Use a public REST test server
  private val PUBLIC_REST_SERVICE = "https://httpbin.org/"

  override protected def design: Design = {
    Design.newDesign
      .bind[SyncClient]
      .toInstance(
        Http.client
          .withBackend(URLConnectionClientBackend)
          .withRetryContext(_.withMaxRetry(1))
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
        client.sendSafe(Http.GET("/user-agent"), _.withUserAgent("airframe-http"))
      MessageCodec.of[Map[String, Any]].fromJson(resp.contentString).get("user-agent") shouldBe Some("airframe-http")
    }

    test("delete") {
      val resp = client.sendSafe(Http.DELETE("/delete"))
      resp.status shouldBe HttpStatus.Ok_200
    }

//    test("patch") {
//      ignore("URLConnection doesn't support patch, so we need to use POST endpoint + X-HTTP-Method-Override header")
//      check(client.patchRaw[Person]("/post", p))
//      check(client.patchOps[Person, Map[String, Any]]("/post", p))
//    }
//
//    test("xxxRaw") {
//      check(client.postRaw[Person]("/post", p))
//      check(client.putRaw[Person]("/put", p))
//    }
//
//    test("getOps") {
//      val m = client.getOps[Person, Map[String, Any]]("/get", p)
//      m("args") shouldBe Map("id" -> "1", "name" -> "leo")
//    }
//
//    test("xxxOps") {
//      check(client.postOps[Person, Map[String, Any]]("/post", p))
//      check(client.putOps[Person, Map[String, Any]]("/put", p))
//    }

    test("Handle 404 (Not Found)") {
      val errorResp = client.sendSafe(Http.GET("/status/404"))
      debug(errorResp)
      errorResp.status shouldBe HttpStatus.NotFound_404
      debug(errorResp.contentString)
    }

    test("Handle 5xx retry") {
      Logger("wvlet.airframe.http.HttpClient").suppressWarnings {
        intercept[MaxRetryException] {
          client.send(Http.GET("/status/500"))
        }
      }
    }
  }
}
