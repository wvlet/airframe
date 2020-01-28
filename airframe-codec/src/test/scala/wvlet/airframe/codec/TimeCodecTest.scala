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
import java.time.Instant

import wvlet.airframe.msgpack.spi.MessagePack
import wvlet.airframe.surface.Surface

/**
  *
  */
class TimeCodecTest extends CodecSpec {
  test("support Java Instant") {
    val timeStr = "2018-05-26T21:10:29.858818Z"
    val i       = Instant.parse(timeStr)
    roundtrip(Surface.of[Instant], i)

    roundtrip(Surface.of[Instant], Instant.ofEpochMilli(0))
    roundtrip(Surface.of[Instant], Instant.ofEpochMilli(14000000))

    val now = Instant.now()
    roundtrip(Surface.of[Instant], now)

    val codec = MessageCodec.of[Seq[Instant]]
    val p     = MessagePack.newBufferPacker

    val epochSecond = Instant.ofEpochMilli(now.getEpochSecond)
    p.packArrayHeader(4)
    p.packLong(epochSecond.toEpochMilli) // Timestamp as millisec LONG
    p.packString(epochSecond.toString)   // Timestamp as millisec string
    p.packString(timeStr)                // Timestamp in string format
    p.packString("invalidstr")

    val v = codec.unpackMsgPack(p.toByteArray)
    v shouldBe defined
    v.get shouldBe Seq[Instant](epochSecond, epochSecond, i, null)
  }
}
