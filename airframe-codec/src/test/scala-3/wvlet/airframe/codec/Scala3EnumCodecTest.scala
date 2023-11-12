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
package wvlet.airframe.codec

import wvlet.airframe.json.JSON
import wvlet.airframe.msgpack.spi.MessagePack
import wvlet.airspec.AirSpec

object Scala3EnumCodecTest extends AirSpec:
  enum Color:
    case Red, Green, Blue

  test("pack Scala 3 Enum") {
    val codec = MessageCodec.of[Color]
    Color.values.foreach { c =>
      val packed   = codec.pack(c)
      val unpacked = codec.unpack(packed)
      unpacked shouldBe c
    }
  }

  test("handle invalid enum value input") {
    val codec   = MessageCodec.of[Color]
    val msgpack = MessagePack.newBufferPacker.packString("Black").toByteArray
    intercept[IllegalArgumentException] {
      codec.unpack(msgpack)
    }
  }

  case class Point(x: Int, y: Int, c: Color)

  test("pack case class with Enum") {
    val codec    = MessageCodec.of[Point]
    val p        = Point(1, 2, Color.Red)
    val packed   = codec.pack(p)
    val unpacked = codec.unpack(packed)
    unpacked shouldBe p
  }

  test("toJson") {
    val codec = MessageCodec.of[Point]
    val p     = Point(1, 2, Color.Red)
    val json  = codec.toJson(p)
    json shouldBe """{"x":1,"y":2,"c":"Red"}"""
  }

  case class OptColor(p: Option[Color])

  test("Option[Enum]") {
    val codec = MessageCodec.of[OptColor]
    val p     = OptColor(Some(Color.Red))
    val json  = codec.toJson(p)
    json shouldBe """{"p":"Red"}"""
  }

  test("Option[Enum] with None") {
    val codec = MessageCodec.of[OptColor]
    val p     = OptColor(None)
    val json  = codec.toJson(p)
    json shouldBe """{}"""
  }
