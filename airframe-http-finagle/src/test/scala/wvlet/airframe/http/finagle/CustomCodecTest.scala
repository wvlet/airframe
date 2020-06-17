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
import com.twitter.finagle.http.Request
import wvlet.airframe.codec.{MessageCodec, MessageContext}
import wvlet.airframe.control.Control
import wvlet.airframe.http.{Endpoint, RPC, Router}
import wvlet.airframe.msgpack.spi.{Packer, Unpacker}
import wvlet.airframe.surface.Surface
import wvlet.airspec.AirSpec
import wvlet.log.LogSupport

/**
  */
object CustomCodecTest extends AirSpec {

  sealed trait Suit
  case object Spade extends Suit
  case object Heart extends Suit

  object SuitCodec extends MessageCodec[Suit] {
    override def pack(
        p: Packer,
        v: Suit
    ): Unit = {
      p.packString(v.toString)
    }
    override def unpack(
        u: Unpacker,
        v: MessageContext
    ): Unit = {
      u.unpackString match {
        case "Spade" => v.setObject(Spade)
        case "Heart" => v.setObject(Heart)
        case other   => v.setError(new IllegalArgumentException(s"Unknown suit: ${other}"))
      }
    }
  }

  class MyApi extends LogSupport {
    @Endpoint(path = "/hello")
    def hello(suit: Suit): String = {
      suit.toString
    }
  }

  test(
    s"custom codec",
    design = Finagle.server
      .withRouter(Router.add[MyApi])
      .withCustomCodec(Map(Surface.of[Suit] -> SuitCodec))
      .design
      .add(Finagle.client.syncClientDesign)
  ) { client: FinagleSyncClient =>
    client.send(Request("/hello?suit=Spade")).contentString shouldBe "Spade"
    client.send(Request("/hello?suit=Heart")).contentString shouldBe "Heart"
  }

}
