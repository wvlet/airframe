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

import org.msgpack.core.{MessagePacker, MessageUnpacker}
import org.msgpack.value.ValueType
import wvlet.surface.{Primitive, Surface}

import scala.util.{Failure, Success, Try}

case class ObjectCodec[A](surface: Surface, paramCodec: Seq[MessageCodec[_]]) extends MessageCodec[A] {
  override def pack(p: MessagePacker, v: A): Unit = {
    val numParams = surface.params.length
    // Use array format [p1, p2, ....]
    p.packArrayHeader(numParams)
    for ((param, codec) <- surface.params.zip(paramCodec)) {
      val paramValue = param.get(v)
      codec.asInstanceOf[MessageCodec[Any]].pack(p, paramValue)
    }
  }

  override def unpack(u: MessageUnpacker, v: MessageHolder): Unit = {
    val numParams = surface.params.length
    val numElems  = u.unpackArrayHeader()
    if (numParams != numElems) {
      u.skipValue(numElems)
      v.setNull
    } else {
      var index = 0
      val args  = Seq.newBuilder[Any]
      while (index < numElems && index < numParams) {
        // TODO reuse message holders
        val m = new MessageHolder
        paramCodec(index).unpack(u, m)
        // TODO handle null value?
        args += m.getLastValue
        index += 1
      }
      surface.objectFactory
        .map(_.newInstance(args.result()))
        .map(x => v.setObject(x))
        .getOrElse(v.setNull)
    }
  }
}

object ByteCodec extends MessageCodec[Byte] {
  override def pack(p: MessagePacker, v: Byte): Unit = {
    p.packByte(v)
  }

  override def unpack(u: MessageUnpacker, v: MessageHolder): Unit = {
    val f  = u.getNextFormat
    val vt = f.getValueType
    vt match {
      case ValueType.NIL =>
        u.unpackNil()
        v.setNull
      case ValueType.INTEGER =>
        v.setLong(u.unpackByte())
      case ValueType.STRING =>
        Try(u.unpackString.toByte) match {
          case Success(l) =>
            v.setLong(l)
          case Failure(e) =>
            v.setNull
        }
      case ValueType.BOOLEAN =>
        v.setLong(if (u.unpackBoolean()) {
          1
        } else {
          0
        })
      case ValueType.FLOAT =>
        v.setLong(u.unpackDouble().toLong)
      case other =>
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
    val f  = u.getNextFormat
    val vt = f.getValueType
    vt match {
      case ValueType.NIL =>
        u.unpackNil()
        v.setNull
      case ValueType.INTEGER =>
        v.setLong(u.unpackShort())
      case ValueType.STRING =>
        Try(u.unpackString.toShort) match {
          case Success(l) =>
            v.setLong(l)
          case Failure(e) =>
            v.setNull
        }
      case ValueType.BOOLEAN =>
        v.setLong(if (u.unpackBoolean()) {
          1
        } else {
          0
        })
      case ValueType.FLOAT =>
        v.setLong(u.unpackDouble().toShort)
      case other =>
        u.skipValue()
        v.setNull
    }
  }
}

object IntCodec extends MessageCodec[Int] {
  def surface = Primitive.Int
  override def pack(p: MessagePacker, v: Int): Unit = {
    p.packInt(v)
  }
  override def unpack(u: MessageUnpacker, v: MessageHolder) {
    val f  = u.getNextFormat
    val vt = f.getValueType
    vt match {
      case ValueType.NIL =>
        u.unpackNil()
        v.setNull
      case ValueType.INTEGER =>
        v.setLong(u.unpackInt)
      case ValueType.STRING =>
        Try(u.unpackString.toInt) match {
          case Success(l) =>
            v.setLong(l)
          case Failure(e) =>
            v.setNull
        }
      case ValueType.BOOLEAN =>
        v.setLong(if (u.unpackBoolean()) {
          1
        } else {
          0
        })
      case ValueType.FLOAT =>
        Try(v.setLong(u.unpackDouble().toInt)) match {
          case Success(l) =>
            v.setLong(l)
          case Failure(e) =>
            v.setNull
        }
      case other =>
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
    val f  = u.getNextFormat
    val vt = f.getValueType
    vt match {
      case ValueType.NIL =>
        u.unpackNil()
        v.setNull
      case ValueType.INTEGER =>
        v.setLong(u.unpackLong())
      case ValueType.STRING =>
        Try(u.unpackString.toLong) match {
          case Success(l) =>
            v.setLong(l)
          case Failure(e) =>
            v.setNull
        }
      case ValueType.BOOLEAN =>
        v.setLong(if (u.unpackBoolean()) {
          1
        } else {
          0
        })
      case ValueType.FLOAT =>
        v.setLong(u.unpackDouble().toLong)
      case other =>
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
    u.getNextFormat.getValueType match {
      case ValueType.NIL =>
        u.unpackNil()
        v.setNull
      case ValueType.STRING =>
        v.setString(u.unpackString())
      case ValueType.INTEGER =>
        v.setString(u.unpackLong().toString)
      case ValueType.BOOLEAN =>
        v.setString(u.unpackBoolean().toString)
      case ValueType.FLOAT =>
        v.setString(u.unpackDouble().toString)
      case ValueType.MAP =>
        val m = u.unpackValue()
        v.setString(m.toJson)
      case ValueType.ARRAY =>
        val arr = v.setObject(u.unpackValue())
        v.setString(arr.toJson)
      case ValueType.BINARY =>
        val len = u.unpackBinaryHeader()
        v.setString(new String(u.readPayload(len)))
      case ValueType.EXTENSION =>
        val ext = u.unpackValue()
        v.setString(ext.toJson)
    }
  }
}

object BooleanCodec extends MessageCodec[Boolean] {
  override def pack(p: MessagePacker, v: Boolean): Unit = {
    p.packBoolean(v)
  }

  override def unpack(u: MessageUnpacker, v: MessageHolder) {
    u.getNextFormat.getValueType match {
      case ValueType.NIL =>
        u.unpackNil()
        v.setNull
      case ValueType.BOOLEAN =>
        v.setBoolean(u.unpackBoolean())
      case ValueType.STRING =>
        val s = u.unpackString()
        v.setBoolean(s.toBoolean)
      case ValueType.INTEGER =>
        val l = u.unpackLong()
        v.setBoolean(l != 0L)
      case other =>
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
    u.getNextFormat.getValueType match {
      case ValueType.NIL =>
        u.unpackNil()
        v.setNull
      case ValueType.FLOAT =>
        v.setDouble(u.unpackDouble())
      case ValueType.INTEGER =>
        v.setDouble(u.unpackLong().toFloat)
      case ValueType.STRING =>
        Try(u.unpackString.toDouble) match {
          case Success(d) => v.setDouble(d)
          case Failure(e) => v.setNull
        }
      case other =>
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
    u.getNextFormat.getValueType match {
      case ValueType.NIL =>
        u.unpackNil()
        v.setNull
      case ValueType.FLOAT =>
        v.setDouble(u.unpackDouble())
      case ValueType.INTEGER =>
        v.setDouble(u.unpackLong().toDouble)
      case ValueType.STRING =>
        Try(u.unpackString.toDouble) match {
          case Success(d) => v.setDouble(d)
          case Failure(e) => v.setNull
        }
      case other =>
        u.skipValue()
        v.setNull
    }
  }
}

object IntArrayCodec extends MessageCodec[Array[Int]] {
  override def pack(p: MessagePacker, v: Array[Int]): Unit = {
    p.packArrayHeader(v.length)
    v.foreach { x =>
      LongCodec.pack(p, x)
    }
  }

  override def unpack(u: MessageUnpacker, v: MessageHolder) {
    val len = u.unpackArrayHeader()
    val b   = Array.newBuilder[Int]
    b.sizeHint(len)
    (0 until len).foreach { i =>
      LongCodec.unpack(u, v)
      if (v.isNull) {
        // TODO report error?
        b += 0
      } else {
        val l = v.getLong
        if (l.isValidInt) {
          b += l.toInt
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

object FlaotArrayCodec$ extends MessageCodec[Array[Float]] {
  override def pack(p: MessagePacker, v: Array[Float]): Unit = {
    p.packArrayHeader(v.length)
    v.foreach { x =>
      DoubleCodec.pack(p, x)
    }
  }

  override def unpack(u: MessageUnpacker, v: MessageHolder) {
    val len = u.unpackArrayHeader()
    val b   = Array.newBuilder[Float]
    b.sizeHint(len)
    (0 until len).foreach { i =>
      val d = DoubleCodec.unpack(u, v)
      if (v.isNull) {
        // report error?
        b += 0
      } else {
        b += v.getDouble.toFloat
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
        b += true
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
        // skip
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
