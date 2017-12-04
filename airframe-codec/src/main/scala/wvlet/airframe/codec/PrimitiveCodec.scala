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

import wvlet.airframe.msgpack.spi._
import wvlet.surface
import wvlet.surface.{Primitive, Surface}

import scala.util.Try

/**
  *
  */
object PrimitiveCodec {

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

  private implicit class RichBoolean(b: Boolean) {
    def toInt: Int     = if (b) 1 else 0
    def toChar: Char   = if (b) 1 else 0
    def toByte: Byte   = if (b) 1 else 0
    def toShort: Short = if (b) 1 else 0
  }

  trait PrimitiveCodec[A] extends MessageCodec[A] {
    def surface: Surface
  }

  object ByteCodec extends PrimitiveCodec[Byte] {
    def surface = Primitive.Byte

    override def pack(p: Packer, v: Byte): Unit = {
      p.packByte(v)
    }

    override def unpack(u: Unpacker, v: MessageHolder): Unit = {
      def read(body: => Byte) {
        try {
          v.setByte(body)
        } catch {
          case e: IntegerOverflowException =>
            v.setIncompatibleFormatException(this, s"${e.bigInteger} is too large for a Byte value")
          case e: NumberFormatException =>
            v.setIncompatibleFormatException(this, e.getMessage)
        }
      }

      u.getNextValueType match {
        case ValueType.NIL =>
          u.unpackNil
          v.setNull
        case ValueType.INTEGER =>
          read(u.unpackByte)
        case ValueType.FLOAT =>
          read(u.unpackDouble.toByte)
        case ValueType.STRING =>
          read {
            val s = u.unpackString
            Try(s.toByte).getOrElse(s.toDouble.toByte)
          }
        case ValueType.BOOLEAN =>
          read(u.unpackBoolean.toByte)
        case _ =>
          u.skipValue
          v.setNull
      }
    }
  }

  object CharCodec extends PrimitiveCodec[Char] {
    def surface = Primitive.Char

    override def pack(p: Packer, v: Char): Unit = {
      p.packInt(v)
    }

    override def unpack(u: Unpacker, v: MessageHolder): Unit = {
      def read(body: => Char) {
        try {
          v.setChar(body)
        } catch {
          case e: IntegerOverflowException =>
            v.setIncompatibleFormatException(this, s"${e.bigInteger} is too large for a Char value")
          case e: NumberFormatException =>
            v.setIncompatibleFormatException(this, e.getMessage)
        }
      }

      u.getNextValueType match {
        case ValueType.NIL =>
          u.unpackNil
          v.setNull
        case ValueType.INTEGER =>
          read(u.unpackInt.toChar)
        case ValueType.STRING =>
          read {
            val s = u.unpackString
            if (s.length == 1) s.charAt(0)
            else s.toDouble.toChar
          }
        case ValueType.BOOLEAN =>
          read(u.unpackBoolean.toChar)
        case ValueType.FLOAT =>
          read(u.unpackDouble.toChar)
        case _ =>
          u.skipValue
          v.setNull
      }
    }
  }

  object ShortCodec extends PrimitiveCodec[Short] {
    def surface = Primitive.Short
    override def pack(p: Packer, v: Short): Unit = {
      p.packShort(v)
    }

    override def unpack(u: Unpacker, v: MessageHolder): Unit = {
      def read(body: => Short) {
        try {
          v.setShort(body)
        } catch {
          case e: IntegerOverflowException =>
            v.setIncompatibleFormatException(this, s"${e.bigInteger} is too large for a Short value")
          case e: NumberFormatException =>
            v.setIncompatibleFormatException(this, e.getMessage)
        }
      }

      u.getNextValueType match {
        case ValueType.NIL =>
          u.unpackNil
          v.setNull
        case ValueType.INTEGER =>
          read(u.unpackShort)
        case ValueType.STRING =>
          read {
            val s = u.unpackString
            Try(s.toShort).getOrElse(s.toDouble.toShort)
          }
        case ValueType.BOOLEAN =>
          read(u.unpackBoolean.toShort)
        case ValueType.FLOAT =>
          read(u.unpackDouble.toShort)
        case _ =>
          u.skipValue
          v.setNull
      }
    }
  }

  object IntCodec extends PrimitiveCodec[Int] {
    def surface = Primitive.Int
    override def pack(p: Packer, v: Int): Unit = {
      p.packInt(v)
    }

    override def unpack(u: Unpacker, v: MessageHolder) {
      def read(body: => Int) {
        try {
          v.setInt(body)
        } catch {
          case e: IntegerOverflowException =>
            v.setIncompatibleFormatException(this, s"${e.bigInteger} is too large for an Int value")
          case e: NumberFormatException =>
            v.setIncompatibleFormatException(this, e.getMessage)
        }
      }

      u.getNextValueType match {
        case ValueType.NIL =>
          u.unpackNil
          v.setNull
        case ValueType.INTEGER =>
          read(u.unpackInt)
        case ValueType.STRING =>
          read {
            val s = u.unpackString
            Try(s.toInt).getOrElse(s.toDouble.toInt)
          }
        case ValueType.BOOLEAN =>
          read(u.unpackBoolean.toInt)
        case ValueType.FLOAT =>
          read(u.unpackDouble.toInt)
        case _ =>
          u.skipValue
          v.setNull
      }
    }
  }

  object LongCodec extends PrimitiveCodec[Long] {
    def surface = Primitive.Long

    override def pack(p: Packer, v: Long): Unit = {
      p.packLong(v)
    }

    override def unpack(u: Unpacker, v: MessageHolder) {
      def read(body: => Long) {
        try {
          v.setLong(body)
        } catch {
          case e: IntegerOverflowException =>
            v.setIncompatibleFormatException(this, s"${e.bigInteger} is too large for a Long value")
          case e: NumberFormatException =>
            v.setIncompatibleFormatException(this, e.getMessage)
        }
      }
      u.getNextValueType match {
        case ValueType.NIL =>
          u.unpackNil
          v.setNull
        case ValueType.INTEGER =>
          read(u.unpackLong)
        case ValueType.STRING =>
          read {
            val s = u.unpackString
            Try(s.toLong).getOrElse(s.toDouble.toLong)
          }
        case ValueType.BOOLEAN =>
          read(u.unpackBoolean.toInt)
        case ValueType.FLOAT =>
          read(u.unpackDouble.toLong)
        case _ =>
          u.skipValue
          v.setNull
      }
    }
  }

  object StringCodec extends PrimitiveCodec[String] {
    def surface = Primitive.String

    override def pack(p: Packer, v: String): Unit = {
      if (v == null) {
        p.packNil
      } else {
        p.packString(v)
      }
    }

    override def unpack(u: Unpacker, v: MessageHolder) {
      def read(body: => String) {
        try {
          val s = body
          v.setString(s)
        } catch {
          case e: IntegerOverflowException =>
            read(e.bigInteger.toString())
          case e: NumberFormatException =>
            v.setIncompatibleFormatException(this, e.getMessage)
        }
      }

      u.getNextValueType match {
        case ValueType.NIL =>
          u.unpackNil
          v.setNull
        case ValueType.STRING =>
          read(u.unpackString)
        case ValueType.INTEGER =>
          read(u.unpackLong.toString)
        case ValueType.BOOLEAN =>
          read(u.unpackBoolean.toString)
        case ValueType.FLOAT =>
          read(u.unpackDouble.toString)
        case ValueType.MAP =>
          read(u.unpackValue.toJson)
        case ValueType.ARRAY =>
          read(u.unpackValue.toJson)
        case ValueType.BINARY =>
          read {
            val len = u.unpackBinaryHeader
            new String(u.readPayload(len))
          }
        case _ =>
          // Use JSON format for unknown types so that we can read arbitrary types as String value
          read(u.unpackValue.toJson)
      }
    }
  }

  object BooleanCodec extends PrimitiveCodec[Boolean] {
    def surface = Primitive.Boolean
    override def pack(p: Packer, v: Boolean): Unit = {
      p.packBoolean(v)
    }

    override def unpack(u: Unpacker, v: MessageHolder) {
      def read(body: => Boolean) {
        try {
          val b = body
          v.setBoolean(b)
        } catch {
          case e: IntegerOverflowException =>
            v.setBoolean(e.bigInteger.doubleValue() != 0.0)
          case e: IllegalArgumentException =>
            v.setIncompatibleFormatException(this, e.getMessage)
          case e: NumberFormatException =>
            v.setIncompatibleFormatException(this, e.getMessage)
        }
      }

      u.getNextValueType match {
        case ValueType.NIL =>
          u.unpackNil
          v.setNull
        case ValueType.BOOLEAN =>
          read(u.unpackBoolean)
        case ValueType.STRING =>
          read {
            val s = u.unpackString
            Try(s.toBoolean).getOrElse(s.toDouble != 0.0)
          }
        case ValueType.INTEGER =>
          read(u.unpackLong != 0L)
        case ValueType.FLOAT =>
          read(u.unpackDouble != 0.0)
        case _ =>
          u.skipValue
          v.setNull
      }
    }
  }

  object FloatCodec extends PrimitiveCodec[Float] {
    def surface = Primitive.Float
    override def pack(p: Packer, v: Float): Unit = {
      p.packFloat(v)
    }

    override def unpack(u: Unpacker, v: MessageHolder) {
      def read(body: => Float) {
        try {
          v.setFloat(body)
        } catch {
          case e: IntegerOverflowException =>
            v.setFloat(e.bigInteger.floatValue())
          case e: IllegalArgumentException =>
            v.setIncompatibleFormatException(this, e.getMessage)
          case e: NumberFormatException =>
            v.setIncompatibleFormatException(this, e.getMessage)
        }
      }
      u.getNextValueType match {
        case ValueType.NIL =>
          u.unpackNil
          v.setNull
        case ValueType.FLOAT =>
          read(u.unpackFloat.toFloat)
        case ValueType.INTEGER =>
          read(u.unpackLong.toFloat)
        case ValueType.BOOLEAN =>
          read(u.unpackBoolean.toInt.toFloat)
        case ValueType.STRING =>
          read(u.unpackString.toFloat)
        case _ =>
          u.skipValue
          v.setNull
      }
    }
  }

  object DoubleCodec extends PrimitiveCodec[Double] {
    def surface = Primitive.Double

    override def pack(p: Packer, v: Double): Unit = {
      p.packDouble(v)
    }

    override def unpack(u: Unpacker, v: MessageHolder) {
      def read(body: => Double) {
        try {
          v.setDouble(body)
        } catch {
          case e: IntegerOverflowException =>
            v.setDouble(e.bigInteger.doubleValue())
          case e: IllegalArgumentException =>
            v.setIncompatibleFormatException(this, e.getMessage)
          case e: NumberFormatException =>
            v.setIncompatibleFormatException(this, e.getMessage)
        }
      }
      u.getNextValueType match {
        case ValueType.NIL =>
          u.unpackNil
          v.setNull
        case ValueType.FLOAT =>
          read(u.unpackDouble)
        case ValueType.INTEGER =>
          read(u.unpackLong.toDouble)
        case ValueType.BOOLEAN =>
          read(u.unpackBoolean.toInt.toDouble)
        case ValueType.STRING =>
          read(u.unpackString.toDouble)
        case _ =>
          u.skipValue
          v.setNull
      }
    }
  }

  object IntArrayCodec extends MessageCodec[Array[Int]] {
    override def pack(p: Packer, v: Array[Int]): Unit = {
      p.packArrayHeader(v.length)
      v.foreach { x =>
        IntCodec.pack(p, x)
      }
    }

    override def unpack(u: Unpacker, v: MessageHolder) {
      val len = u.unpackArrayHeader
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
    override def pack(p: Packer, v: Array[Short]): Unit = {
      p.packArrayHeader(v.length)
      v.foreach { x =>
        ShortCodec.pack(p, x)
      }
    }

    override def unpack(u: Unpacker, v: MessageHolder) {
      val len = u.unpackArrayHeader
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
    override def pack(p: Packer, v: Array[Char]): Unit = {
      p.packArrayHeader(v.length)
      v.foreach { x =>
        CharCodec.pack(p, x)
      }
    }

    override def unpack(u: Unpacker, v: MessageHolder) {
      val len = u.unpackArrayHeader
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
    override def pack(p: Packer, v: Array[Long]): Unit = {
      p.packArrayHeader(v.length)
      v.foreach { x =>
        LongCodec.pack(p, x)
      }
    }

    override def unpack(u: Unpacker, v: MessageHolder) {
      val len = u.unpackArrayHeader
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
    override def pack(p: Packer, v: Array[Float]): Unit = {
      p.packArrayHeader(v.length)
      v.foreach { x =>
        FloatCodec.pack(p, x)
      }
    }

    override def unpack(u: Unpacker, v: MessageHolder) {
      val len = u.unpackArrayHeader
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
    override def pack(p: Packer, v: Array[Double]): Unit = {
      p.packArrayHeader(v.length)
      v.foreach { x =>
        DoubleCodec.pack(p, x)
      }
    }

    override def unpack(u: Unpacker, v: MessageHolder) {
      val len = u.unpackArrayHeader
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
    override def pack(p: Packer, v: Array[Boolean]): Unit = {
      p.packArrayHeader(v.length)
      v.foreach { x =>
        BooleanCodec.pack(p, x)
      }
    }

    override def unpack(u: Unpacker, v: MessageHolder) {
      val len = u.unpackArrayHeader
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
    override def pack(p: Packer, v: Array[Byte]): Unit = {
      p.packBinaryHeader(v.length)
      p.addPayload(v)
    }
    override def unpack(u: Unpacker, v: MessageHolder): Unit = {
      val len = u.unpackBinaryHeader
      val b   = u.readPayload(len)
      v.setObject(b)
    }
  }

  object StringArrayCodec extends MessageCodec[Array[String]] {
    override def pack(p: Packer, v: Array[String]): Unit = {
      p.packArrayHeader(v.length)
      v.foreach { x =>
        StringCodec.pack(p, x)
      }
    }

    override def unpack(u: Unpacker, v: MessageHolder) {
      val len = u.unpackArrayHeader
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

}
