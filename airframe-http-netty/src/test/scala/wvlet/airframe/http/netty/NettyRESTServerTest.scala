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
package wvlet.airframe.http.netty

import java.lang.reflect.InvocationTargetException
import wvlet.airframe.Design
import wvlet.airframe.codec.{JSONCodec, MessageCodec}
import wvlet.airframe.control.Control
import wvlet.airframe.http.*
import wvlet.airframe.http.client.{AsyncClient, SyncClient}
import wvlet.airframe.msgpack.spi.MessagePack
import wvlet.airspec.AirSpec
import wvlet.airspec.spi.AirSpecContext
import wvlet.log.{LogLevel, LogSupport, Logger}
import wvlet.airframe.rx.Rx

import scala.concurrent.Future

case class RichInfo(version: String, name: String, details: RichNestedInfo)
case class RichNestedInfo(serverType: String)
case class RichRequest(id: Int, name: String)

class MyApi extends LogSupport {
  @Endpoint(path = "/v1/info")
  def getInfo: String = {
    "hello MyApi"
  }

  @Endpoint(path = "/v1/rich_info")
  def getRichInfo: RichInfo = {
    RichInfo("0.1", "MyApi", RichNestedInfo("test-server"))
  }

  @Endpoint(path = "/v1/future")
  def futureString: Rx[String] = {
    Rx.single("hello")
  }

  @Endpoint(path = "/v1/rich_info_future")
  def futureRichInfo: Rx[RichInfo] = {
    Rx.single(getRichInfo)
  }

  // An example to map JSON requests to objects
  @Endpoint(method = HttpMethod.POST, path = "/v1/json_api")
  def jsonApi(request: RichRequest): Rx[String] = {
    Rx.single(request.toString)
  }

  @Endpoint(method = HttpMethod.GET, path = "/v1/json_api")
  def jsonApiForGet(request: RichRequest): Rx[String] = {
    Rx.single(request.toString)
  }

  @Endpoint(path = "/v1/http_header_test")
  def httpHeaderTest(): HttpMessage.Response = {
    Http
      .response(HttpStatus.Ok_200)
      .withContent("Hello")
      .withHeader("Server", "Airframe")
  }

  @Endpoint(method = HttpMethod.POST, path = "/v1/raw_string_arg")
  def rawString(body: String): String = {
    body
  }

  @Endpoint(method = HttpMethod.POST, path = "/v1/json_api_default")
  def jsonApiDefault(request: RichRequest = RichRequest(100, "dummy")): Future[String] = {
    Future.successful(request.toString)
  }

  @Endpoint(path = "/v1/error")
  def throw_ex: String = {
    throw new InvocationTargetException(new IllegalArgumentException("test error"))
  }

//  @Endpoint(path = "/v1/reader")
//  def reader: Reader[Buf] = {
//    val json = MessageCodec.of[RichInfo].toJson(getRichInfo)
//    Reader.fromBuf(Buf.Utf8(json))
//  }
//
//  @Endpoint(path = "/v1/reader-seq")
//  def readerSeq: Reader[RichInfo] = {
//    val r1     = Reader.fromSeq(Seq(getRichInfo))
//    val r2     = Reader.fromSeq(Seq(getRichInfo))
//    val stream = AsyncStream.fromSeq(Seq(r1, r2))
//    Reader.concat(stream)
//  }

  @Endpoint(path = "/v1/delete", method = HttpMethod.DELETE)
  def emptyResponse: Unit = {}

  @Endpoint(path = "/v1/scala-future", method = HttpMethod.GET)
  def scalaFutureResponse: scala.concurrent.Future[String] = {
    scala.concurrent.Future.successful("Hello Scala Future")
  }

  @Endpoint(path = "/v1/scala-future2", method = HttpMethod.GET)
  def scalaFutureResponse2: Future[HttpMessage.Response] = {
    val r = Http.response(HttpStatus.Ok_200, "Hello Scala Future")
    Future.successful(r)
  }

  @Endpoint(path = "/v1/user/:user_id/profile")
  def queryParamTest(user_id: String, session_id: Option[String]): HttpMessage.Response = {
    Http.response().withContent(s"${user_id}:${session_id.getOrElse("unknown")}")
  }

  @Endpoint(method = HttpMethod.POST, path = "/v1/user/:user_id/profile")
  def queryParamPostTest(user_id: String, session_id: Option[String]): HttpMessage.Response = {
    Http.response().withContent(s"${user_id}:${session_id.getOrElse("unknown")}")
  }
}

/**
  */
class NettyRESTServerTest extends AirSpec {

  initDesign {
    _.add(
      Netty.server
        .withRouter(RxRouter.of[MyApi])
        .designWithAsyncClient
    )
  }

  test("support production mode") { (server: HttpServer) =>
    // #432: Just need to check the startup of finagle without MISSING_DEPENDENCY error
  }

  test("async responses") { (client: AsyncClient) =>
    test("rx response") {
      val f1 = client.send(Http.GET("/v1/info"))
      val f2 = client.send(Http.GET("/v1/rich_info"))
      Rx.zip(f1, f2).map { case (r1, r2) =>
        r1.contentString shouldBe "hello MyApi"
        r2.contentString shouldBe """{"version":"0.1","name":"MyApi","details":{"serverType":"test-server"}}"""
      }
    }

    test("multiple rx responses") {
      // making many requests
      val rxList = (0 until 5).map { x =>
        client.send(Http.GET("/v1/rich_info")).map { response => response.contentString }
      }
      Rx.zip(rxList).map { result =>
        result.size shouldBe 5
        result.forall(_ == """{"version":"0.1","name":"MyApi","details":{"serverType":"test-server"}}""") shouldBe true
      }
    }
  }

  test("test various responses") { (client: AsyncClient) =>
    test("support JSON response") {
      // JSON response
      client.send(Http.GET("/v1/rich_info_future")).map { response =>
        response.contentString shouldBe """{"version":"0.1","name":"MyApi","details":{"serverType":"test-server"}}"""
      }
    }

    test("support JSON POST request") {
      val request = Http.POST("/v1/json_api").withJson("""{"id":10, "name":"leo"}""")
      client.send(request).map {
        _.contentString shouldBe """RichRequest(10,leo)"""
      }
    }

    test("return a response header except for Content-Type") {
      val request = Http.GET("/v1/http_header_test")
      client.send(request).map { ret =>
        ret.header.getOrElse("Server", "") shouldBe "Airframe"
        ret.contentString shouldBe """Hello"""
      }
    }

    test("JSON POST request with explicit JSON content type") {
      val request = Http.POST("/v1/json_api").withJson("""{"id":10, "name":"leo"}""")
      client.send(request).map {
        _.contentString shouldBe """RichRequest(10,leo)"""
      }
    }

    test("test parameter mappings") {
      test("Use the default argument") {
        client.send(Http.POST("/v1/json_api_default")).map {
          _.contentString shouldBe """RichRequest(100,dummy)"""
        }
      }

      test("GET request with query parameters") {
        client.send(Http.GET("/v1/json_api?id=10&name=leo")).map {
          _.contentString shouldBe """RichRequest(10,leo)"""
        }
      }

      // JSON requests with POST
      test("JSON POST request with explicit JSON content type") {
        val request = Http.POST("/v1/json_api").withJson("""{"id":10, "name":"leo"}""")
        client.send(request).map(_.contentString shouldBe """RichRequest(10,leo)""")
      }
    }

    test("test error response") {
      warn("Exception response test")
      // Receive the raw error response
      client.withConfig(_.noRetry).sendSafe(Http.GET("/v1/error")).map { ret =>
        ret.statusCode shouldBe 500
        ret.header.get(HttpHeader.xAirframeRPCStatus) shouldBe defined
      }
    }

    test("MsgPack response") {
      test("MessagePack request") {
        val msgpack = JSONCodec.toMsgPack("""{"id":10, "name":"leo"}""")
        val request = Http.POST("/v1/json_api").withMsgPack(msgpack)
        client.send(request).map(_.contentString shouldBe """RichRequest(10,leo)""")
      }
//
//      // Receive MessagePack
//      {
//        val request = Request("/v1/raw_string_arg")
//        request.method = Method.Post
//        request.contentType = "application/x-msgpack"
//        val msgpack = MessagePack.newBufferPacker.packString("1.0").toByteArray
//        request.content = ByteArray.Owned(msgpack)
//        val response = Await.result(client.send(request))
//        response.contentString shouldBe "1.0"
//        response.statusCode shouldBe HttpStatus.Ok_200.code
//      }
//    }
//
//    test("Raw string request") {
//      // Raw string arg
//      val request = Request("/v1/raw_string_arg")
//      request.method = Method.Post
//      request.contentString = "1.0"
//      Await.result(client.send(request).map(_.contentString)) shouldBe "1.0"
//    }
//
//    test("Finagle Reader[Buf] response") {
//      val request = Request("/v1/reader")
//      request.method = Method.Get
//      val json  = Await.result(client.send(request).map(_.contentString))
//      val codec = MessageCodec.of[RichInfo]
//      codec.unpackJson(json) shouldBe Some(RichInfo("0.1", "MyApi", RichNestedInfo("test-server")))
//    }
//
//    val richInfo = RichInfo("0.1", "MyApi", RichNestedInfo("test-server"))
//
//    test("convert Reader[X] response to JSON stream") {
//      val request = Request("/v1/reader-seq")
//      request.method = Method.Get
//      val json = Await.result(client.send(request).map(_.contentString))
//      debug(json)
//      val codec = MessageCodec.of[Seq[RichInfo]]
//      codec.fromJson(json) shouldBe Seq(richInfo, richInfo)
//    }
//
//    test("Convert Reader[X] response to MsgPack stream") {
//      val request = Request("/v1/reader-seq")
//      request.method = Method.Get
//      request.accept = "application/x-msgpack"
//      val msgpack = Await.result {
//        client.send(request).map { resp =>
//          val c       = resp.content
//          val msgpack = new Array[Byte](c.length)
//          c.write(msgpack, 0)
//          msgpack
//        }
//      }
//      val codec = MessageCodec.of[RichInfo]
//
//      Control.withResource(MessagePack.newUnpacker(msgpack)) { unpacker =>
//        while (unpacker.hasNext) {
//          val v = unpacker.unpackValue
//          codec.fromMsgPack(v.toMsgpack) shouldBe richInfo
//        }
//      }
//    }
//
//    test("return 204 for Unit response") {
//      val result = Await.result(client.send(Request(Method.Delete, "/v1/delete")))
//      result.statusCode shouldBe HttpStatus.NoContent_204.code
//    }
//
//    test("support scala.concurrent.Future[X]") {
//      val result = Await.result(client.send(Request(Method.Get, "/v1/scala-future")))
//      result.statusCode shouldBe HttpStatus.Ok_200.code
//      result.contentString shouldBe "Hello Scala Future"
//    }
//
//    test("support scala.concurrent.Future[Response]") {
//      val result = Await.result(client.send(Request(Method.Get, "/v1/scala-future2")))
//      result.statusCode shouldBe HttpStatus.Ok_200.code
//      result.contentString shouldBe "Hello Scala Future"
//    }
//
//    test("support query parameter mapping") {
//      val result = Await.result(client.send(Request(Method.Get, "/v1/user/1/profile?session_id=xyz")))
//      result.statusCode shouldBe HttpStatus.Ok_200.code
//      result.contentString shouldBe "1:xyz"
//    }
//
//    test("support missing query parameter mapping") {
//      val result = Await.result(client.send(Request(Method.Get, "/v1/user/1/profile")))
//      result.statusCode shouldBe HttpStatus.Ok_200.code
//      result.contentString shouldBe "1:unknown"
//    }
//
//    test("support query parameter mapping for POST") {
//      val r = Request(Method.Post, "/v1/user/1/profile?session_id=xyz")
//      r.contentString = "hello"
//      val result = Await.result(client.send(r))
//      result.statusCode shouldBe HttpStatus.Ok_200.code
//      result.contentString shouldBe "1:xyz"
//    }
//
//    test("support option parameter mapping for POST") {
//      val r = Request(Method.Post, "/v1/user/1/profile")
//      r.contentString = "hello"
//      val result = Await.result(client.send(r))
//      result.statusCode shouldBe HttpStatus.Ok_200.code
//      result.contentString shouldBe "1:hello"
//    }
//
//    test("skip content body mapping for application/octet-stream requests") {
//      val r = Request(Method.Post, "/v1/user/1/profile")
//      r.contentString = "hello" // This content should not be used for RPC binding
//      r.contentType = MediaType.OctetStream
//      val result = Await.result(client.send(r))
//      result.statusCode shouldBe HttpStatus.Ok_200.code
//      result.contentString shouldBe "1:unknown"
    }
  }
}
