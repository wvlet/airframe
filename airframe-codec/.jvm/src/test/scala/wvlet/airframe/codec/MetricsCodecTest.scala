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
import wvlet.airframe.AirframeSpec
import wvlet.airframe.metrics.{DataSize, ElapsedTime}
import wvlet.airframe.msgpack.spi.MessagePack
import wvlet.airframe.surface.{Surface, Zero}

/**
  *
  */
class MetricsCodecTest extends AirframeSpec {

  "support DataSize" in {
    val codec = MessageCodec.of[DataSize]

    // String
    {
      val d = DataSize("10GB")
      codec.unpackMsgPack(codec.toMsgPack(d)) shouldBe Some(d)
    }
    // Float
    {
      val p = MessagePack.newBufferPacker
      p.packFloat(1000)
      codec.unpackMsgPack(p.toByteArray) shouldBe Some(DataSize(1000))
    }

    // Int
    {
      val p = MessagePack.newBufferPacker
      p.packInt(1000)
      codec.unpackMsgPack(p.toByteArray) shouldBe Some(DataSize(1000))
    }
  }

  "support Zero.of[DataSize]" in {
    val z = Zero.zeroOf(Surface.of[DataSize])
    z shouldBe DataSize(0)
  }

  "support ElapsedTime" in {
    val codec = MessageCodec.of[ElapsedTime]

    // String
    {
      val v = ElapsedTime("10h")
      codec.unpackMsgPack(codec.toMsgPack(v)) shouldBe Some(v)
    }

    // Float
    {
      val p = MessagePack.newBufferPacker
      p.packFloat(1000)
      codec.unpackMsgPack(p.toByteArray) shouldBe Some(ElapsedTime.succinctNanos(1000))
    }

    // Int
    {
      val p = MessagePack.newBufferPacker
      p.packInt(1000)
      codec.unpackMsgPack(p.toByteArray) shouldBe Some(ElapsedTime.succinctNanos(1000))
    }

  }

}
