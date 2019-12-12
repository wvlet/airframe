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
    Surface.of[Instant]       -> JavaInstantTimeCodec,
    Surface.of[ZonedDateTime] -> ZonedDateTimeCodec,
    Surface.of[Date]          -> JavaUtilDateCodec
  )

  object JavaInstantTimeCodec extends MessageCodec[Instant] {
    override def pack(p: Packer, v: Instant): Unit = {
      // TODO airframe-msgpack in Codec interface
      // Use msgpack Timestamp type
      val buf    = ByteArrayBuffer.newBuffer(15)
      val cursor = WriteCursor(buf, 0)
      OffsetPacker.packTimestamp(cursor, v)
      val extData = buf.readBytes(0, cursor.lastWrittenBytes)
      p.writePayload(extData, 0, cursor.lastWrittenBytes)
    }

    override def unpack(u: Unpacker, v: MessageContext): Unit = {
      Try {
        u.getNextFormat.getValueType match {
          case ValueType.STRING =>
            // Use ISO instant formatter
            val isoInstantFormat = u.unpackString
            Try(Instant.parse(isoInstantFormat))
              .getOrElse(Instant.ofEpochMilli(isoInstantFormat.toLong))
          case ValueType.INTEGER =>
            val epochMillis = u.unpackLong
            Instant.ofEpochMilli(epochMillis)
          case ValueType.EXTENSION =>
            u.unpackTimestamp
          case other =>
            v.setIncompatibleFormatException(this, s"Cannot create Instant from ${other} type")
        }
      } match {
        case Success(x) => v.setObject(x)
        case Failure(e) => v.setError(e)
      }
    }
  }

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

  object JavaUtilDateCodec extends MessageCodec[Date] with LogSupport {
    override def pack(p: Packer, v: Date): Unit = {
      // Use Instant for encoding
      JavaInstantTimeCodec.pack(p, v.toInstant)
    }
    override def unpack(u: Unpacker, v: MessageContext): Unit = {
      JavaInstantTimeCodec.unpack(u, v)
      if (!v.isNull) {
        v.setObject(Date.from(v.getLastValue.asInstanceOf[Instant]))
      }
    }
  }
}
