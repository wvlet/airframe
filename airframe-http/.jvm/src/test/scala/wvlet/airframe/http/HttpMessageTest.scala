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
package wvlet.airframe.http
import wvlet.airspec.AirSpec

/**
  *
  */
class HttpMessageTest extends AirSpec {

  test("request builder shortcut") {
    val r1 = Http.GET("/v1/info")
    r1.method shouldBe HttpMethod.GET
    r1.path shouldBe "/v1/info"

    val r2 = Http.POST("/v1/books")
    r2.method shouldBe HttpMethod.POST
    r2.path shouldBe "/v1/books"

    val r3 = Http.PUT("/v1/books/1")
    r3.method shouldBe HttpMethod.PUT
    r3.path shouldBe "/v1/books/1"

    val r4 = Http.DELETE("/v1/books/1")
    r4.method shouldBe HttpMethod.DELETE
    r4.path shouldBe "/v1/books/1"

    val r5 = Http.PATCH("/v1/books/1")
    r5.method shouldBe HttpMethod.PATCH
    r5.path shouldBe "/v1/books/1"
  }

  test("create new requests") {
    val r = Http
      .GET("/v1/info")
      .withAllow("all")
      .withAccept("application/json")
      .withHeader("X-MyApp-Version", "1.0")
      .withExpires("xxx")
      .withAuthorization("xxx-yyy")
      .withCacheControl("zzzzz")
      .withContent("""{"message":"Hello Airframe!"}""")
      .withContentTypeJson
      .withDate("2020-01-23")
      .withHost("myserver.org")
      .withLastModified("Wed, 21 Oct 2015 07:28:00 GMT")
      .withUserAgent("my-client 1.0")
      .addHeader(HttpHeader.UserAgent, "my-browser")
      .withXForwardedFor("123.45.678.9")
      .withXForwardedProto("https")

    val s = r.toString
    s.contains("/v1/info") shouldBe true
    debug(s)

    r.method shouldBe HttpMethod.GET
    r.getHeader("unknown-key") shouldBe empty
    r.getAllHeader("unknown-key") shouldBe Seq.empty

    r.allow shouldBe Some("all")
    r.accept shouldBe Seq("application/json")
    r.acceptsJson shouldBe true
    r.acceptsMsgPack shouldBe false
    r.getHeader("X-MyApp-Version") shouldBe Some("1.0")
    r.expires shouldBe Some("xxx")
    r.authorization shouldBe Some("xxx-yyy")
    r.cacheControl shouldBe Some("zzzzz")
    r.contentString shouldBe """{"message":"Hello Airframe!"}"""
    r.date shouldBe Some("2020-01-23")
    r.host shouldBe Some("myserver.org")
    r.lastModified shouldBe Some("Wed, 21 Oct 2015 07:28:00 GMT")
    r.userAgent shouldBe Some("my-client 1.0")
    r.getAllHeader(HttpHeader.UserAgent) shouldBe Seq("my-client 1.0", "my-browser")
    r.xForwardedFor shouldBe Some("123.45.678.9")
    r.xForwardedProto shouldBe Some("https")
  }

  test("create new response") {
    val r = Http
      .response()
      .withAllow("all")
      .withAccept("application/json")
      .withHeader("X-MyApp-Version", "1.0")
      .withExpires("xxx")
      .withAuthorization("xxx-yyy")
      .withCacheControl("zzzzz")
      .withContent("""{"message":"Hello Airframe!"}""")
      .withContentTypeJson
      .withDate("2020-01-23")
      .withHost("myserver.org")
      .withLastModified("Wed, 21 Oct 2015 07:28:00 GMT")
      .withUserAgent("my-client 1.0")
      .addHeader(HttpHeader.UserAgent, "my-browser")
      .withXForwardedFor("123.45.678.9")
      .withXForwardedProto("https")

    val s = r.toString
    s.contains("200") shouldBe true
    debug(s)

    r.getHeader("unknown-key") shouldBe empty
    r.getAllHeader("unknown-key") shouldBe Seq.empty

    r.allow shouldBe Some("all")
    r.accept shouldBe Seq("application/json")
    r.acceptsJson shouldBe true
    r.acceptsMsgPack shouldBe false
    r.getHeader("X-MyApp-Version") shouldBe Some("1.0")
    r.expires shouldBe Some("xxx")
    r.authorization shouldBe Some("xxx-yyy")
    r.cacheControl shouldBe Some("zzzzz")
    r.contentString shouldBe """{"message":"Hello Airframe!"}"""
    r.date shouldBe Some("2020-01-23")
    r.host shouldBe Some("myserver.org")
    r.lastModified shouldBe Some("Wed, 21 Oct 2015 07:28:00 GMT")
    r.userAgent shouldBe Some("my-client 1.0")
    r.getAllHeader(HttpHeader.UserAgent) shouldBe Seq("my-client 1.0", "my-browser")
    r.xForwardedFor shouldBe Some("123.45.678.9")
    r.xForwardedProto shouldBe Some("https")
  }

  test("create json responses") {
    val json = """{"id":1}"""
    val r    = Http.response(HttpStatus.Ok_200).withJson(json)
    r.contentString shouldBe json
    r.contentType.map(_.startsWith("application/json"))
  }

  test("msgpack request") {
    val r = Http.request("/v1/info").withAcceptMsgPack

    r.accept shouldBe Seq("application/x-msgpack")
    r.acceptsMsgPack shouldBe true
    r.acceptsJson shouldBe false
  }
}
