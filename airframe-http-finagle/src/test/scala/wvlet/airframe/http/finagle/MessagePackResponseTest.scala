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
import com.twitter.finagle.http
import com.twitter.finagle.http.Method
import wvlet.airframe.Design
import wvlet.airframe.codec.MessageCodec
import wvlet.airframe.codec.PrimitiveCodec.StringCodec
import wvlet.airframe.http.{Endpoint, HttpMethod, Router}
import wvlet.airframe.msgpack.spi.MsgPack
import wvlet.airspec.AirSpec

case class SampleResponse(id: Int, name: String)

trait TestMessagePackApi {
  @Endpoint(path = "/v1/hello")
  def hello: SampleResponse = {
    SampleResponse(1, "leo")
  }

  @Endpoint(method = HttpMethod.DELETE, path = "/v1/resource/:id")
  def delete(id: Int): Unit = {}

  @Endpoint(path = "/v1/hello_string")
  def helloStr: String = {
    "hello"
  }
  @Endpoint(path = "/v1/hello_msgpack")
  def helloMsgPack: MsgPack = {
    MessageCodec.of[Seq[String]].toMsgPack(Seq("hello", "msgpack"))
  }
}

/**
  */
class MessagePackResponseTest extends AirSpec {

  val router = Router.of[TestMessagePackApi]

  override protected def design: Design =
    Finagle.server.withRouter(router).design +
      Finagle.client.syncClientDesign

  test("support Accept: application/x-msgpack") { (client: FinagleSyncClient) =>
    val req = http.Request("/v1/hello")
    req.accept = "application/x-msgpack"
    val resp    = client.send(req)
    val c       = resp.content
    val msgpack = new Array[Byte](c.length)
    c.write(msgpack, 0)

    val decoded = MessageCodec.of[SampleResponse].fromMsgPack(msgpack)
    debug(decoded)
    decoded shouldBe SampleResponse(1, "leo")
  }

  test("support raw String response with application/x-msgpack") { (client: FinagleSyncClient) =>
    val req = http.Request("/v1/hello_string")
    req.accept = "application/x-msgpack"
    val resp    = client.send(req)
    val c       = resp.content
    val msgpack = new Array[Byte](c.length)
    c.write(msgpack, 0)

    val decoded = StringCodec.fromMsgPack(msgpack)
    debug(decoded)
    decoded shouldBe "hello"
  }

  test("support raw MsgPack response with application/x-msgpack") { (client: FinagleSyncClient) =>
    val req = http.Request("/v1/hello_msgpack")
    req.accept = "application/x-msgpack"
    val resp    = client.send(req)
    val c       = resp.content
    val msgpack = new Array[Byte](c.length)
    c.write(msgpack, 0)

    val decoded = MessageCodec.of[Seq[String]].fromMsgPack(msgpack)
    debug(decoded)
    decoded shouldBe Seq("hello", "msgpack")
  }

  test("DELETE response should have no content body") { (client: FinagleSyncClient) =>
    val req = http.Request("/v1/resource/100")
    req.method = Method.Delete
    req.contentType = "application/x-msgpack"
    req.accept = "application/x-msgpack"

    val resp = client.send(req)
    val c    = resp.content
    resp.status.code shouldBe 204
    c.length shouldBe 0
  }
}
