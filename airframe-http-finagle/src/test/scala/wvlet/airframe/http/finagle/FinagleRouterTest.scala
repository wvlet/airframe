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
package wvlet.airframe.http.finagle

import java.lang.reflect.InvocationTargetException

import com.twitter.concurrent.AsyncStream
import com.twitter.finagle.http.{Method, Request, Response}
import com.twitter.io.Buf.ByteArray
import com.twitter.io.{Buf, Reader}
import com.twitter.util.{Await, Future}
import wvlet.airframe.Design
import wvlet.airframe.codec.{JSONCodec, MessageCodec}
import wvlet.airframe.control.Control
import wvlet.airframe.http._
import wvlet.airframe.msgpack.spi.MessagePack
import wvlet.airspec.AirSpec
import wvlet.airspec.spi.AirSpecContext
import wvlet.log.{LogLevel, LogSupport, Logger}

case class RichInfo(version: String, name: String, details: RichNestedInfo)
case class RichNestedInfo(serverType: String)
case class RichRequest(id: Int, name: String)

trait MyApi extends LogSupport {
  @Endpoint(path = "/v1/info")
  def getInfo: String = {
    "hello MyApi"
  }

  @Endpoint(path = "/v1/rich_info")
  def getRichInfo: RichInfo = {
    RichInfo("0.1", "MyApi", RichNestedInfo("test-server"))
  }

  @Endpoint(path = "/v1/future")
  def futureString: Future[String] = {
    Future.value("hello")
  }

  @Endpoint(path = "/v1/rich_info_future")
  def futureRichInfo: Future[RichInfo] = {
    Future.value(getRichInfo)
  }

  // An example to map JSON requests to objects
  @Endpoint(method = HttpMethod.POST, path = "/v1/json_api")
  def jsonApi(request: RichRequest): Future[String] = {
    Future.value(request.toString)
  }

  @Endpoint(method = HttpMethod.GET, path = "/v1/json_api")
  def jsonApiForGet(request: RichRequest): Future[String] = {
    Future.value(request.toString)
  }

  @Endpoint(method = HttpMethod.POST, path = "/v1/raw_string_arg")
  def rawString(body: String): String = {
    body
  }

  @Endpoint(method = HttpMethod.POST, path = "/v1/json_api_default")
  def jsonApiDefault(request: RichRequest = RichRequest(100, "dummy")): Future[String] = {
    Future.value(request.toString)
  }

  @Endpoint(path = "/v1/error")
  def throw_ex: String = {
    throw new InvocationTargetException(new IllegalArgumentException("test error"))
  }

  @Endpoint(path = "/v1/reader")
  def reader: Reader[Buf] = {
    val json = MessageCodec.of[RichInfo].toJson(getRichInfo)
    Reader.fromBuf(Buf.Utf8(json))
  }

  @Endpoint(path = "/v1/reader-seq")
  def readerSeq: Reader[RichInfo] = {
    val r1     = Reader.fromSeq(Seq(getRichInfo))
    val r2     = Reader.fromSeq(Seq(getRichInfo))
    val stream = AsyncStream.fromSeq(Seq(r1, r2))
    Reader.concat(stream)
  }

  @Endpoint(path = "/v1/delete", method = HttpMethod.DELETE)
  def emptyResponse: Unit = {}

  @Endpoint(path = "/v1/scala-future", method = HttpMethod.GET)
  def scalaFutureResponse: scala.concurrent.Future[String] = {
    scala.concurrent.Future.successful("Hello Scala Future")
  }

  @Endpoint(path = "/v1/scala-future2", method = HttpMethod.GET)
  def scalaFutureResponse2: scala.concurrent.Future[Response] = {
    val r = Response()
    r.contentString = "Hello Scala Future"
    scala.concurrent.Future.successful(r)
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
  *
  */
class FinagleRouterTest extends AirSpec {
  protected override def design: Design = {
    Finagle.server
      .withRouter(Router.add[MyApi])
      .design
      .bind[FinagleClient].toProvider { server: FinagleServer => Finagle.client.noRetry.newClient(server.localAddress) }
  }

  def `support Router.of[X] and Router.add[X]` : Unit = {
    // sanity test
    val r1 = Router.add[MyApi]
    val r2 = Router.of[MyApi]
  }

  def `support production mode`(server: FinagleServer): Unit = {
    // #432: Just need to check the startup of finagle without MISSING_DEPENDENCY error
  }

  def `test various responses`(context: AirSpecContext): Unit = {
    context.test[ResponseTest]
  }

  class ResponseTest(client: FinagleClient) extends AirSpec {
    def `Support future responses`: Unit = {
      val f1 = client.send(Request("/v1/info")).map { response => debug(response.contentString) }
      val f2 = client.send(Request("/v1/rich_info")).map { r => debug(r.contentString) }

      Await.result(f1.join(f2))

      // making many requests
      val futures = (0 until 5).map { x =>
        client.send(Request("/v1/rich_info")).map { response => response.contentString }
      }

      val result = Await.result(Future.collect(futures))
      debug(result.mkString(", "))

      // Future response
      Await.result(client.send(Request("/v1/future")).map { response => response.contentString }) shouldBe "hello"
    }

    def `support JSON response` = {
      // JSON response
      val json = Await.result(client.send(Request("/v1/rich_info_future")).map { response => response.contentString })

      json shouldBe """{"version":"0.1","name":"MyApi","details":{"serverType":"test-server"}}"""
    }

    def `support JSON POST request` {
      val request = Request("/v1/json_api")
      request.method = Method.Post
      request.contentString = """{"id":10, "name":"leo"}"""
      val ret = Await.result(client.send(request).map(_.contentString))
      ret shouldBe """RichRequest(10,leo)"""
    }

    def `JSON POST request with explicit JSON content type` {
      val request = Request("/v1/json_api")
      request.method = Method.Post
      request.contentString = """{"id":10, "name":"leo"}"""
      request.setContentTypeJson()
      val ret = Await.result(client.send(request).map(_.contentString))
      ret shouldBe """RichRequest(10,leo)"""
    }

    def `test parameter mappings` {
      // Use the default argument
      {
        val request = Request("/v1/json_api_default")
        request.method = Method.Post
        val ret = Await.result(client.send(request).map(_.contentString))
        ret shouldBe """RichRequest(100,dummy)"""
      }

      // GET requests with query parameters
      {
        val request = Request("/v1/json_api?id=10&name=leo")
        request.method = Method.Get
        val ret = Await.result(client.send(request).map(_.contentString))
        ret shouldBe """RichRequest(10,leo)"""
      }

      // JSON requests with POST
      {
        val request = Request("/v1/json_api")
        request.method = Method.Post
        request.contentString = """{"id":10, "name":"leo"}"""
        val ret = Await.result(client.send(request).map(_.contentString))
        ret shouldBe """RichRequest(10,leo)"""
      }
    }

    def `test error response` = {
      warn("Exception response test")
      val l  = Logger.of[FinagleServer]
      val lv = l.getLogLevel
      l.setLogLevel(LogLevel.ERROR)
      try {
        val request = Request("/v1/error")
        val ret     = Await.result(client.sendSafe(request)) // Receive the raw error response
        ret.statusCode shouldBe 500
      } finally {
        l.setLogLevel(lv)
      }
    }

    def `MsgPack response` = {
      // MessagePack request
      {
        val request = Request("/v1/json_api")
        request.method = Method.Post
        val msgpack = JSONCodec.toMsgPack("""{"id":10, "name":"leo"}""")
        request.content = ByteArray.Owned(msgpack)
        request.contentType = "application/x-msgpack"
        val ret = Await.result(client.send(request).map(_.contentString))
        ret shouldBe """RichRequest(10,leo)"""
      }

      // Receive MessagePack
      {
        val request = Request("/v1/raw_string_arg")
        request.method = Method.Post
        request.contentType = "application/x-msgpack"
        val msgpack = MessagePack.newBufferPacker.packString("1.0").toByteArray
        request.content = ByteArray.Owned(msgpack)
        val response = Await.result(client.send(request))
        response.contentString shouldBe "1.0"
        response.statusCode shouldBe HttpStatus.Ok_200.code
      }
    }

    def `Raw string request` {
      // Raw string arg
      val request = Request("/v1/raw_string_arg")
      request.method = Method.Post
      request.contentString = "1.0"
      Await.result(client.send(request).map(_.contentString)) shouldBe "1.0"
    }

    def `Finagle Reader[Buf] response` {
      val request = Request("/v1/reader")
      request.method = Method.Get
      val json  = Await.result(client.send(request).map(_.contentString))
      val codec = MessageCodec.of[RichInfo]
      codec.unpackJson(json) shouldBe Some(RichInfo("0.1", "MyApi", RichNestedInfo("test-server")))
    }

    val richInfo = RichInfo("0.1", "MyApi", RichNestedInfo("test-server"))

    def `convert Reader[X] response to JSON stream`: Unit = {
      val request = Request("/v1/reader-seq")
      request.method = Method.Get
      val json = Await.result(client.send(request).map(_.contentString))
      debug(json)
      val codec = MessageCodec.of[Seq[RichInfo]]
      codec.fromJson(json) shouldBe Seq(richInfo, richInfo)
    }

    def `Convert Reader[X] response to MsgPack stream`: Unit = {
      val request = Request("/v1/reader-seq")
      request.method = Method.Get
      request.accept = "application/x-msgpack"
      val msgpack = Await.result {
        client.send(request).map { resp =>
          val c       = resp.content
          val msgpack = new Array[Byte](c.length)
          c.write(msgpack, 0)
          msgpack
        }
      }
      val codec = MessageCodec.of[RichInfo]

      Control.withResource(MessagePack.newUnpacker(msgpack)) { unpacker =>
        while (unpacker.hasNext) {
          val v = unpacker.unpackValue
          codec.fromMsgPack(v.toMsgpack) shouldBe richInfo
        }
      }
    }

    def `return 204 for Unit response` = {
      val result = Await.result(client.send(Request(Method.Delete, "/v1/delete")))
      result.statusCode shouldBe HttpStatus.NoContent_204.code
    }

    def `support scala.concurrent.Future[X]` : Unit = {
      val result = Await.result(client.send(Request(Method.Get, "/v1/scala-future")))
      result.statusCode shouldBe HttpStatus.Ok_200.code
      result.contentString shouldBe "Hello Scala Future"
    }

    def `support scala.concurrent.Future[Response]` : Unit = {
      val result = Await.result(client.send(Request(Method.Get, "/v1/scala-future2")))
      result.statusCode shouldBe HttpStatus.Ok_200.code
      result.contentString shouldBe "Hello Scala Future"
    }

    def `support query parameter mapping`: Unit = {
      val result = Await.result(client.send(Request(Method.Get, "/v1/user/1/profile?session_id=xyz")))
      result.statusCode shouldBe HttpStatus.Ok_200.code
      result.contentString shouldBe "1:xyz"
    }

    def `support missing query parameter mapping`: Unit = {
      val result = Await.result(client.send(Request(Method.Get, "/v1/user/1/profile")))
      result.statusCode shouldBe HttpStatus.Ok_200.code
      result.contentString shouldBe "1:unknown"
    }

    def `support query parameter mapping for POST`: Unit = {
      val r = Request(Method.Post, "/v1/user/1/profile?session_id=xyz")
      r.contentString = "hello"
      val result = Await.result(client.send(r))
      result.statusCode shouldBe HttpStatus.Ok_200.code
      result.contentString shouldBe "1:xyz"
    }

    def `support missing query parameter mapping for POST`: Unit = {
      val r = Request(Method.Post, "/v1/user/1/profile")
      r.contentString = "hello"
      val result = Await.result(client.send(r))
      result.statusCode shouldBe HttpStatus.Ok_200.code
      result.contentString shouldBe "1:unknown"
    }
  }
}
