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

import com.twitter.finagle.Http
import com.twitter.finagle.http.{Method, Request}
import com.twitter.io.Buf.ByteArray
import com.twitter.util.{Await, Future}
import wvlet.airframe.codec.JSONCodec
import wvlet.airframe.http._
import wvlet.airframe.msgpack.spi.MessagePack
import wvlet.airspec.AirSpec
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
}

/**
  *
  */
class FinagleRouterTest extends AirSpec {

  def `support Router.of[X] and Router.add[X]` : Unit = {
    // sanity test
    val r1 = Router.add[MyApi]
    val r2 = Router.of[MyApi]
  }

  val d = newFinagleServerDesign(router = Router.add[MyApi]).noLifeCycleLogging

  def `Support function arg mappings`: Unit = {
    d.build[FinagleServer] { server =>
      val client = Http.client
        .newService(server.localAddress)
      val f1 = client(Request("/v1/info")).map { response =>
        debug(response.contentString)
      }
      val f2 = client(Request("/v1/rich_info")).map { r =>
        debug(r.contentString)
      }

      Await.result(f1.join(f2))

      // making many requests
      val futures = (0 until 5).map { x =>
        client(Request("/v1/rich_info")).map { response =>
          response.contentString
        }
      }

      val result = Await.result(Future.collect(futures))
      debug(result.mkString(", "))

      // Future response
      Await.result(client(Request("/v1/future")).map { response =>
        response.contentString
      }) shouldBe "hello"

      // JSON response
      {
        val json = Await.result(client(Request("/v1/rich_info_future")).map { response =>
          response.contentString
        })

        json shouldBe """{"version":"0.1","name":"MyApi","details":{"serverType":"test-server"}}"""
      }

      // JSON POST request
      {
        val request = Request("/v1/json_api")
        request.method = Method.Post
        request.contentString = """{"id":10, "name":"leo"}"""
        val ret = Await.result(client(request).map(_.contentString))
        ret shouldBe """RichRequest(10,leo)"""
      }

      // JSON POST request with explicit JSON content type
      {
        val request = Request("/v1/json_api")
        request.method = Method.Post
        request.contentString = """{"id":10, "name":"leo"}"""
        request.setContentTypeJson()
        val ret = Await.result(client(request).map(_.contentString))
        ret shouldBe """RichRequest(10,leo)"""
      }

      // Use the default argument
      {
        val request = Request("/v1/json_api_default")
        request.method = Method.Post
        val ret = Await.result(client(request).map(_.contentString))
        ret shouldBe """RichRequest(100,dummy)"""
      }

      // GET requests with query parameters
      {
        val request = Request("/v1/json_api?id=10&name=leo")
        request.method = Method.Get
        val ret = Await.result(client(request).map(_.contentString))
        ret shouldBe """RichRequest(10,leo)"""
      }

      // JSON requests with POST
      {
        val request = Request("/v1/json_api")
        request.method = Method.Post
        request.contentString = """{"id":10, "name":"leo"}"""
        val ret = Await.result(client(request).map(_.contentString))
        ret shouldBe """RichRequest(10,leo)"""
      }

      // Error test
      {
        warn("Exception response test")
        val l  = Logger.of[FinagleServer]
        val lv = l.getLogLevel
        l.setLogLevel(LogLevel.ERROR)
        try {
          val request = Request("/v1/error")
          val ret     = Await.result(client(request))
          ret.statusCode shouldBe 500
        } finally {
          l.setLogLevel(lv)
        }
      }

      // Msgpack body
      {
        val request = Request("/v1/json_api")
        request.method = Method.Post
        val msgpack = JSONCodec.toMsgPack("""{"id":10, "name":"leo"}""")
        request.content = ByteArray.Owned(msgpack)
        request.contentType = "application/x-msgpack"
        val ret = Await.result(client(request).map(_.contentString))
        ret shouldBe """RichRequest(10,leo)"""
      }

      // Raw string arg
      {
        val request = Request("/v1/raw_string_arg")
        request.method = Method.Post
        request.contentString = "1.0"
        Await.result(client(request).map(_.contentString)) shouldBe "1.0"
      }

      // Receive MessagePack
      {
        val request = Request("/v1/raw_string_arg")
        request.method = Method.Post
        request.contentType = "application/x-msgpack"
        val msgpack = MessagePack.newBufferPacker.packString("1.0").toByteArray
        request.content = ByteArray.Owned(msgpack)
        Await.result(client(request).map(_.contentString)) shouldBe "1.0"
      }

    }
  }

  def `support production mode`: Unit = {
    d.withProductionMode.build[FinagleServer] { server =>
      // #432: Just need to check the startup of finagle without MISSING_DEPENDENCY error
    }
  }
}
