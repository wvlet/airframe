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
import java.text.DateFormat
import java.time.{Instant, ZonedDateTime}
import java.util.Date

import org.msgpack.core.{MessagePacker, MessageUnpacker}
import org.msgpack.value.ValueType
import wvlet.airframe.msgpack.io.ByteArrayBuffer
import wvlet.airframe.msgpack.spi._
import wvlet.log.LogSupport
import wvlet.surface
import wvlet.surface.Surface

import scala.util.{Failure, Success, Try}

/**
  * Standard codec collection
  */
object StandardCodec {

  val javaClassCodec = Map(
    surface.of[File] -> FileCodec
  )

  val javaTimeCodec = Map(
    surface.of[Instant]       -> JavaInstantTimeCodec,
    surface.of[ZonedDateTime] -> ZonedDateTimeCodec,
    surface.of[Date]          -> JavaUtilDateCodec
  )

  val standardCodec
    : Map[Surface, MessageCodec[_]] = PrimitiveCodec.primitiveCodec ++ PrimitiveCodec.primitiveArrayCodec ++ javaClassCodec ++ javaTimeCodec

  object FileCodec extends MessageCodec[File] {
    override def pack(p: MessagePacker, v: File): Unit = {
      p.packString(v.getPath)
    }
    override def unpack(u: MessageUnpacker, v: MessageHolder): Unit = {
      val path = u.unpackString()
      v.setObject(new File(path))
    }
  }

  object JavaInstantTimeCodec extends MessageCodec[Instant] {
    override def pack(p: MessagePacker, v: Instant): Unit = {
      // TODO airframe-msgpack in Codec interface
      // Use msgpack Timestamp type
      val buf    = ByteArrayBuffer.newBuffer(15)
      val cursor = WriteCursor(buf, 0)
      Packer.packTimestamp(cursor, v)
      val extData = buf.readBytes(0, cursor.lastWrittenBytes)
      p.writePayload(extData, 0, cursor.lastWrittenBytes)
    }

    override def unpack(u: MessageUnpacker, v: MessageHolder): Unit = {
      Try {
        u.getNextFormat.getValueType match {
          case ValueType.STRING =>
            // Use ISO instant formatter
            val isoInstantFormat = u.unpackString()
            Try(Instant.parse(isoInstantFormat))
              .getOrElse(Instant.ofEpochMilli(isoInstantFormat.toLong))
          case ValueType.INTEGER =>
            val epochMillis = u.unpackLong()
            Instant.ofEpochMilli(epochMillis)
          case ValueType.EXTENSION =>
            // TODO usg airframe-msgpack directly
            val extHeader = u.unpackExtensionTypeHeader()
            val buf       = ByteArrayBuffer.newBuffer(15)
            val cursor    = WriteCursor(buf, 0)
            Packer.packExtTypeHeader(cursor, ExtTypeHeader(extHeader.getType, extHeader.getLength))
            val data = u.readPayload(extHeader.getLength)
            cursor.writeBytes(data)
            Unpacker.unpackTimestamp(ReadCursor(buf, 0))
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
    override def pack(p: MessagePacker, v: ZonedDateTime): Unit = {
      // Use java standard ZonedDateTime string repr such as "2007-12-03T10:15:30+01:00[Europe/Paris]"
      p.packString(v.toString)
    }

    override def unpack(u: MessageUnpacker, v: MessageHolder): Unit = {
      val zonedDateTimeStr = u.unpackString()
      Try(ZonedDateTime.parse(zonedDateTimeStr)) match {
        case Success(zd) =>
          v.setObject(zd)
        case Failure(e) =>
          v.setIncompatibleFormatException(this,
                                           s"${zonedDateTimeStr} cannot be read as ZonedDateTime: ${e.getMessage}")
      }
    }
  }

  object JavaUtilDateCodec extends MessageCodec[Date] with LogSupport {
    private val format = DateFormat.getInstance()

    override def pack(p: MessagePacker, v: Date): Unit = {
      // Use Instant for encoding
      JavaInstantTimeCodec.pack(p, v.toInstant)
    }
    override def unpack(u: MessageUnpacker, v: MessageHolder): Unit = {
      JavaInstantTimeCodec.unpack(u, v)
      if (!v.isNull) {
        v.setObject(Date.from(v.getLastValue.asInstanceOf[Instant]))
      }
    }
  }

  case class EnumCodec[A](enumType: Class[A]) extends MessageCodec[A] with LogSupport {
    private val enumValueOfMethod = classOf[Enum[_]].getDeclaredMethod("valueOf", classOf[Class[_]], classOf[String])

    override def pack(p: MessagePacker, v: A): Unit = {
      p.packString(v.asInstanceOf[Enum[_]].name())
    }

    override def unpack(u: MessageUnpacker, v: MessageHolder): Unit = {
      val name = u.unpackString

      Try(enumValueOfMethod.invoke(null, enumType, name)) match {
        case Success(enum) => v.setObject(enum)
        case _ =>
          v.setIncompatibleFormatException(this, s"${name} is not a value of ${enumType}")
      }
    }
  }

}
