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

/**
  *
  */
class MessageCodecFactoryTest extends AirframeSpec {

  "MessageCodecFactory" should {
    "generate codec" in {
      val intCodec = MessageCodec.of[Int]
      val v        = new MessageHolder
      forAll { (i: Int) =>
        val packer = MessagePack.newDefaultBufferPacker()
        intCodec.pack(packer, i)
        val unpacker = MessagePack.newDefaultUnpacker(packer.toByteArray)
        intCodec.unpack(unpacker, v)

        v.isNull shouldBe false
        v.getValueType shouldBe Schema.INTEGER
        v.getLong shouldBe (i)
      }

    }
  }

}
