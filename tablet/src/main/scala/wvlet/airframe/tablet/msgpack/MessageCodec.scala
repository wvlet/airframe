package wvlet.airframe.tablet.msgpack

import org.msgpack.core.{MessagePacker, MessageUnpacker}
import org.msgpack.value.ValueType

import scala.util.{Failure, Success, Try}

trait MessageCodec[@specialized(Int, Long, Float, Double, Boolean) A] {

  def pack(p: MessagePacker, v: A)
  def unpack(u: MessageUnpacker, v: MessageHolder)
}

object LongCodec$ extends MessageCodec[Long] {
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

object StringCodec$ extends MessageCodec[String] {
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

object BooleanCodec$ extends MessageCodec[Boolean] {
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

object DoubleCodec$ extends MessageCodec[Double] {
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

object IntArrayCodec$ extends MessageCodec[Array[Int]] {
  override def pack(p: MessagePacker, v: Array[Int]): Unit = {
    p.packArrayHeader(v.length)
    v.foreach { x =>
      LongCodec$.pack(p, x)
    }
  }

  override def unpack(u: MessageUnpacker, v: MessageHolder) {
    val len = u.unpackArrayHeader()
    val b   = Array.newBuilder[Int]
    b.sizeHint(len)
    (0 until len).foreach { i =>
      LongCodec$.unpack(u, v)
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

object LongArrayCodec$ extends MessageCodec[Array[Long]] {
  override def pack(p: MessagePacker, v: Array[Long]): Unit = {
    p.packArrayHeader(v.length)
    v.foreach { x =>
      LongCodec$.pack(p, x)
    }
  }

  override def unpack(u: MessageUnpacker, v: MessageHolder) {
    val len = u.unpackArrayHeader()
    val b   = Array.newBuilder[Long]
    b.sizeHint(len)
    (0 until len).foreach { i =>
      LongCodec$.unpack(u, v)
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
      DoubleCodec$.pack(p, x)
    }
  }

  override def unpack(u: MessageUnpacker, v: MessageHolder) {
    val len = u.unpackArrayHeader()
    val b   = Array.newBuilder[Float]
    b.sizeHint(len)
    (0 until len).foreach { i =>
      val d = DoubleCodec$.unpack(u, v)
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

object DoubleArrayCodec$ extends MessageCodec[Array[Double]] {
  override def pack(p: MessagePacker, v: Array[Double]): Unit = {
    p.packArrayHeader(v.length)
    v.foreach { x =>
      DoubleCodec$.pack(p, x)
    }
  }

  override def unpack(u: MessageUnpacker, v: MessageHolder) {
    val len = u.unpackArrayHeader()
    val b   = Array.newBuilder[Double]
    b.sizeHint(len)
    (0 until len).foreach { i =>
      val d = DoubleCodec$.unpack(u, v)
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

object BooleanArrayCodec$ extends MessageCodec[Array[Boolean]] {
  override def pack(p: MessagePacker, v: Array[Boolean]): Unit = {
    p.packArrayHeader(v.length)
    v.foreach { x =>
      BooleanCodec$.pack(p, x)
    }
  }

  override def unpack(u: MessageUnpacker, v: MessageHolder) {
    val len = u.unpackArrayHeader()
    val b   = Array.newBuilder[Boolean]
    b.sizeHint(len)
    (0 until len).foreach { i =>
      BooleanCodec$.unpack(u, v)
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

object StringArrayCodec$ extends MessageCodec[Array[String]] {
  override def pack(p: MessagePacker, v: Array[String]): Unit = {
    p.packArrayHeader(v.length)
    v.foreach { x =>
      StringCodec$.pack(p, x)
    }
  }

  override def unpack(u: MessageUnpacker, v: MessageHolder) {
    val len = u.unpackArrayHeader()
    val b   = Array.newBuilder[String]
    b.sizeHint(len)
    (0 until len).foreach { i =>
      StringCodec$.unpack(u, v)
      if (v.isNull) {
        // skip
      } else {
        b += v.getString
      }
    }
    v.setObject(b.result())

  }
}

class ObjectFormatter[A](cl: Class[A], codecTable: Map[Class[_], MessageCodec[_]]) {}
