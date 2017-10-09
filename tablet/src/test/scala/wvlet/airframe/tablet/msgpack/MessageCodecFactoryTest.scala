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
package wvlet.airframe.tablet.msgpack

import org.msgpack.core.{MessagePack, MessagePacker}
import wvlet.airframe.AirframeSpec
import wvlet.airframe.tablet.Schema
import org.scalatest.prop.GeneratorDrivenPropertyChecks._
import wvlet.airframe.tablet.Schema.ColumnType

/**
  *
  */
class MessageCodecFactoryTest extends AirframeSpec {

  def roundtrip[A](codec: MessageCodec[A], v: A, expectedType: ColumnType): MessageHolder = {
    val h      = new MessageHolder
    val packer = MessagePack.newDefaultBufferPacker()
    codec.pack(packer, v)
    val unpacker = MessagePack.newDefaultUnpacker(packer.toByteArray)
    codec.unpack(unpacker, h)

    h.isNull shouldBe false
    h.getValueType shouldBe expectedType
    h
  }

  "MessageCodecFactory" should {
    "support int" in {
      val codec = MessageCodec.of[Int]
      forAll { (i: Int) =>
        val v = roundtrip(codec, i, Schema.INTEGER)
        v.getLong shouldBe (i)
      }
    }

    "support float" in {
      val codec = MessageCodec.of[Float]
      forAll { (f: Float) =>
        val v = roundtrip(codec, f, Schema.FLOAT)
        v.getDouble shouldBe (f)
      }
    }
  }

}
