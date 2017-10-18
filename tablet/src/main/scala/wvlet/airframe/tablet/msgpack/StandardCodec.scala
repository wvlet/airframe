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
import java.text.DateFormat
import java.time.format.DateTimeFormatter
import java.time.{Instant, ZonedDateTime}
import java.util.Date

import org.msgpack.core.{MessageIntegerOverflowException, MessagePacker, MessageUnpacker}
import org.msgpack.value.ValueType
import wvlet.log.LogSupport
import wvlet.surface
import wvlet.surface.{Primitive, Surface}

import scala.util.{Failure, Success, Try}

/**
  * Standard codec collection
  */
object StandardCodec {

  val primitiveCodec: Map[Surface, MessageCodec[_]] = Map(
    Primitive.Int     -> IntCodec,
    Primitive.Long    -> LongCodec,
    Primitive.Float   -> FloatCodec,
    Primitive.Double  -> DoubleCodec,
    Primitive.Boolean -> BooleanCodec,
    Primitive.String  -> StringCodec,
    Primitive.Byte    -> ByteCodec,
    Primitive.Short   -> ShortCodec,
    Primitive.Char    -> CharCodec
  )

  val primitiveArrayCodec = Map(
    surface.of[Array[Int]]     -> IntArrayCodec,
    surface.of[Array[Long]]    -> LongArrayCodec,
    surface.of[Array[Float]]   -> FloatArrayCodec,
    surface.of[Array[Double]]  -> DoubleArrayCodec,
    surface.of[Array[Boolean]] -> BooleanArrayCodec,
    surface.of[Array[String]]  -> StringArrayCodec,
    surface.of[Array[Byte]]    -> ByteArrayCodec,
    surface.of[Array[Short]]   -> ShortArrayCodec,
    surface.of[Array[Char]]    -> CharArrayCodec
  )

  val javaClassCodec = Map(
    surface.of[File] -> FileCodec
  )

  val javaTimeCodec = Map(
    surface.of[Instant]       -> JavaInstantTimeCodec,
    surface.of[ZonedDateTime] -> ZonedDateTimeCodec,
    surface.of[Date]          -> JavaUtilDateCodec
  )

  val standardCodec: Map[Surface, MessageCodec[_]] = primitiveCodec ++ primitiveArrayCodec ++ javaClassCodec ++ javaTimeCodec

  private implicit class RichBoolean(b: Boolean) {
    def toInt: Int     = if (b) 1 else 0
    def toChar: Char   = if (b) 1 else 0
    def toByte: Byte   = if (b) 1 else 0
    def toShort: Short = if (b) 1 else 0
  }

  object ByteCodec extends MessageCodec[Byte] {
    override def pack(p: MessagePacker, v: Byte): Unit = {
      p.packByte(v)
    }

    override def unpack(u: MessageUnpacker, v: MessageHolder): Unit = {
      def read(body: => Byte) {
        try {
          v.setByte(body)
        } catch {
          case e: MessageIntegerOverflowException =>
            v.setIncompatibleFormatException(s"${e.getBigInteger} is too large for a Byte value")
          case e: NumberFormatException =>
            v.setIncompatibleFormatException(e.getMessage)
        }
      }

      val f  = u.getNextFormat
      val vt = f.getValueType
      vt match {
        case ValueType.NIL =>
          u.unpackNil()
          v.setNull
        case ValueType.INTEGER =>
          read(u.unpackByte)
        case ValueType.STRING =>
          read(u.unpackString.toByte)
        case ValueType.BOOLEAN =>
          read(u.unpackBoolean().toByte)
        case ValueType.FLOAT =>
          read(u.unpackDouble().toByte)
        case _ =>
          u.skipValue()
          v.setNull
      }
    }
  }

  object CharCodec extends MessageCodec[Char] {
    override def pack(p: MessagePacker, v: Char): Unit = {
      p.packInt(v)
    }

    override def unpack(u: MessageUnpacker, v: MessageHolder): Unit = {
      def read(body: => Char) {
        try {
          v.setChar(body)
        } catch {
          case e: MessageIntegerOverflowException =>
            v.setIncompatibleFormatException(s"${e.getBigInteger} is too large for a Char value")
          case e: NumberFormatException =>
            v.setIncompatibleFormatException(e.getMessage)
        }
      }

      val f  = u.getNextFormat
      val vt = f.getValueType
      vt match {
        case ValueType.NIL =>
          u.unpackNil()
          v.setNull
        case ValueType.INTEGER =>
          read(u.unpackInt.toChar)
        case ValueType.STRING =>
          read(u.unpackString.toInt.toChar)
        case ValueType.BOOLEAN =>
          read(u.unpackBoolean().toChar)
        case ValueType.FLOAT =>
          read(u.unpackDouble().toChar)
        case _ =>
          u.skipValue()
          v.setNull
      }
    }
  }

  object ShortCodec extends MessageCodec[Short] {
    override def pack(p: MessagePacker, v: Short): Unit = {
      p.packShort(v)
    }

    override def unpack(u: MessageUnpacker, v: MessageHolder): Unit = {
      def read(body: => Short) {
        try {
          v.setShort(body)
        } catch {
          case e: MessageIntegerOverflowException =>
            v.setIncompatibleFormatException(s"${e.getBigInteger} is too large for a Short value")
          case e: NumberFormatException =>
            v.setIncompatibleFormatException(e.getMessage)
        }
      }

      val f  = u.getNextFormat
      val vt = f.getValueType
      vt match {
        case ValueType.NIL =>
          u.unpackNil()
          v.setNull
        case ValueType.INTEGER =>
          read(u.unpackShort())
        case ValueType.STRING =>
          read(u.unpackString.toShort)
        case ValueType.BOOLEAN =>
          read(u.unpackBoolean().toShort)
        case ValueType.FLOAT =>
          read(u.unpackDouble().toShort)
        case _ =>
          u.skipValue()
          v.setNull
      }
    }
  }

  object IntCodec extends MessageCodec[Int] {
    override def pack(p: MessagePacker, v: Int): Unit = {
      p.packInt(v)
    }

    override def unpack(u: MessageUnpacker, v: MessageHolder) {
      def read(body: => Int) {
        try {
          v.setInt(body)
        } catch {
          case e: MessageIntegerOverflowException =>
            v.setIncompatibleFormatException(s"${e.getBigInteger} is too large for an Int value")
          case e: NumberFormatException =>
            v.setIncompatibleFormatException(e.getMessage)
        }
      }

      val f  = u.getNextFormat
      val vt = f.getValueType
      vt match {
        case ValueType.NIL =>
          u.unpackNil()
          v.setNull
        case ValueType.INTEGER =>
          read(u.unpackInt)
        case ValueType.STRING =>
          read(u.unpackString.toInt)
        case ValueType.BOOLEAN =>
          read(u.unpackBoolean().toInt)
        case ValueType.FLOAT =>
          read(u.unpackDouble().toInt)
        case _ =>
          u.skipValue()
          v.setNull
      }
    }
  }

  object LongCodec extends MessageCodec[Long] {
    def surface = Primitive.Long

    override def pack(p: MessagePacker, v: Long): Unit = {
      p.packLong(v)
    }

    override def unpack(u: MessageUnpacker, v: MessageHolder) {
      def read(body: => Long) {
        try {
          v.setLong(body)
        } catch {
          case e: MessageIntegerOverflowException =>
            v.setIncompatibleFormatException(s"${e.getBigInteger} is too large for a Long value")
          case e: NumberFormatException =>
            v.setIncompatibleFormatException(e.getMessage)
        }
      }
      val f  = u.getNextFormat
      val vt = f.getValueType
      vt match {
        case ValueType.NIL =>
          u.unpackNil()
          v.setNull
        case ValueType.INTEGER =>
          read(u.unpackLong)
        case ValueType.STRING =>
          read(u.unpackString().toLong)
        case ValueType.BOOLEAN =>
          read(u.unpackBoolean().toInt)
        case ValueType.FLOAT =>
          read(u.unpackDouble().toLong)
        case _ =>
          u.skipValue()
          v.setNull
      }
    }
  }

  object StringCodec extends MessageCodec[String] {
    def surface = Primitive.String

    override def pack(p: MessagePacker, v: String): Unit = {
      p.packString(v)
    }

    override def unpack(u: MessageUnpacker, v: MessageHolder) {
      def read(body: => String) {
        try {
          val s = body
          v.setString(s)
        } catch {
          case e: MessageIntegerOverflowException =>
            read(e.getBigInteger.toString())
          case e: NumberFormatException =>
            v.setIncompatibleFormatException(e.getMessage)
        }
      }

      u.getNextFormat.getValueType match {
        case ValueType.NIL =>
          u.unpackNil()
          v.setNull
        case ValueType.STRING =>
          read(u.unpackString())
        case ValueType.INTEGER =>
          read(u.unpackLong().toString)
        case ValueType.BOOLEAN =>
          read(u.unpackBoolean().toString)
        case ValueType.FLOAT =>
          read(u.unpackDouble().toString)
        case ValueType.MAP =>
          read(u.unpackValue().toJson)
        case ValueType.ARRAY =>
          read(u.unpackValue().toJson)
        case ValueType.BINARY =>
          read {
            val len = u.unpackBinaryHeader()
            new String(u.readPayload(len))
          }
        case _ =>
          // Use JSON format for unknown types so that we can read arbitrary types as String value
          read(u.unpackValue.toJson)
      }
    }
  }

  object BooleanCodec extends MessageCodec[Boolean] {
    override def pack(p: MessagePacker, v: Boolean): Unit = {
      p.packBoolean(v)
    }

    override def unpack(u: MessageUnpacker, v: MessageHolder) {
      def read(body: => Boolean) {
        try {
          val b = body
          v.setBoolean(b)
        } catch {
          case e: IllegalArgumentException =>
            v.setIncompatibleFormatException(e.getMessage)
          case e: NumberFormatException =>
            v.setIncompatibleFormatException(e.getMessage)
        }
      }

      u.getNextFormat.getValueType match {
        case ValueType.NIL =>
          u.unpackNil()
          v.setNull
        case ValueType.BOOLEAN =>
          read(u.unpackBoolean())
        case ValueType.STRING =>
          read(u.unpackString().toBoolean)
        case ValueType.INTEGER =>
          read(u.unpackLong() != 0L)
        case _ =>
          u.skipValue()
          v.setNull
      }
    }
  }

  object FloatCodec extends MessageCodec[Float] {
    override def pack(p: MessagePacker, v: Float): Unit = {
      p.packFloat(v)
    }

    override def unpack(u: MessageUnpacker, v: MessageHolder) {
      def read(body: => Float) {
        try {
          v.setFloat(body)
        } catch {
          case e: IllegalArgumentException =>
            v.setIncompatibleFormatException(e.getMessage)
          case e: NumberFormatException =>
            v.setIncompatibleFormatException(e.getMessage)
        }
      }
      u.getNextFormat.getValueType match {
        case ValueType.NIL =>
          u.unpackNil()
          v.setNull
        case ValueType.FLOAT =>
          read(u.unpackFloat().toFloat)
        case ValueType.INTEGER =>
          read(u.unpackLong().toFloat)
        case ValueType.STRING =>
          read(u.unpackString().toFloat)
        case _ =>
          u.skipValue()
          v.setNull
      }
    }
  }

  object DoubleCodec extends MessageCodec[Double] {
    override def pack(p: MessagePacker, v: Double): Unit = {
      p.packDouble(v)
    }

    override def unpack(u: MessageUnpacker, v: MessageHolder) {
      def read(body: => Double) {
        try {
          v.setDouble(body)
        } catch {
          case e: IllegalArgumentException =>
            v.setIncompatibleFormatException(e.getMessage)
          case e: NumberFormatException =>
            v.setIncompatibleFormatException(e.getMessage)
        }
      }
      u.getNextFormat.getValueType match {
        case ValueType.NIL =>
          u.unpackNil()
          v.setNull
        case ValueType.FLOAT =>
          read(u.unpackDouble())
        case ValueType.INTEGER =>
          read(u.unpackLong().toDouble)
        case ValueType.STRING =>
          read(u.unpackString.toDouble)
        case _ =>
          u.skipValue()
          v.setNull
      }
    }
  }

  object IntArrayCodec extends MessageCodec[Array[Int]] {
    override def pack(p: MessagePacker, v: Array[Int]): Unit = {
      p.packArrayHeader(v.length)
      v.foreach { x =>
        IntCodec.pack(p, x)
      }
    }

    override def unpack(u: MessageUnpacker, v: MessageHolder) {
      val len = u.unpackArrayHeader()
      val b   = Array.newBuilder[Int]
      b.sizeHint(len)
      (0 until len).foreach { i =>
        IntCodec.unpack(u, v)
        if (v.isNull) {
          // TODO report error?
          b += 0
        } else {
          val l = v.getInt
          b += l.toInt
        }
      }
      v.setObject(b.result())
    }
  }

  object ShortArrayCodec extends MessageCodec[Array[Short]] {
    override def pack(p: MessagePacker, v: Array[Short]): Unit = {
      p.packArrayHeader(v.length)
      v.foreach { x =>
        ShortCodec.pack(p, x)
      }
    }

    override def unpack(u: MessageUnpacker, v: MessageHolder) {
      val len = u.unpackArrayHeader()
      val b   = Array.newBuilder[Short]
      b.sizeHint(len)
      (0 until len).foreach { i =>
        IntCodec.unpack(u, v)
        if (v.isNull) {
          // TODO report error?
          b += 0
        } else {
          val l = v.getShort
          if (l.isValidInt) {
            b += l.toShort
          } else {
            // report error?
            b += 0
          }
        }
      }
      v.setObject(b.result())
    }
  }

  object CharArrayCodec extends MessageCodec[Array[Char]] {
    override def pack(p: MessagePacker, v: Array[Char]): Unit = {
      p.packArrayHeader(v.length)
      v.foreach { x =>
        CharCodec.pack(p, x)
      }
    }

    override def unpack(u: MessageUnpacker, v: MessageHolder) {
      val len = u.unpackArrayHeader()
      val b   = Array.newBuilder[Char]
      b.sizeHint(len)
      (0 until len).foreach { i =>
        CharCodec.unpack(u, v)
        if (v.isNull) {
          // TODO report error?
          b += 0
        } else {
          val l = v.getLong
          if (l.isValidChar) {
            b += l.toChar
          } else {
            // report error?
            b += 0
          }
        }
      }
      v.setObject(b.result())
    }
  }

  object LongArrayCodec extends MessageCodec[Array[Long]] {
    override def pack(p: MessagePacker, v: Array[Long]): Unit = {
      p.packArrayHeader(v.length)
      v.foreach { x =>
        LongCodec.pack(p, x)
      }
    }

    override def unpack(u: MessageUnpacker, v: MessageHolder) {
      val len = u.unpackArrayHeader()
      val b   = Array.newBuilder[Long]
      b.sizeHint(len)
      (0 until len).foreach { i =>
        LongCodec.unpack(u, v)
        if (v.isNull) {
          // TODO report error?
          b += 0L
        } else {
          val l = v.getLong
          b += l
        }
      }
      v.setObject(b.result())
    }
  }

  object FloatArrayCodec extends MessageCodec[Array[Float]] {
    override def pack(p: MessagePacker, v: Array[Float]): Unit = {
      p.packArrayHeader(v.length)
      v.foreach { x =>
        FloatCodec.pack(p, x)
      }
    }

    override def unpack(u: MessageUnpacker, v: MessageHolder) {
      val len = u.unpackArrayHeader()
      val b   = Array.newBuilder[Float]
      b.sizeHint(len)
      (0 until len).foreach { i =>
        val d = FloatCodec.unpack(u, v)
        if (v.isNull) {
          // report error?
          b += 0
        } else {
          // TODO check precision
          b += v.getDouble.toFloat
        }
      }
      v.setObject(b.result())
    }
  }

  object DoubleArrayCodec extends MessageCodec[Array[Double]] {
    override def pack(p: MessagePacker, v: Array[Double]): Unit = {
      p.packArrayHeader(v.length)
      v.foreach { x =>
        DoubleCodec.pack(p, x)
      }
    }

    override def unpack(u: MessageUnpacker, v: MessageHolder) {
      val len = u.unpackArrayHeader()
      val b   = Array.newBuilder[Double]
      b.sizeHint(len)
      (0 until len).foreach { i =>
        val d = DoubleCodec.unpack(u, v)
        if (v.isNull) {
          // report error?
          b += 0
        } else {
          b += v.getDouble
        }
      }
      v.setObject(b.result())
    }
  }

  object BooleanArrayCodec extends MessageCodec[Array[Boolean]] {
    override def pack(p: MessagePacker, v: Array[Boolean]): Unit = {
      p.packArrayHeader(v.length)
      v.foreach { x =>
        BooleanCodec.pack(p, x)
      }
    }

    override def unpack(u: MessageUnpacker, v: MessageHolder) {
      val len = u.unpackArrayHeader()
      val b   = Array.newBuilder[Boolean]
      b.sizeHint(len)
      (0 until len).foreach { i =>
        BooleanCodec.unpack(u, v)
        if (v.isNull) {
          // report error?
          b += false
        } else {
          b += v.getBoolean
        }
      }
      v.setObject(b.result())
    }
  }

  object ByteArrayCodec extends MessageCodec[Array[Byte]] {
    override def pack(p: MessagePacker, v: Array[Byte]): Unit = {
      p.packBinaryHeader(v.length)
      p.addPayload(v)
    }
    override def unpack(u: MessageUnpacker, v: MessageHolder): Unit = {
      val len = u.unpackBinaryHeader()
      val b   = u.readPayload(len)
      v.setObject(b)
    }
  }

  object StringArrayCodec extends MessageCodec[Array[String]] {
    override def pack(p: MessagePacker, v: Array[String]): Unit = {
      p.packArrayHeader(v.length)
      v.foreach { x =>
        StringCodec.pack(p, x)
      }
    }

    override def unpack(u: MessageUnpacker, v: MessageHolder) {
      val len = u.unpackArrayHeader()
      val b   = Array.newBuilder[String]
      b.sizeHint(len)
      (0 until len).foreach { i =>
        StringCodec.unpack(u, v)
        if (v.isNull) {
          b += "" // or report error?
        } else {
          b += v.getString
        }
      }
      v.setObject(b.result())
    }
  }

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
      // Use ISO instant formatter
      val isoInstantFormat = DateTimeFormatter.ISO_INSTANT.format(v)
      p.packString(isoInstantFormat)
    }

    override def unpack(u: MessageUnpacker, v: MessageHolder): Unit = {
      val isoInstantFormat = u.unpackString()
      v.setObject(Instant.parse(isoInstantFormat))
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
          v.setIncompatibleFormatException(s"${zonedDateTimeStr} cannot be read as ZonedDateTime: ${e.getMessage}")
      }
    }
  }

  object JavaUtilDateCodec extends MessageCodec[Date] {
    private val format = DateFormat.getInstance()

    override def pack(p: MessagePacker, v: Date): Unit = {
      val dateStr = format.format(v)
      p.packString(dateStr)
    }
    override def unpack(u: MessageUnpacker, v: MessageHolder): Unit = {
      val dateStr = u.unpackString()
      Try(format.parse(dateStr)) match {
        case Success(d) =>
          v.setObject(d)
        case Failure(e) =>
          v.setIncompatibleFormatException(s"Cannot parse ${dateStr}: ${e.getMessage}")
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
          v.setIncompatibleFormatException(s"${name} is not a value of ${enumType}")
      }
    }
  }

}
