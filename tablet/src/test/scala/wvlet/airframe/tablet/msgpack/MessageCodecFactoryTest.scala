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

import wvlet.airframe.tablet.Schema

import scala.reflect.runtime.{universe => ru}

/**
  *
  */
class MessageCodecFactoryTest extends CodecSpec {

  "MessageCodecFactory" should {
    "support numeric" in {
      roundTripTestWithStr[Int](Schema.INTEGER)
      roundTripTestWithStr[Byte](Schema.INTEGER)
      roundTripTestWithStr[Short](Schema.INTEGER)
      roundTripTestWithStr[Long](Schema.INTEGER)
      roundTripTestWithStr[Boolean](Schema.BOOLEAN)
    }

    "support char" in {
      roundTripTest[Char](Schema.INTEGER)
    }

    "support float" in {
      roundTripTestWithStr[Float](Schema.FLOAT)
      roundTripTestWithStr[Double](Schema.FLOAT)
    }

    "support string" in {
      roundTripTest[String](Schema.STRING)
    }

    "support arrays" taggedAs ("array") in {
      arrayRoundTripTest[Byte]
      arrayRoundTripTest[Char]
      arrayRoundTripTest[Int]
      arrayRoundTripTest[Short]
      arrayRoundTripTest[Long]
      arrayRoundTripTest[String]
      arrayRoundTripTest[Float]
      arrayRoundTripTest[Double]
      arrayRoundTripTest[Boolean]
    }

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

  }
}

object MessageCodecFactoryTest {}
