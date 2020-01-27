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
import java.util.Date

import wvlet.airframe.msgpack.io.ByteArrayBuffer
import wvlet.airframe.msgpack.spi._
import wvlet.airframe.surface.Surface
import wvlet.log.LogSupport

import scala.util.{Failure, Success, Try}

/**
  * Codec for java.time package
  */
object JavaTimeCodec {
  val javaTimeCodecs = Map(
    Surface.of[ZonedDateTime] -> ZonedDateTimeCodec,
    Surface.of[Date]          -> JavaUtilDateCodec
  )

  object ZonedDateTimeCodec extends MessageCodec[ZonedDateTime] {
    override def pack(p: Packer, v: ZonedDateTime): Unit = {
      // Use java standard ZonedDateTime string repr such as "2007-12-03T10:15:30+01:00[Europe/Paris]"
      p.packString(v.toString)
    }

    override def unpack(u: Unpacker, v: MessageContext): Unit = {
      val zonedDateTimeStr = u.unpackString
      Try(ZonedDateTime.parse(zonedDateTimeStr)) match {
        case Success(zd) =>
          v.setObject(zd)
        case Failure(e) =>
          v.setIncompatibleFormatException(
            this,
            s"${zonedDateTimeStr} cannot be read as ZonedDateTime: ${e.getMessage}"
          )
      }
    }
  }

}
