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
package wvlet.airframe.http.js
import wvlet.airframe.codec.{MessageCodec, MessageCodecFactory, MessageContext}
import wvlet.airframe.http.Http
import wvlet.airframe.http.HttpMessage.Request
import wvlet.airframe.http.js.JSHttpClient.MessageEncoding
import wvlet.airframe.msgpack.spi.{Packer, Unpacker}
import wvlet.airframe.surface.Surface
import wvlet.airspec.AirSpec

/**
  */
object JSHttpClientTest extends AirSpec {

  implicit val ec: scala.concurrent.ExecutionContext = scala.concurrent.ExecutionContext.global
  case class Person(id: Int, name: String)

  test("create http client") {
    ignore("ignore server interaction tests")
    val s      = Surface.of[Person]
    val client = JSHttpClient()
    client.getOps[Person, Person]("/v1/info", Person(1, "leo"), s, s).recover { case e: Throwable =>
      logger.warn(e)
      1
    }
  }

  test("crete a request") {
    val req = Http.request("/v1/info")
  }

  sealed trait Suit
  case object Spade extends Suit
  case object Heart extends Suit

  object SuitCodec extends MessageCodec[Suit] {
    override def pack(
        p: Packer,
        v: Suit
    ): Unit = {
      // Use lowercase string for making sure the custom codec is used
      p.packString(v.toString.toLowerCase)
    }
    override def unpack(
        u: Unpacker,
        v: MessageContext
    ): Unit = {
      u.unpackString match {
        case "spade" => v.setObject(Spade)
        case "heart" => v.setObject(Heart)
        case _       => v.setNull
      }

    }
  }

  test("support custom codec") {
    val f = MessageCodecFactory.defaultFactory.withCodecs(Map(Surface.of[Suit] -> SuitCodec))
    val client =
      JSHttpClient(JSHttpClientConfig().withRequestEncoding(MessageEncoding.JsonEncoding).withCodecFactory(f))

    val request = client.prepareRequestBody[Suit](Http.POST("/v1/suit"), Spade, Surface.of[Suit])
    request.contentString shouldBe """"spade""""
  }
}
