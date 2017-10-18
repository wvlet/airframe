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

import java.io.File
import java.time.{Instant, ZonedDateTime}

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

    "support Java Instant" in {
      roundtrip(Instant.ofEpochMilli(0))
      roundtrip(Instant.ofEpochMilli(14000000))
      roundtrip(Instant.now())
    }

    "support ZonedDateTime" in {
      roundtrip(ZonedDateTime.now())
      roundtrip(ZonedDateTime.parse("2007-12-03T10:15:30+01:00[Europe/Paris]"))
    }

    "support java.util.Date" in {
      val now = java.util.Date.from(Instant.now)
      roundtrip(now)
    }

    "support Enum" in {
      for (v <- TestEnum.values()) {
        roundtrip[TestEnum](v)
      }
    }
  }
}
