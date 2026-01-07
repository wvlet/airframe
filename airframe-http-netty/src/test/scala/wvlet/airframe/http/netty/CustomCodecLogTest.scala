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
package wvlet.airframe.http.netty

import wvlet.airframe.codec.{MessageCodec, MessageCodecFactory, MessageContext}
import wvlet.airframe.http.HttpLoggerConfig
import wvlet.airframe.msgpack.spi.{Packer, Unpacker}
import wvlet.airframe.surface.Surface
import wvlet.airspec.AirSpec

/**
  * Test for custom codec support in HTTP access logging configuration.
  */
object CustomCodecLogTest extends AirSpec {

  case class CustomId(value: String)

  object CustomIdCodec extends MessageCodec[CustomId] {
    override def pack(p: Packer, v: CustomId): Unit = {
      p.packString(s"ID:${v.value}")
    }
    override def unpack(u: Unpacker, v: MessageContext): Unit = {
      val s = u.unpackString
      if (s.startsWith("ID:")) {
        v.setObject(CustomId(s.drop(3)))
      } else {
        v.setError(new IllegalArgumentException(s"Invalid CustomId format: ${s}"))
      }
    }
  }

  private val customCodec: PartialFunction[Surface, MessageCodec[_]] = {
    case s if s == Surface.of[CustomId] => CustomIdCodec
  }

  test("HttpLoggerConfig.withCustomCodec") {
    val config = HttpLoggerConfig().withCustomCodec(customCodec)
    val codec  = config.codecFactory.of(Surface.of[CustomId])
    codec shouldBeTheSameInstanceAs CustomIdCodec
  }

  test("HttpLoggerConfig.withCodecFactory") {
    val customFactory = MessageCodecFactory.defaultFactoryForJSON.withCodecs(customCodec)
    val config        = HttpLoggerConfig().withCodecFactory(customFactory)

    config.codecFactory shouldBeTheSameInstanceAs customFactory
    config.codecFactory.of(Surface.of[CustomId]) shouldBeTheSameInstanceAs CustomIdCodec
  }

  test("logFormatter") {
    val config = HttpLoggerConfig()

    test("serialize primitive types") {
      val logEntry = Map[String, Any](
        "method" -> "GET",
        "path"   -> "/test",
        "status" -> 200
      )
      val json = config.logFormatter(logEntry)
      debug(s"JSON: ${json}")

      json shouldContain "\"method\":\"GET\""
      json shouldContain "\"path\":\"/test\""
      json shouldContain "\"status\":200"
    }

    test("serialize nested maps") {
      val logEntry = Map[String, Any](
        "request_header" -> Map(
          "content_type" -> "application/json",
          "host"         -> "localhost"
        ),
        "status_code" -> 200
      )
      val json = config.logFormatter(logEntry)
      debug(s"JSON: ${json}")

      json shouldContain "\"request_header\":{"
      json shouldContain "\"content_type\":\"application/json\""
      json shouldContain "\"host\":\"localhost\""
    }
  }
}
