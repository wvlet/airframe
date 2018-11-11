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

import java.io.File
import java.time.{Instant, ZonedDateTime}

import wvlet.airframe.{msgpack, surface}

/**
  *
  */
class StandardCodecTest extends CodecSpec {
  "StandardCodec" should {
    "support File" in {
      val codec          = MessageCodec.of[File]
      def check(v: File) = checkCodec(codec, v)
      check(new File("sample.txt"))
      check(new File("/var/log"))
      check(new File("/etc/conf.d/myconf.conf"))
      check(new File("c:/etc/conf.d/myconf.conf"))
      check(new File("."))
      check(new File(".."))
      check(new File("relative/path.txt"))
    }

    "support Enum" in {
      for (v <- TestEnum.values()) {
        roundtrip[TestEnum](surface.of[TestEnum], v)
      }

      val codec = MessageCodec.of[TestEnum]
      val p     = msgpack.newBufferPacker
      p.packString("ABORTED") // non-existing enum type
      val v = codec.unpackMsgPack(p.toByteArray)
      v shouldBe empty
    }
  }
}
