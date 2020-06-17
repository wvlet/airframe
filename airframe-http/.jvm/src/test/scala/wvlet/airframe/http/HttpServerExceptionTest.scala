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
import wvlet.airframe.codec.{MessageCodec, MessageCodecFactory, MessageContext}
import wvlet.airframe.msgpack.spi.{Packer, Unpacker}
import wvlet.airframe.surface.Surface
import wvlet.airspec.AirSpec

/**
  */
object HttpServerExceptionTest extends AirSpec {

  test("custom error responses") {
    val json = """{"message":"illegal argument"}"""

    val ex = Http.serverException(HttpStatus.BadRequest_400)
    val e =
      ex.withHeader("X-MyApp-ErrorCode", "ERR")
        .withJson(json)

    e.getStackTrace shouldBe ex.getStackTrace

    e.getHeader("X-MyApp-ErrorCode") shouldBe Some("ERR")
    e.contentString shouldBe json
    e.isContentTypeJson shouldBe true
    e.status shouldBe HttpStatus.BadRequest_400
    e.statusCode shouldBe 400
  }

  test("redirect") {
    val redirectUrl = "https://mydomain.org/error_page"
    val e           = Http.redirectException(redirectUrl)

    e.status shouldBe HttpStatus.Found_302
    e.getHeader("Location") shouldBe Some(redirectUrl)
  }

  case class ErrorResponse(code: Int, message: String)

  private val customCodec = MessageCodecFactory.newFactory(
    Map(
      Surface.of[ErrorResponse] ->
        new MessageCodec[ErrorResponse] {
          override def pack(
              p: Packer,
              v: ErrorResponse
          ): Unit = {
            p.packArrayHeader(2)
            p.packInt(v.code)
            p.packString(v.message)
          }
          override def unpack(
              u: Unpacker,
              v: MessageContext
          ): Unit = {
            u.unpackArrayHeader
            val code = u.unpackInt
            val m    = u.unpackString
            v.setObject(ErrorResponse(code, m))
          }
        }
    )
  )

  test("custom message") {
    val err = ErrorResponse(400, "invalid response")

    test("json error response") {
      val e =
        Http
          .serverException(HttpStatus.BadRequest_400)
          .withJsonOf(err)

      e.contentString shouldBe """{"code":400,"message":"invalid response"}"""
      e.isContentTypeJson shouldBe true
    }

    test("msgpack error response") {
      val e =
        Http
          .serverException(HttpStatus.BadRequest_400)
          .withMsgPackOf(err)

      val msgpack = e.contentBytes
      MessageCodec.of[ErrorResponse].fromMsgPack(msgpack) shouldBe err
      e.isContentTypeMsgPack shouldBe true
    }

    test("custom codec") {
      // JSON with custom codec
      val e =
        Http
          .serverException(HttpStatus.BadRequest_400)
          .withJsonOf(err, customCodec)
      e.contentString shouldBe """[400,"invalid response"]"""
      e.isContentTypeJson shouldBe true

      // MsgPack with custom codec
      val em = e.withMsgPackOf(err, customCodec)
      customCodec.of[ErrorResponse].fromMsgPack(em.contentBytes) shouldBe err
      em.isContentTypeMsgPack shouldBe true
    }
  }

  test("object error response") {
    val req = Http.request("/v1/info")

    test("json") {
      val e =
        Http.serverException(req, HttpStatus.BadRequest_400, ErrorResponse(100, "invalid input"))
      e.status shouldBe HttpStatus.BadRequest_400
      MessageCodec.of[ErrorResponse].fromJson(e.contentString) shouldBe ErrorResponse(100, "invalid input")
    }

    test("msgpack") {
      val e =
        Http.serverException(req.withAcceptMsgPack, HttpStatus.BadRequest_400, ErrorResponse(100, "invalid input"))
      e.status shouldBe HttpStatus.BadRequest_400
      MessageCodec.of[ErrorResponse].fromMsgPack(e.contentBytes) shouldBe ErrorResponse(100, "invalid input")
    }

    test("json custom codec") {
      val e =
        Http.serverException(req, HttpStatus.BadRequest_400, ErrorResponse(100, "invalid input"), customCodec)
      e.status shouldBe HttpStatus.BadRequest_400
      e.contentString shouldBe """[100,"invalid input"]"""
      MessageCodec.of[ErrorResponse].fromJson(e.contentString) shouldBe ErrorResponse(100, "invalid input")
    }

  }
}
