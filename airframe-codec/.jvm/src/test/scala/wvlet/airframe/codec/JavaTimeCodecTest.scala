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
import java.time.{Instant, ZonedDateTime}

import wvlet.airframe.{AirframeSpec, msgpack, surface}

/**
  *
  */
class JavaTimeCodecTest extends CodecSpec {
  "support Java Instant" in {
    val now     = Instant.now()
    val timeStr = "2018-05-26T21:10:29.858818Z"
    val i       = Instant.parse(timeStr)
    roundtrip(surface.of[Instant], i)

    roundtrip(surface.of[Instant], Instant.ofEpochMilli(0))
    roundtrip(surface.of[Instant], Instant.ofEpochMilli(14000000))
    roundtrip(surface.of[Instant], now)

    val codec = Codec.of[Seq[Instant]]
    val p     = msgpack.newBufferPacker

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

  "support ZonedDateTime" in {
    roundtrip(surface.of[ZonedDateTime], ZonedDateTime.now())
    roundtrip(surface.of[ZonedDateTime], ZonedDateTime.parse("2007-12-03T10:15:30+01:00[Europe/Paris]"))

    val codec = Codec.of[ZonedDateTime]
    val p     = msgpack.newBufferPacker
    p.packString("non-date string")
    val v = codec.unpackMsgPack(p.toByteArray)
    v shouldBe empty
  }

  "support java.util.Date" in {
    val now = java.util.Date.from(Instant.now())
    roundtrip(surface.of[java.util.Date], now)
  }

}
