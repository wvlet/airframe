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
import wvlet.airframe.Design
import wvlet.airframe.codec.MessageCodec
import wvlet.airframe.codec.PrimitiveCodec.StringCodec
import wvlet.airframe.http.client.SyncClient
import wvlet.airframe.http.netty.Netty
import wvlet.airframe.http.{Endpoint, Http, HttpMethod, HttpStatus, Router, RxRouter}
import wvlet.airframe.msgpack.spi.MsgPack
import wvlet.airspec.AirSpec

case class SampleResponse(id: Int, name: String)

class TestMessagePackApi {
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

  val router = RxRouter.of[TestMessagePackApi]

  override protected def design: Design =
    Netty.server.withRouter(router).designWithSyncClient

  test("support Accept: application/x-msgpack") { (client: SyncClient) =>
    val req                  = Http.GET("/v1/hello").withAcceptMsgPack
    val resp                 = client.send(req)
    val msgpack: Array[Byte] = resp.contentBytes
    val decoded              = MessageCodec.of[SampleResponse].fromMsgPack(msgpack)
    debug(decoded)
    decoded shouldBe SampleResponse(1, "leo")
  }

  test("support raw String response with application/x-msgpack") { (client: SyncClient) =>
    val req     = Http.GET("/v1/hello_string").withAcceptMsgPack
    val resp    = client.send(req)
    val msgpack = resp.contentBytes
    val decoded = StringCodec.fromMsgPack(msgpack)
    debug(decoded)
    decoded shouldBe "hello"
  }

  test("support raw MsgPack response with application/x-msgpack") { (client: SyncClient) =>
    val req     = Http.GET("/v1/hello_msgpack").withAcceptMsgPack
    val resp    = client.send(req)
    val msgpack = resp.contentBytes
    val decoded = MessageCodec.of[Seq[String]].fromMsgPack(msgpack)
    debug(decoded)
    decoded shouldBe Seq("hello", "msgpack")
  }

  test("DELETE response should have no content body") { (client: SyncClient) =>
    val req  = Http.DELETE("/v1/resource/100").withContentTypeMsgPack.withAcceptMsgPack
    val resp = client.send(req)
    val c    = resp.contentBytes
    resp.status shouldBe HttpStatus.NoContent_204
    c.length shouldBe 0
  }
}
