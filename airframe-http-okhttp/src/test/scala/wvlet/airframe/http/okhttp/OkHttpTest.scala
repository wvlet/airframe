package wvlet.airframe.http.okhttp

import java.nio.charset.StandardCharsets

import okhttp3.internal.http.HttpMethod
import okhttp3.{Protocol, Request, RequestBody, Response, ResponseBody}
import wvlet.airframe.http.HttpStatus
import wvlet.airspec.AirSpec

class OkHttpTest extends AirSpec {

  def `provide facade of http requests`: Unit = {
    val body = RequestBody.create(ContentTypeJson, "hello okhttp")
    Seq(
      new Request.Builder().get(),
      new Request.Builder().post(body),
      new Request.Builder().delete(body),
      new Request.Builder().put(body),
      new Request.Builder().patch(body),
      new Request.Builder().head(),
      new Request.Builder().method("OPTIONS", body),
      new Request.Builder().method("TRACE", body)
    ).foreach { builder =>
      val req = builder.url("http://localhost/hello").build()

      val r = req.toHttpRequest
      r.method shouldBe toHttpMethod(req.method())
      r.path shouldBe "/hello"
      r.query shouldBe Map.empty
      if (HttpMethod.permitsRequestBody(req.method())) {
        r.contentString shouldBe "hello okhttp"
        r.contentBytes shouldBe "hello okhttp".getBytes(StandardCharsets.UTF_8)
        r.contentType shouldBe Some("application/json;charset=utf-8")
      } else {
        r.contentString shouldBe ""
        r.contentBytes shouldBe Array.empty[Byte]
        r.contentType shouldBe empty
      }
      r.toRaw shouldBeTheSameInstanceAs req
    }
  }

  def `provide facade of http responses`: Unit = {
    val res = new Response.Builder()
      .code(403)
      .body(ResponseBody.create(ContentTypeJson, "hello world"))
      .request(new Request.Builder().url("http://localhost/").get().build())
      .protocol(Protocol.HTTP_1_1)
      .message("message")
      .build()

    val r = res.toHttpResponse
    r.status shouldBe HttpStatus.Forbidden_403
    r.statusCode shouldBe 403
    r.contentType shouldBe Some("application/json;charset=utf-8")
    r.contentBytes shouldBe "hello world".getBytes(StandardCharsets.UTF_8)
    r.toRaw shouldBeTheSameInstanceAs res
  }

}
